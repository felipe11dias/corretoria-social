package com.corretoria.service;

import com.corretoria.entity.Role;
import com.corretoria.entity.SavedSearch;
import com.corretoria.entity.User;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.RoleName;
import com.corretoria.enums.SearchVisibility;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.InvalidSavedSearchException;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.SavedSearchMapper;
import com.corretoria.repository.SavedSearchRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.PropertySearchCriteriaRequest;
import com.corretoria.vo.v1.request.SavedSearchCreationRequest;
import com.corretoria.vo.v1.request.SavedSearchUpdateRequest;
import com.corretoria.vo.v1.request.VehicleSearchCriteriaRequest;
import com.corretoria.vo.v1.response.SavedSearchResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio da busca salva.
 *
 * <p>Só cliente procura: o corretor oferta. A mesma entidade atende ao alerta privado e ao
 * anúncio de procura público — o que muda é a visibilidade, e é ela que decide quem enxerga a
 * busca.
 *
 * <p>O cliente dono vem do token, nunca do corpo. A posse nas escritas é barrada antes, no
 * {@code @PreAuthorize} do controller, pelo {@code SavedSearchSecurity}.
 */
@Service
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final SavedSearchMapper savedSearchMapper;
    private final UserSecurity userSecurity;

    public SavedSearchService(
            SavedSearchRepository savedSearchRepository,
            SavedSearchMapper savedSearchMapper,
            UserSecurity userSecurity) {
        this.savedSearchRepository = savedSearchRepository;
        this.savedSearchMapper = savedSearchMapper;
        this.userSecurity = userSecurity;
    }

    @Transactional
    public SavedSearchResponse create(
            SavedSearchCreationRequest request, Authentication authentication) {

        User customer = userSecurity.getAuthenticatedUser(authentication);
        ensureCustomerRole(customer);
        ensureCriteriaMatchType(
                request.type(), request.propertyCriteria(), request.vehicleCriteria());
        ensurePriceRangeIsCoherent(request.minPrice(), request.maxPrice());
        ensureRadiusHasCenter(request.radiusKm(), request.latitude(), request.longitude());

        SavedSearch savedSearch = savedSearchMapper.toEntity(request);
        savedSearch.setCustomer(customer);

        return savedSearchMapper.toResponse(savedSearchRepository.save(savedSearch));
    }

    /**
     * Atualiza critérios, título, visibilidade e o estado ativo.
     *
     * <p>O tipo é imutável, como no anúncio: os critérios enviados são conferidos contra o tipo
     * <strong>já registrado</strong>.
     */
    @Transactional
    public SavedSearchResponse update(Long id, SavedSearchUpdateRequest request) {
        SavedSearch savedSearch = getSavedSearchOrThrow(id);
        ensureCriteriaMatchType(
                savedSearch.getType(), request.propertyCriteria(), request.vehicleCriteria());
        ensurePriceRangeIsCoherent(request.minPrice(), request.maxPrice());
        ensureRadiusHasCenter(request.radiusKm(), request.latitude(), request.longitude());

        savedSearchMapper.updateEntity(savedSearch, request);
        updateCriteria(savedSearch, request);

        return savedSearchMapper.toResponse(savedSearchRepository.save(savedSearch));
    }

    @Transactional
    public void delete(Long id) {
        savedSearchRepository.delete(getSavedSearchOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<SavedSearchResponse> findMine(Authentication authentication, Pageable pageable) {
        User customer = userSecurity.getAuthenticatedUser(authentication);

        return savedSearchRepository.findByCustomerId(customer.getId(), pageable)
                .map(savedSearchMapper::toResponse);
    }

    /**
     * Vitrine de procura: as buscas que os clientes tornaram públicas.
     *
     * <p>Só as ativas aparecem — uma procura pausada continua sendo do cliente, mas não é mais
     * um convite ao corretor.
     */
    @Transactional(readOnly = true)
    public Page<SavedSearchResponse> findPublic(Pageable pageable) {
        return savedSearchRepository
                .findByVisibilityAndActiveTrue(SearchVisibility.PUBLIC, pageable)
                .map(savedSearchMapper::toResponse);
    }

    private void updateCriteria(SavedSearch savedSearch, SavedSearchUpdateRequest request) {
        switch (savedSearch.getType()) {
            case PROPERTY -> updatePropertyCriteria(savedSearch, request.propertyCriteria());
            case VEHICLE -> updateVehicleCriteria(savedSearch, request.vehicleCriteria());
        }
    }

    /**
     * Critério ausente na requisição significa "sem critério específico": o registro
     * dependente é removido, e não mantido com os valores antigos.
     */
    private void updatePropertyCriteria(
            SavedSearch savedSearch, PropertySearchCriteriaRequest request) {

        if (request == null) {
            savedSearch.setPropertyCriteria(null);
        } else if (savedSearch.getPropertyCriteria() == null) {
            savedSearch.setPropertyCriteria(savedSearchMapper.toEntity(request));
        } else {
            savedSearchMapper.updatePropertyCriteria(savedSearch.getPropertyCriteria(), request);
        }
    }

    /** Ver {@link #updatePropertyCriteria}. */
    private void updateVehicleCriteria(
            SavedSearch savedSearch, VehicleSearchCriteriaRequest request) {

        if (request == null) {
            savedSearch.setVehicleCriteria(null);
        } else if (savedSearch.getVehicleCriteria() == null) {
            savedSearch.setVehicleCriteria(savedSearchMapper.toEntity(request));
        } else {
            savedSearchMapper.updateVehicleCriteria(savedSearch.getVehicleCriteria(), request);
        }
    }

    private void ensureCustomerRole(User customer) {
        boolean hasCustomerRole = customer.getRoles().stream()
                .map(Role::getName)
                .anyMatch(RoleName.ROLE_CUSTOMER::equals);

        if (!hasCustomerRole) {
            throw ForbiddenOperationException.customerRoleRequired();
        }
    }

    /**
     * Os critérios específicos são os do tipo da busca — ou nenhum.
     *
     * <p>Diferente do anúncio, aqui o detalhamento é opcional: uma busca por qualquer imóvel na
     * cidade é legítima. O que não existe é critério de automóvel numa busca de imóvel.
     */
    private void ensureCriteriaMatchType(
            ListingType type,
            PropertySearchCriteriaRequest propertyCriteria,
            VehicleSearchCriteriaRequest vehicleCriteria) {

        boolean coherent = switch (type) {
            case PROPERTY -> vehicleCriteria == null;
            case VEHICLE -> propertyCriteria == null;
        };

        if (!coherent) {
            throw InvalidSavedSearchException.criteriaDoNotMatchType();
        }
    }

    private void ensurePriceRangeIsCoherent(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw InvalidSavedSearchException.invertedPriceRange();
        }
    }

    private void ensureRadiusHasCenter(
            Integer radiusKm, BigDecimal latitude, BigDecimal longitude) {

        if (radiusKm != null && (latitude == null || longitude == null)) {
            throw InvalidSavedSearchException.radiusRequiresCoordinates();
        }
    }

    private SavedSearch getSavedSearchOrThrow(Long id) {
        return savedSearchRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.savedSearch(id));
    }
}
