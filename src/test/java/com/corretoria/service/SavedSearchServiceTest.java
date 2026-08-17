package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.PropertySearchCriteria;
import com.corretoria.entity.Role;
import com.corretoria.entity.SavedSearch;
import com.corretoria.entity.User;
import com.corretoria.entity.VehicleSearchCriteria;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.PropertyType;
import com.corretoria.enums.RoleName;
import com.corretoria.enums.SearchVisibility;
import com.corretoria.enums.VehicleType;
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
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class SavedSearchServiceTest {

    private static final Long SEARCH_ID = 7L;
    private static final Long CUSTOMER_ID = 3L;

    @Mock
    private SavedSearchRepository savedSearchRepository;

    @Mock
    private SavedSearchMapper savedSearchMapper;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private SavedSearchService savedSearchService;

    private static Authentication anAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "cliente.demo", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    private static User aUser(RoleName... roles) {
        User user = new User();
        user.setId(CUSTOMER_ID);
        user.setLogin("cliente.demo");
        for (RoleName role : roles) {
            user.addRole(new Role(role));
        }
        return user;
    }

    private static PropertySearchCriteriaRequest aPropertyCriteriaRequest() {
        return new PropertySearchCriteriaRequest(
                PropertyType.APARTMENT, 2, 1, new BigDecimal("60.00"));
    }

    private static VehicleSearchCriteriaRequest aVehicleCriteriaRequest() {
        return new VehicleSearchCriteriaRequest(VehicleType.CAR, "Fiat", 80000, 2018);
    }

    private static SavedSearchCreationRequest aCreationRequest(
            ListingType type,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer radiusKm,
            BigDecimal latitude,
            PropertySearchCriteriaRequest propertyCriteria,
            VehicleSearchCriteriaRequest vehicleCriteria) {

        return new SavedSearchCreationRequest(type, SearchVisibility.PRIVATE,
                "Apartamento na Aldeota até 500 mil", null, minPrice, maxPrice,
                "Fortaleza", "CE", null, latitude,
                latitude == null ? null : new BigDecimal("-38.5270"), radiusKm,
                propertyCriteria, vehicleCriteria);
    }

    private static SavedSearchCreationRequest aPropertyRequest() {
        return aCreationRequest(ListingType.PROPERTY, null, null, null, null,
                aPropertyCriteriaRequest(), null);
    }

    private static SavedSearchUpdateRequest anUpdateRequest(
            PropertySearchCriteriaRequest propertyCriteria) {

        return new SavedSearchUpdateRequest(SearchVisibility.PUBLIC, "Procuro apartamento",
                true, null, null, null, "Fortaleza", "CE", null, null, null, null,
                propertyCriteria, null);
    }

    private static SavedSearch aSavedSearch(ListingType type) {
        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setId(SEARCH_ID);
        savedSearch.setType(type);
        savedSearch.setTitle("Procuro apartamento");
        savedSearch.setCustomer(aUser(RoleName.ROLE_CUSTOMER));
        return savedSearch;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("create - cliente autenticado e critérios coerentes - cria em nome dele")
        void shouldCreateForAuthenticatedCustomer() {
            Authentication authentication = anAuthentication();
            User customer = aUser(RoleName.ROLE_CUSTOMER);
            SavedSearchCreationRequest request = aPropertyRequest();
            SavedSearch savedSearch = aSavedSearch(ListingType.PROPERTY);

            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(customer);
            given(savedSearchMapper.toEntity(request)).willReturn(savedSearch);
            given(savedSearchRepository.save(savedSearch)).willReturn(savedSearch);

            savedSearchService.create(request, authentication);

            // O dono sai do token: o contrato de criação sequer tem campo de cliente.
            assertThat(savedSearch.getCustomer()).isSameAs(customer);
        }

        @Test
        @DisplayName("create - usuário sem ROLE_CUSTOMER - lança ForbiddenOperationException")
        void shouldRejectCreationFromNonCustomer() {
            // Corretor oferta, cliente procura: quem anuncia não usa a vitrine de procura.
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_REALTOR));

            assertThatThrownBy(() ->
                    savedSearchService.create(aPropertyRequest(), authentication))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("ROLE_CUSTOMER");

            then(savedSearchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("create - critérios de veículo em busca de imóvel - lança InvalidSavedSearchException")
        void shouldRejectCriteriaOfAnotherType() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_CUSTOMER));

            assertThatThrownBy(() -> savedSearchService.create(
                    aCreationRequest(ListingType.PROPERTY, null, null, null, null,
                            null, aVehicleCriteriaRequest()), authentication))
                    .isInstanceOf(InvalidSavedSearchException.class);
        }

        @Test
        @DisplayName("create - busca sem critérios específicos - aceita")
        void shouldAcceptSearchWithoutSpecificCriteria() {
            // "Qualquer imóvel em Fortaleza" é uma procura legítima: diferente do anúncio,
            // aqui o detalhamento é opcional.
            Authentication authentication = anAuthentication();
            User customer = aUser(RoleName.ROLE_CUSTOMER);
            SavedSearchCreationRequest request =
                    aCreationRequest(ListingType.PROPERTY, null, null, null, null, null, null);
            SavedSearch savedSearch = aSavedSearch(ListingType.PROPERTY);

            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(customer);
            given(savedSearchMapper.toEntity(request)).willReturn(savedSearch);
            given(savedSearchRepository.save(savedSearch)).willReturn(savedSearch);

            savedSearchService.create(request, authentication);

            then(savedSearchRepository).should().save(savedSearch);
        }

        @Test
        @DisplayName("create - faixa de preço invertida - lança InvalidSavedSearchException")
        void shouldRejectInvertedPriceRange() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_CUSTOMER));

            assertThatThrownBy(() -> savedSearchService.create(
                    aCreationRequest(ListingType.PROPERTY, new BigDecimal("500000"),
                            new BigDecimal("100000"), null, null,
                            aPropertyCriteriaRequest(), null), authentication))
                    .isInstanceOf(InvalidSavedSearchException.class)
                    .hasMessageContaining("preço mínimo");
        }

        @Test
        @DisplayName("create - raio sem coordenada - lança InvalidSavedSearchException")
        void shouldRejectRadiusWithoutCoordinates() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(RoleName.ROLE_CUSTOMER));

            assertThatThrownBy(() -> savedSearchService.create(
                    aCreationRequest(ListingType.PROPERTY, null, null, 10, null,
                            aPropertyCriteriaRequest(), null), authentication))
                    .isInstanceOf(InvalidSavedSearchException.class)
                    .hasMessageContaining("latitude e longitude");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("update - critério ausente na requisição - remove o registro dependente")
        void shouldRemoveCriteriaWhenAbsent() {
            SavedSearch savedSearch = aSavedSearch(ListingType.PROPERTY);
            savedSearch.setPropertyCriteria(new PropertySearchCriteria());

            given(savedSearchRepository.findById(SEARCH_ID)).willReturn(Optional.of(savedSearch));
            given(savedSearchRepository.save(savedSearch)).willReturn(savedSearch);

            savedSearchService.update(SEARCH_ID, anUpdateRequest(null));

            assertThat(savedSearch.getPropertyCriteria()).isNull();
        }

        @Test
        @DisplayName("update - busca ainda sem critério - cria o registro dependente")
        void shouldCreateCriteriaWhenMissing() {
            SavedSearch savedSearch = aSavedSearch(ListingType.PROPERTY);
            PropertySearchCriteriaRequest criteriaRequest = aPropertyCriteriaRequest();
            PropertySearchCriteria criteria = new PropertySearchCriteria();

            given(savedSearchRepository.findById(SEARCH_ID)).willReturn(Optional.of(savedSearch));
            given(savedSearchMapper.toEntity(criteriaRequest)).willReturn(criteria);
            given(savedSearchRepository.save(savedSearch)).willReturn(savedSearch);

            savedSearchService.update(SEARCH_ID, anUpdateRequest(criteriaRequest));

            assertThat(savedSearch.getPropertyCriteria()).isSameAs(criteria);
            // O @MapsId exige a referência de volta fechada, senão não há chave primária.
            assertThat(criteria.getSavedSearch()).isSameAs(savedSearch);
        }

        @Test
        @DisplayName("update - critério existente - atualiza no lugar")
        void shouldUpdateExistingCriteria() {
            SavedSearch savedSearch = aSavedSearch(ListingType.PROPERTY);
            PropertySearchCriteria criteria = new PropertySearchCriteria();
            savedSearch.setPropertyCriteria(criteria);
            PropertySearchCriteriaRequest criteriaRequest = aPropertyCriteriaRequest();

            given(savedSearchRepository.findById(SEARCH_ID)).willReturn(Optional.of(savedSearch));
            given(savedSearchRepository.save(savedSearch)).willReturn(savedSearch);

            savedSearchService.update(SEARCH_ID, anUpdateRequest(criteriaRequest));

            then(savedSearchMapper).should().updatePropertyCriteria(criteria, criteriaRequest);
        }

        @Test
        @DisplayName("update - critérios de outro tipo - lança InvalidSavedSearchException")
        void shouldRejectCriteriaOfAnotherType() {
            SavedSearch savedSearch = aSavedSearch(ListingType.VEHICLE);
            given(savedSearchRepository.findById(SEARCH_ID)).willReturn(Optional.of(savedSearch));

            assertThatThrownBy(() ->
                    savedSearchService.update(SEARCH_ID, anUpdateRequest(aPropertyCriteriaRequest())))
                    .isInstanceOf(InvalidSavedSearchException.class);

            then(savedSearchRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("update - busca inexistente - lança ResourceNotFoundException")
        void shouldRejectUnknownSavedSearch() {
            given(savedSearchRepository.findById(SEARCH_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> savedSearchService.update(SEARCH_ID, anUpdateRequest(null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Busca salva não encontrada");
        }
    }

    @Nested
    @DisplayName("findPublic")
    class FindPublic {

        @Test
        @DisplayName("findPublic - vitrine traz apenas buscas públicas e ativas")
        void shouldListOnlyPublicAndActiveSearches() {
            // Uma procura pausada continua sendo do cliente, mas deixou de ser um convite.
            given(savedSearchRepository.findByVisibilityAndActiveTrue(
                    SearchVisibility.PUBLIC, Pageable.unpaged())).willReturn(Page.empty());

            savedSearchService.findPublic(Pageable.unpaged());

            then(savedSearchRepository).should()
                    .findByVisibilityAndActiveTrue(SearchVisibility.PUBLIC, Pageable.unpaged());
        }
    }
}
