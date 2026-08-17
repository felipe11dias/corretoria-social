package com.corretoria.service;

import com.corretoria.entity.Listing;
import com.corretoria.entity.ListingPhoto;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.InvalidListingException;
import com.corretoria.event.ListingPublishedEvent;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.ListingMapper;
import com.corretoria.repository.ListingRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.util.ObjectUtils;
import com.corretoria.util.TextUtils;
import com.corretoria.vo.shared.PhotoContent;
import com.corretoria.vo.v1.request.ListingCreationRequest;
import com.corretoria.vo.v1.request.ListingSearchRequest;
import com.corretoria.vo.v1.request.ListingStatusUpdateRequest;
import com.corretoria.vo.v1.request.ListingUpdateRequest;
import com.corretoria.vo.v1.request.PropertyRequest;
import com.corretoria.vo.v1.request.VehicleRequest;
import com.corretoria.vo.v1.response.ListingPhotoResponse;
import com.corretoria.vo.v1.response.ListingResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio do catálogo de anúncios.
 *
 * <p>O corretor dono nunca vem do corpo da requisição: é resolvido a partir do token. A posse
 * nas operações de escrita é barrada antes, no {@code @PreAuthorize} do controller, com o
 * {@code ListingSecurity} — aqui ficam as regras que dependem do conteúdo do anúncio.
 */
@Service
public class ListingService {

    private static final int MAX_PHOTOS_PER_LISTING = 20;
    private static final int FIRST_PHOTO_POSITION = 1;
    private static final String PHOTO_KEY_PREFIX = "listings/";

    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final StorageService storageService;
    private final UserSecurity userSecurity;
    private final ApplicationEventPublisher eventPublisher;

    public ListingService(
            ListingRepository listingRepository,
            ListingMapper listingMapper,
            StorageService storageService,
            UserSecurity userSecurity,
            ApplicationEventPublisher eventPublisher) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.storageService = storageService;
        this.userSecurity = userSecurity;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publica um anúncio em nome do corretor autenticado.
     *
     * <p>A publicação é imediata, sem fila de aprovação — mesma confiança já dada ao
     * autocadastro de usuários. O contrapeso é a denúncia, no Cluster E.
     *
     * <p>Ao final, anuncia o fato ({@code ListingPublishedEvent}) em vez de chamar o casamento
     * com buscas salvas: publicar continua sendo publicar, e quem quiser reagir se inscreve. O
     * evento só é entregue depois do commit, e quem o consome trabalha em outra thread — o
     * corretor não espera pelo trabalho de notificar terceiros.
     */
    @Transactional
    public ListingResponse publish(
            ListingCreationRequest request, Authentication authentication) {

        User realtor = userSecurity.getAuthenticatedUser(authentication);
        ensureRealtorRole(realtor);
        ensureDetailMatchesType(request.type(), request.property(), request.vehicle());
        ensureOperationIsAllowed(request.type(), request.operationType());

        Listing listing = listingMapper.toEntity(request);
        listing.setRealtor(realtor);

        Listing published = listingRepository.save(listing);
        eventPublisher.publishEvent(new ListingPublishedEvent(published.getId()));

        return toResponse(published);
    }

    /**
     * Atualiza conteúdo e detalhamento do anúncio.
     *
     * <p>O tipo é imutável: trocar imóvel por automóvel descartaria o detalhamento inteiro.
     * Por isso o detalhamento enviado é conferido contra o tipo <strong>já registrado</strong>,
     * e não contra um tipo vindo da requisição.
     */
    @Transactional
    public ListingResponse update(Long id, ListingUpdateRequest request) {
        Listing listing = getListingOrThrow(id);
        ensureDetailMatchesType(listing.getType(), request.property(), request.vehicle());
        ensureOperationIsAllowed(listing.getType(), request.operationType());

        listingMapper.updateEntity(listing, request);
        updateDetail(listing, request);

        return toResponse(listingRepository.save(listing));
    }

    /** Pausa, remove, reativa ou marca o anúncio como concluído. */
    @Transactional
    public ListingResponse changeStatus(Long id, ListingStatusUpdateRequest request) {
        Listing listing = getListingOrThrow(id);
        listing.setStatus(request.status());

        return toResponse(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public ListingResponse findById(Long id) {
        return toResponse(getListingOrThrow(id));
    }

    /**
     * Busca paginada do catálogo.
     *
     * <p>Sem filtro de situação, devolve apenas anúncios ativos: é o catálogo público. Pedir
     * outra situação exige ser o corretor dono ou administrador.
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> search(
            ListingSearchRequest request, Pageable pageable, Authentication authentication) {

        ensureSearchCriteriaAreCoherent(request);
        ensureNonActiveSearchIsOwnScope(request, authentication);
        String photoBaseUrl = storageService.baseUrl();

        return listingRepository.search(
                        request.effectiveStatus(),
                        request.type(),
                        request.operationType(),
                        request.realtorId(),
                        normalizedOrNull(request.city()),
                        normalizedOrNull(request.state()),
                        normalizedOrNull(request.neighborhood()),
                        request.minPrice(),
                        request.maxPrice(),
                        request.propertyType(),
                        request.minBedrooms(),
                        request.minTotalArea(),
                        request.vehicleType(),
                        normalizedOrNull(request.brand()),
                        request.maxMileageKm(),
                        request.minManufactureYear(),
                        request.latitude(),
                        request.longitude(),
                        request.radiusKm(),
                        pageable)
                .map(listing -> listingMapper.toResponse(listing, photoBaseUrl));
    }

    /**
     * Envia uma foto para o storage e a vincula ao anúncio.
     *
     * <p>A primeira foto vira a capa automaticamente: um anúncio sem capa apareceria sem
     * imagem na listagem, e exigir uma segunda requisição só para marcá-la seria cerimônia.
     *
     * <p>A gravação no storage acontece dentro da transação: se o commit falhar depois dela, o
     * objeto fica órfão no bucket — sem linha apontando para ele, portanto invisível. É o lado
     * seguro do dilema; o inverso deixaria o anúncio com uma foto quebrada.
     */
    @Transactional
    public ListingPhotoResponse addPhoto(Long id, PhotoContent photo) {
        Listing listing = getListingOrThrow(id);
        ensurePhotoLimitIsNotReached(listing);

        ListingPhoto listingPhoto = new ListingPhoto();
        listingPhoto.setStorageKey(storageService.store(photo, PHOTO_KEY_PREFIX + listing.getId()));
        listingPhoto.setPosition(nextPhotoPosition(listing));
        listingPhoto.setCover(listing.getPhotos().isEmpty());
        listing.addPhoto(listingPhoto);

        // flush, e não save: o anúncio já é uma entidade gerenciada, então a nova foto é
        // gravada por cascade — o flush apenas antecipa o INSERT, que é o que gera o id
        // devolvido na resposta.
        //
        // save() aqui seria pior que redundante: para uma entidade com id ele é um merge, e o
        // merge de um filho ainda transiente grava uma *cópia* gerenciada. O id nasceria na
        // cópia, e o cliente receberia "id": null — sem como apagar a foto que acabou de
        // enviar. Foi o que aconteceu na primeira validação contra o banco real.
        listingRepository.flush();
        return listingMapper.toPhotoResponse(listingPhoto, storageService.baseUrl());
    }

    /**
     * Remove a foto do anúncio e o objeto correspondente do storage.
     *
     * <p>Remover a capa promove a próxima foto: o anúncio nunca fica com fotos e sem capa.
     */
    @Transactional
    public void deletePhoto(Long id, Long photoId) {
        Listing listing = getListingOrThrow(id);
        ListingPhoto photo = findPhotoOrThrow(listing, photoId);

        listing.removePhoto(photo);
        promoteCoverIfRemoved(listing, photo);

        listingRepository.save(listing);
        storageService.delete(photo.getStorageKey());
    }

    private void updateDetail(Listing listing, ListingUpdateRequest request) {
        switch (listing.getType()) {
            case PROPERTY -> listingMapper.updateProperty(listing.getProperty(), request.property());
            case VEHICLE -> listingMapper.updateVehicle(listing.getVehicle(), request.vehicle());
        }
    }

    private void ensureRealtorRole(User realtor) {
        boolean hasRealtorRole = realtor.getRoles().stream()
                .map(Role::getName)
                .anyMatch(RoleName.ROLE_REALTOR::equals);

        if (!hasRealtorRole) {
            throw ForbiddenOperationException.realtorRoleRequired();
        }
    }

    /**
     * Um anúncio tem exatamente um detalhamento, o do seu tipo.
     *
     * <p>A regra não cabe na Bean Validation porque depende de três campos ao mesmo tempo — é
     * regra de negócio, não formato de campo.
     */
    private void ensureDetailMatchesType(
            ListingType type, PropertyRequest property, VehicleRequest vehicle) {

        boolean coherent = switch (type) {
            case PROPERTY -> property != null && vehicle == null;
            case VEHICLE -> vehicle != null && property == null;
        };

        if (coherent) {
            return;
        }

        throw switch (type) {
            case PROPERTY -> property == null
                    ? InvalidListingException.propertyDetailRequired()
                    : InvalidListingException.detailDoesNotMatchType();
            case VEHICLE -> vehicle == null
                    ? InvalidListingException.vehicleDetailRequired()
                    : InvalidListingException.detailDoesNotMatchType();
        };
    }

    private void ensureOperationIsAllowed(ListingType type, OperationType operationType) {
        if (type == ListingType.VEHICLE && operationType == OperationType.RENT) {
            throw InvalidListingException.vehicleCannotBeRented();
        }
    }

    /**
     * Coerência entre os critérios de busca.
     *
     * <p>Fica aqui, e não no construtor do VO, porque o VO é montado pelo <em>data binding</em>
     * da query string — exceção durante a construção viraria {@code 500} em vez de
     * {@code 400}.
     */
    private void ensureSearchCriteriaAreCoherent(ListingSearchRequest request) {
        if (request.radiusKm() != null
                && (request.latitude() == null || request.longitude() == null)) {
            throw new IllegalArgumentException("A busca por raio exige latitude e longitude");
        }
        if (request.radiusKm() != null && request.radiusKm() <= 0) {
            throw new IllegalArgumentException("O raio deve ser maior que zero");
        }
        if (request.minPrice() != null && request.maxPrice() != null
                && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw new IllegalArgumentException(
                    "O preço mínimo não pode ser maior que o preço máximo");
        }
    }

    private void ensureNonActiveSearchIsOwnScope(
            ListingSearchRequest request, Authentication authentication) {

        if (request.effectiveStatus() == ListingStatus.ACTIVE
                || userSecurity.isAdmin(authentication)) {
            return;
        }

        boolean ownScope = userSecurity.findAuthenticatedUser(authentication)
                .map(User::getId)
                .filter(ownId -> ownId.equals(request.realtorId()))
                .isPresent();

        if (!ownScope) {
            throw ForbiddenOperationException.nonActiveSearchRequiresOwnership();
        }
    }

    private void ensurePhotoLimitIsNotReached(Listing listing) {
        if (listing.getPhotos().size() >= MAX_PHOTOS_PER_LISTING) {
            throw InvalidListingException.photoLimitReached(MAX_PHOTOS_PER_LISTING);
        }
    }

    /**
     * Próxima posição livre.
     *
     * <p>Continua da maior posição existente em vez de contar as fotos: excluir a foto 2 de
     * três deixa as posições 1 e 3, e contar devolveria 3 — posição já ocupada, barrada pela
     * restrição {@code uk_listing_photos_listing_id_position}.
     */
    private int nextPhotoPosition(Listing listing) {
        return listing.getPhotos().stream()
                .map(ListingPhoto::getPosition)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(FIRST_PHOTO_POSITION - 1) + 1;
    }

    private void promoteCoverIfRemoved(Listing listing, ListingPhoto removed) {
        if (!removed.isCover() || listing.getPhotos().isEmpty()) {
            return;
        }
        listing.getPhotos().getFirst().setCover(true);
    }

    private ListingPhoto findPhotoOrThrow(Listing listing, Long photoId) {
        return listing.getPhotos().stream()
                .filter(photo -> photo.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.listingPhoto(photoId));
    }

    /**
     * Forma canônica de um critério de texto: minúsculas, ou nulo quando não há filtro.
     *
     * <p>Filtro em branco é ausência de filtro — {@code ?city=} não deve devolver zero
     * resultados. A normalização acontece aqui, e não na consulta, porque {@code lower(:city)}
     * sobre parâmetro nulo é recusado pelo PostgreSQL (ver {@code ListingRepository.search}).
     */
    private String normalizedOrNull(String criterion) {
        return ObjectUtils.isBlank(criterion) ? null : TextUtils.normalize(criterion);
    }

    private Listing getListingOrThrow(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.listing(id));
    }

    private ListingResponse toResponse(Listing listing) {
        return listingMapper.toResponse(listing, storageService.baseUrl());
    }
}
