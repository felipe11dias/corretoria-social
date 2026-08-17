package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.Listing;
import com.corretoria.entity.ListingPhoto;
import com.corretoria.entity.Property;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.entity.Vehicle;
import com.corretoria.enums.FuelType;
import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import com.corretoria.enums.PropertyType;
import com.corretoria.enums.RoleName;
import com.corretoria.enums.TransmissionType;
import com.corretoria.enums.VehicleCondition;
import com.corretoria.enums.VehicleType;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.InvalidListingException;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.ListingMapper;
import com.corretoria.repository.ListingRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.shared.PhotoContent;
import com.corretoria.vo.v1.request.ListingCreationRequest;
import com.corretoria.vo.v1.request.ListingSearchRequest;
import com.corretoria.vo.v1.request.ListingStatusUpdateRequest;
import com.corretoria.vo.v1.request.ListingUpdateRequest;
import com.corretoria.vo.v1.request.PropertyRequest;
import com.corretoria.vo.v1.request.VehicleRequest;
import com.corretoria.vo.v1.response.ListingPhotoResponse;
import com.corretoria.vo.v1.response.ListingResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    private static final Long LISTING_ID = 10L;
    private static final Long REALTOR_ID = 1L;
    private static final Long OTHER_REALTOR_ID = 2L;
    private static final String PHOTO_BASE_URL = "http://localhost:9000/corretoria-listing-photos";
    private static final String STORAGE_KEY = "listings/10/foto.jpg";

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private StorageService storageService;

    @Mock
    private UserSecurity userSecurity;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ListingService listingService;

    private static Authentication anAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "corretor.demo", null, AuthorityUtils.createAuthorityList("ROLE_REALTOR"));
    }

    private static User aUser(Long id, RoleName... roles) {
        User user = new User();
        user.setId(id);
        user.setLogin("corretor.demo");
        for (RoleName role : roles) {
            user.addRole(new Role(role));
        }
        return user;
    }

    private static PropertyRequest aPropertyRequest() {
        return new PropertyRequest(PropertyType.APARTMENT, new BigDecimal("90.00"),
                3, 2, 1, new BigDecimal("450.00"), new BigDecimal("1200.00"), Set.of());
    }

    private static VehicleRequest aVehicleRequest() {
        return new VehicleRequest(VehicleType.CAR, "Fiat", "Argo", "Prata",
                2022, 2023, 30000, FuelType.FLEX, TransmissionType.AUTOMATIC,
                VehicleCondition.USED);
    }

    private static ListingCreationRequest aCreationRequest(
            ListingType type, OperationType operationType,
            PropertyRequest property, VehicleRequest vehicle) {

        return new ListingCreationRequest(type, operationType, "Apartamento na Aldeota",
                "Três quartos, nascente", new BigDecimal("450000.00"), "Aldeota", "Fortaleza",
                "CE", new BigDecimal("-3.7327"), new BigDecimal("-38.5270"), property, vehicle);
    }

    private static ListingUpdateRequest anUpdateRequest(
            OperationType operationType, PropertyRequest property, VehicleRequest vehicle) {

        return new ListingUpdateRequest(operationType, "Apartamento reformado",
                "Três quartos, nascente", new BigDecimal("470000.00"), "Aldeota", "Fortaleza",
                "CE", new BigDecimal("-3.7327"), new BigDecimal("-38.5270"), property, vehicle);
    }

    private static Listing aListing(ListingType type) {
        Listing listing = new Listing();
        listing.setId(LISTING_ID);
        listing.setType(type);
        listing.setOperationType(OperationType.SALE);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setRealtor(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));
        if (type == ListingType.PROPERTY) {
            listing.setProperty(new Property());
        } else {
            listing.setVehicle(new Vehicle());
        }
        return listing;
    }

    private static ListingPhoto aPhoto(Long id, int position, boolean cover) {
        ListingPhoto photo = new ListingPhoto();
        photo.setId(id);
        photo.setStorageKey("listings/10/foto-" + id + ".jpg");
        photo.setPosition(position);
        photo.setCover(cover);
        return photo;
    }

    private static ListingSearchRequest aSearchRequest(ListingStatus status, Long realtorId) {
        return new ListingSearchRequest(null, null, status, realtorId, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static ListingResponse aListingResponse() {
        return new ListingResponse(LISTING_ID, ListingType.PROPERTY, OperationType.SALE,
                ListingStatus.ACTIVE, "Apartamento na Aldeota", "Três quartos, nascente",
                new BigDecimal("450000.00"), "Aldeota", "Fortaleza", "CE",
                new BigDecimal("-3.7327"), new BigDecimal("-38.5270"),
                null, null, null, java.util.List.of(), null, null);
    }

    private void givenResponseIsMappedFor(Listing listing) {
        given(storageService.baseUrl()).willReturn(PHOTO_BASE_URL);
        given(listingMapper.toResponse(listing, PHOTO_BASE_URL)).willReturn(aListingResponse());
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("publish - corretor autenticado e detalhamento coerente - publica em nome dele")
        void shouldPublishForAuthenticatedRealtor() {
            // arrange
            Authentication authentication = anAuthentication();
            User realtor = aUser(REALTOR_ID, RoleName.ROLE_REALTOR);
            ListingCreationRequest request = aCreationRequest(
                    ListingType.PROPERTY, OperationType.SALE, aPropertyRequest(), null);
            Listing listing = aListing(ListingType.PROPERTY);

            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(realtor);
            given(listingMapper.toEntity(request)).willReturn(listing);
            given(listingRepository.save(listing)).willReturn(listing);
            givenResponseIsMappedFor(listing);

            // act
            ListingResponse response = listingService.publish(request, authentication);

            // assert
            assertThat(response.id()).isEqualTo(LISTING_ID);
            // O dono sai do token: o contrato de publicação sequer tem campo de corretor.
            assertThat(listing.getRealtor()).isSameAs(realtor);
        }

        @Test
        @DisplayName("publish - usuário sem ROLE_REALTOR - lança ForbiddenOperationException")
        void shouldRejectPublicationFromNonRealtor() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_CUSTOMER));

            assertThatThrownBy(() -> listingService.publish(
                    aCreationRequest(ListingType.PROPERTY, OperationType.SALE,
                            aPropertyRequest(), null), authentication))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("ROLE_REALTOR");

            then(listingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("publish - tipo PROPERTY sem detalhamento de imóvel - lança InvalidListingException")
        void shouldRejectPropertyWithoutDetail() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));

            assertThatThrownBy(() -> listingService.publish(
                    aCreationRequest(ListingType.PROPERTY, OperationType.SALE, null, null),
                    authentication))
                    .isInstanceOf(InvalidListingException.class)
                    .hasMessageContaining("property");
        }

        @Test
        @DisplayName("publish - os dois detalhamentos preenchidos - lança InvalidListingException")
        void shouldRejectBothDetails() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));

            assertThatThrownBy(() -> listingService.publish(
                    aCreationRequest(ListingType.PROPERTY, OperationType.SALE,
                            aPropertyRequest(), aVehicleRequest()), authentication))
                    .isInstanceOf(InvalidListingException.class)
                    .hasMessageContaining("único detalhamento");
        }

        @Test
        @DisplayName("publish - veículo com operação RENT - lança InvalidListingException")
        void shouldRejectVehicleForRent() {
            // Aluguel de automóvel é operação de locadora: a mesma regra está no CHECK
            // ck_listings_vehicle_only_sale, para valer também fora da aplicação.
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));

            assertThatThrownBy(() -> listingService.publish(
                    aCreationRequest(ListingType.VEHICLE, OperationType.RENT,
                            null, aVehicleRequest()), authentication))
                    .isInstanceOf(InvalidListingException.class)
                    .hasMessageContaining("SALE");

            then(listingRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("update - detalhamento do tipo já registrado - atualiza")
        void shouldUpdateDetailOfRegisteredType() {
            Listing listing = aListing(ListingType.PROPERTY);
            ListingUpdateRequest request = anUpdateRequest(
                    OperationType.SALE, aPropertyRequest(), null);

            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(listingRepository.save(listing)).willReturn(listing);
            givenResponseIsMappedFor(listing);

            listingService.update(LISTING_ID, request);

            then(listingMapper).should().updateEntity(listing, request);
            then(listingMapper).should().updateProperty(listing.getProperty(), request.property());
        }

        @Test
        @DisplayName("update - detalhamento de outro tipo - lança InvalidListingException")
        void shouldRejectDetailOfAnotherType() {
            // O tipo é imutável: mandar "vehicle" para um anúncio de imóvel não converte nada.
            Listing listing = aListing(ListingType.PROPERTY);
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.update(LISTING_ID,
                    anUpdateRequest(OperationType.SALE, null, aVehicleRequest())))
                    .isInstanceOf(InvalidListingException.class);

            then(listingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("update - anúncio inexistente - lança ResourceNotFoundException")
        void shouldRejectUnknownListing() {
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.update(LISTING_ID,
                    anUpdateRequest(OperationType.SALE, aPropertyRequest(), null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Anúncio não encontrado");
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("changeStatus - anúncio existente - aplica a nova situação")
        void shouldChangeStatus() {
            Listing listing = aListing(ListingType.PROPERTY);
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(listingRepository.save(listing)).willReturn(listing);
            givenResponseIsMappedFor(listing);

            listingService.changeStatus(LISTING_ID,
                    new ListingStatusUpdateRequest(ListingStatus.PAUSED));

            assertThat(listing.getStatus()).isEqualTo(ListingStatus.PAUSED);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("search - sem filtro de situação - consulta apenas anúncios ativos")
        void shouldSearchOnlyActiveByDefault() {
            given(storageService.baseUrl()).willReturn(PHOTO_BASE_URL);
            given(listingRepository.search(any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any())).willReturn(org.springframework.data.domain.Page.empty());

            listingService.search(aSearchRequest(null, null),
                    org.springframework.data.domain.Pageable.unpaged(), null);

            then(listingRepository).should().search(
                    org.mockito.ArgumentMatchers.eq(ListingStatus.ACTIVE),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("search - situação não ativa por visitante anônimo - lança ForbiddenOperationException")
        void shouldRejectNonActiveSearchFromAnonymous() {
            // Pausar tira o anúncio da vitrine; deixar qualquer visitante listá-lo pelo filtro
            // de situação anularia a decisão do corretor.
            given(userSecurity.isAdmin(null)).willReturn(false);
            given(userSecurity.findAuthenticatedUser(null)).willReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.search(
                    aSearchRequest(ListingStatus.PAUSED, null),
                    org.springframework.data.domain.Pageable.unpaged(), null))
                    .isInstanceOf(ForbiddenOperationException.class);

            then(listingRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("search - situação não ativa filtrando os próprios anúncios - permite")
        void shouldAllowNonActiveSearchForOwnListings() {
            Authentication authentication = anAuthentication();
            given(userSecurity.isAdmin(authentication)).willReturn(false);
            given(userSecurity.findAuthenticatedUser(authentication))
                    .willReturn(Optional.of(aUser(REALTOR_ID, RoleName.ROLE_REALTOR)));
            given(storageService.baseUrl()).willReturn(PHOTO_BASE_URL);
            given(listingRepository.search(any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any())).willReturn(org.springframework.data.domain.Page.empty());

            listingService.search(aSearchRequest(ListingStatus.PAUSED, REALTOR_ID),
                    org.springframework.data.domain.Pageable.unpaged(), authentication);

            then(listingRepository).should().search(any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any());
        }

        @Test
        @DisplayName("search - situação não ativa de outro corretor - lança ForbiddenOperationException")
        void shouldRejectNonActiveSearchOfAnotherRealtor() {
            Authentication authentication = anAuthentication();
            given(userSecurity.isAdmin(authentication)).willReturn(false);
            given(userSecurity.findAuthenticatedUser(authentication))
                    .willReturn(Optional.of(aUser(REALTOR_ID, RoleName.ROLE_REALTOR)));

            assertThatThrownBy(() -> listingService.search(
                    aSearchRequest(ListingStatus.PAUSED, OTHER_REALTOR_ID),
                    org.springframework.data.domain.Pageable.unpaged(), authentication))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("search - raio sem coordenada - lança IllegalArgumentException")
        void shouldRejectRadiusWithoutCoordinates() {
            ListingSearchRequest request = new ListingSearchRequest(null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null,
                    null, null, 10);

            assertThatThrownBy(() -> listingService.search(
                    request, org.springframework.data.domain.Pageable.unpaged(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("latitude e longitude");
        }

        @Test
        @DisplayName("search - preço mínimo maior que o máximo - lança IllegalArgumentException")
        void shouldRejectInvertedPriceRange() {
            ListingSearchRequest request = new ListingSearchRequest(null, null, null, null, null,
                    null, null, new BigDecimal("500000"), new BigDecimal("100000"), null, null,
                    null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> listingService.search(
                    request, org.springframework.data.domain.Pageable.unpaged(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("preço mínimo");
        }
    }

    @Nested
    @DisplayName("addPhoto")
    class AddPhoto {

        private static PhotoContent aPhotoContent() {
            return new PhotoContent("foto".getBytes(StandardCharsets.UTF_8), "image/jpeg");
        }

        @Test
        @DisplayName("addPhoto - primeira foto - vira capa na posição 1")
        void shouldMakeFirstPhotoTheCover() {
            Listing listing = aListing(ListingType.PROPERTY);
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(storageService.store(any(), any())).willReturn(STORAGE_KEY);
            given(storageService.baseUrl()).willReturn(PHOTO_BASE_URL);
            given(listingMapper.toPhotoResponse(any(), any()))
                    .willReturn(new ListingPhotoResponse(1L, PHOTO_BASE_URL, 1, true));

            listingService.addPhoto(LISTING_ID, aPhotoContent());

            assertThat(listing.getPhotos()).hasSize(1);
            assertThat(listing.getPhotos().getFirst().isCover()).isTrue();
            assertThat(listing.getPhotos().getFirst().getPosition()).isEqualTo(1);
            assertThat(listing.getPhotos().getFirst().getStorageKey()).isEqualTo(STORAGE_KEY);
            // A resposta carrega o id da foto, gerado pelo INSERT: sem a descarga explícita, a
            // inserção em cascata só ocorreria no commit e o cliente receberia id nulo.
            then(listingRepository).should().flush();
            // E nunca por save(): para entidade com id, save é merge, e o merge de um filho
            // transiente grava uma cópia — o id nasceria fora do objeto que vira resposta.
            then(listingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("addPhoto - com fotos anteriores - continua da maior posição e não é capa")
        void shouldContinueFromHighestPosition() {
            // Excluir a foto 2 de três deixa as posições 1 e 3: contar as fotos devolveria 3,
            // posição já ocupada, e a restrição de unicidade barraria o INSERT.
            Listing listing = aListing(ListingType.PROPERTY);
            listing.addPhoto(aPhoto(1L, 1, true));
            listing.addPhoto(aPhoto(3L, 3, false));

            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(storageService.store(any(), any())).willReturn(STORAGE_KEY);
            given(storageService.baseUrl()).willReturn(PHOTO_BASE_URL);
            given(listingMapper.toPhotoResponse(any(), any()))
                    .willReturn(new ListingPhotoResponse(4L, PHOTO_BASE_URL, 4, false));

            listingService.addPhoto(LISTING_ID, aPhotoContent());

            ListingPhoto added = listing.getPhotos().getLast();
            assertThat(added.getPosition()).isEqualTo(4);
            assertThat(added.isCover()).isFalse();
        }

        @Test
        @DisplayName("addPhoto - limite de fotos atingido - lança InvalidListingException")
        void shouldRejectWhenPhotoLimitIsReached() {
            Listing listing = aListing(ListingType.PROPERTY);
            for (int position = 1; position <= 20; position++) {
                listing.addPhoto(aPhoto((long) position, position, position == 1));
            }
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.addPhoto(LISTING_ID, aPhotoContent()))
                    .isInstanceOf(InvalidListingException.class)
                    .hasMessageContaining("20");

            then(storageService).should(never()).store(any(), any());
        }
    }

    @Nested
    @DisplayName("deletePhoto")
    class DeletePhoto {

        @Test
        @DisplayName("deletePhoto - remove a capa - promove a foto seguinte")
        void shouldPromoteNextPhotoWhenCoverIsRemoved() {
            Listing listing = aListing(ListingType.PROPERTY);
            ListingPhoto cover = aPhoto(1L, 1, true);
            ListingPhoto second = aPhoto(2L, 2, false);
            listing.addPhoto(cover);
            listing.addPhoto(second);
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));

            listingService.deletePhoto(LISTING_ID, 1L);

            assertThat(listing.getPhotos()).containsExactly(second);
            assertThat(second.isCover()).isTrue();
            then(storageService).should().delete(cover.getStorageKey());
        }

        @Test
        @DisplayName("deletePhoto - foto de outro anúncio - lança ResourceNotFoundException")
        void shouldRejectPhotoFromAnotherListing() {
            Listing listing = aListing(ListingType.PROPERTY);
            listing.addPhoto(aPhoto(1L, 1, true));
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));

            assertThatThrownBy(() -> listingService.deletePhoto(LISTING_ID, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Foto não encontrada");

            then(storageService).should(never()).delete(any());
        }
    }
}
