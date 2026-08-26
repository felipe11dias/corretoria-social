package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.Listing;
import com.corretoria.entity.Proposal;
import com.corretoria.entity.Role;
import com.corretoria.entity.SavedSearch;
import com.corretoria.entity.User;
import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.ProposalInitiator;
import com.corretoria.enums.ProposalStatus;
import com.corretoria.enums.RoleName;
import com.corretoria.enums.SearchVisibility;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.InvalidProposalException;
import com.corretoria.mapper.ProposalMapper;
import com.corretoria.repository.ListingRepository;
import com.corretoria.repository.ProposalRepository;
import com.corretoria.repository.SavedSearchRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.ProposalCreationRequest;
import com.corretoria.vo.v1.request.ProposalStatusUpdateRequest;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    private static final Long LISTING_ID = 10L;
    private static final Long PROPOSAL_ID = 30L;
    private static final Long SEARCH_ID = 7L;
    private static final Long REALTOR_ID = 1L;
    private static final Long CUSTOMER_ID = 2L;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private SavedSearchRepository savedSearchRepository;

    @Mock
    private ProposalMapper proposalMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private ProposalService proposalService;

    private static Authentication anAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "usuario.demo", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    private static User aUser(Long id, RoleName... roles) {
        User user = new User();
        user.setId(id);
        user.setName("Usuário " + id);
        for (RoleName role : roles) {
            user.addRole(new Role(role));
        }
        return user;
    }

    private static Listing aListing(ListingStatus status) {
        Listing listing = new Listing();
        listing.setId(LISTING_ID);
        listing.setTitle("Apartamento na Aldeota");
        listing.setStatus(status);
        listing.setRealtor(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));
        return listing;
    }

    private static SavedSearch aPublicSearch(User customer) {
        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setId(SEARCH_ID);
        savedSearch.setTitle("Procuro apartamento");
        savedSearch.setVisibility(SearchVisibility.PUBLIC);
        savedSearch.setActive(true);
        savedSearch.setCustomer(customer);
        return savedSearch;
    }

    private static Proposal aProposal(ProposalInitiator initiator, ProposalStatus status) {
        Proposal proposal = new Proposal();
        proposal.setId(PROPOSAL_ID);
        proposal.setListing(aListing(ListingStatus.ACTIVE));
        proposal.setCustomer(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
        proposal.setInitiator(initiator);
        proposal.setStatus(status);
        proposal.setAmount(new BigDecimal("430000.00"));
        return proposal;
    }

    private static ProposalCreationRequest aRequest(Long savedSearchId) {
        return new ProposalCreationRequest(LISTING_ID, savedSearchId, new BigDecimal("430000.00"));
    }

    /** O que o mapeador devolve na criação: só o valor proposto, sem papéis nem anúncio. */
    private static Proposal aMappedProposal() {
        Proposal proposal = new Proposal();
        proposal.setAmount(new BigDecimal("430000.00"));
        return proposal;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("create - cliente sobre anúncio de outro - inicia como CUSTOMER e avisa o corretor")
        void shouldCreateCustomerProposal() {
            Authentication authentication = anAuthentication();
            User customer = aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER);
            Listing listing = aListing(ListingStatus.ACTIVE);
            // O mapeador real traz o valor proposto; o duplo precisa refletir isso, porque a
            // mensagem da notificação cita o valor.
            Proposal proposal = aMappedProposal();

            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(customer);
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(proposalMapper.toEntity(any(ProposalCreationRequest.class))).willReturn(proposal);
            given(proposalRepository.save(proposal)).willReturn(proposal);

            proposalService.create(aRequest(null), authentication);

            assertThat(proposal.getInitiator()).isEqualTo(ProposalInitiator.CUSTOMER);
            assertThat(proposal.getCustomer()).isSameAs(customer);
            assertThat(proposal.getOriginSavedSearch()).isNull();
            // O avisado é o outro lado: quem propôs já sabe que propôs.
            then(notificationService).should()
                    .notify(eq(listing.getRealtor()), eq(NotificationType.NEW_PROPOSAL), any());
        }

        @Test
        @DisplayName("create - corretor a partir de busca pública - inicia como REALTOR e guarda a origem")
        void shouldCreateRealtorProposal() {
            Authentication authentication = anAuthentication();
            User realtor = aUser(REALTOR_ID, RoleName.ROLE_REALTOR);
            User customer = aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER);
            Listing listing = aListing(ListingStatus.ACTIVE);
            SavedSearch origin = aPublicSearch(customer);
            Proposal proposal = aMappedProposal();

            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(realtor);
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(savedSearchRepository.findById(SEARCH_ID)).willReturn(Optional.of(origin));
            given(proposalMapper.toEntity(any(ProposalCreationRequest.class))).willReturn(proposal);
            given(proposalRepository.save(proposal)).willReturn(proposal);

            proposalService.create(aRequest(SEARCH_ID), authentication);

            assertThat(proposal.getInitiator()).isEqualTo(ProposalInitiator.REALTOR);
            // A contraparte cliente é a dona da busca, não quem chamou.
            assertThat(proposal.getCustomer()).isSameAs(customer);
            assertThat(proposal.getOriginSavedSearch()).isSameAs(origin);
            then(notificationService).should()
                    .notify(eq(customer), eq(NotificationType.NEW_PROPOSAL), any());
        }

        @Test
        @DisplayName("create - corretor sem informar a busca de origem - lança InvalidProposalException")
        void shouldRejectRealtorProposalWithoutOrigin() {
            // Sem a busca pública não há a quem propor, nem o que autorize a abordagem.
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));
            given(listingRepository.findById(LISTING_ID))
                    .willReturn(Optional.of(aListing(ListingStatus.ACTIVE)));
            given(proposalMapper.toEntity(any(ProposalCreationRequest.class)))
                    .willReturn(new Proposal());

            assertThatThrownBy(() -> proposalService.create(aRequest(null), authentication))
                    .isInstanceOf(InvalidProposalException.class)
                    .hasMessageContaining("savedSearchId");

            then(proposalRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("create - cliente informando busca de origem - lança InvalidProposalException")
        void shouldRejectCustomerProposalWithOrigin() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
            given(listingRepository.findById(LISTING_ID))
                    .willReturn(Optional.of(aListing(ListingStatus.ACTIVE)));
            given(proposalMapper.toEntity(any(ProposalCreationRequest.class)))
                    .willReturn(new Proposal());

            assertThatThrownBy(() -> proposalService.create(aRequest(SEARCH_ID), authentication))
                    .isInstanceOf(InvalidProposalException.class);
        }

        @Test
        @DisplayName("create - busca privada como origem - lança InvalidProposalException")
        void shouldRejectPrivateSearchAsOrigin() {
            // Procura privada é alerta pessoal, não convite ao corretor.
            Authentication authentication = anAuthentication();
            SavedSearch privateSearch = aPublicSearch(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
            privateSearch.setVisibility(SearchVisibility.PRIVATE);

            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));
            given(listingRepository.findById(LISTING_ID))
                    .willReturn(Optional.of(aListing(ListingStatus.ACTIVE)));
            given(savedSearchRepository.findById(SEARCH_ID)).willReturn(Optional.of(privateSearch));
            given(proposalMapper.toEntity(any(ProposalCreationRequest.class)))
                    .willReturn(new Proposal());

            assertThatThrownBy(() -> proposalService.create(aRequest(SEARCH_ID), authentication))
                    .isInstanceOf(InvalidProposalException.class)
                    .hasMessageContaining("pública");
        }
    }

    @Nested
    @DisplayName("create - invariantes")
    class CreateInvariantes {

        @Test
        @DisplayName("create - anúncio pausado - lança InvalidProposalException")
        void shouldRejectProposalOnInactiveListing() {
            // Pausar é justamente a forma de dizer que aquilo saiu de negociação.
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
            given(listingRepository.findById(LISTING_ID))
                    .willReturn(Optional.of(aListing(ListingStatus.PAUSED)));

            assertThatThrownBy(() -> proposalService.create(aRequest(null), authentication))
                    .isInstanceOf(InvalidProposalException.class)
                    .hasMessageContaining("ativos");

            then(proposalRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("create - dono da busca é o próprio corretor - lança InvalidProposalException")
        void shouldRejectProposalToOwnSearch() {
            // Um mesmo usuário pode acumular os dois papéis: sem a checagem, negociaria consigo.
            Authentication authentication = anAuthentication();
            User realtor = aUser(REALTOR_ID, RoleName.ROLE_REALTOR, RoleName.ROLE_CUSTOMER);

            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(realtor);
            given(listingRepository.findById(LISTING_ID))
                    .willReturn(Optional.of(aListing(ListingStatus.ACTIVE)));
            given(savedSearchRepository.findById(SEARCH_ID))
                    .willReturn(Optional.of(aPublicSearch(realtor)));
            given(proposalMapper.toEntity(any(ProposalCreationRequest.class)))
                    .willReturn(new Proposal());

            assertThatThrownBy(() -> proposalService.create(aRequest(SEARCH_ID), authentication))
                    .isInstanceOf(InvalidProposalException.class)
                    .hasMessageContaining("próprio anúncio");
        }

        @Test
        @DisplayName("create - corretor sobre anúncio alheio sem ser cliente - lança ForbiddenOperationException")
        void shouldRejectRealtorActingAsBuyerWithoutCustomerRole() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(99L, RoleName.ROLE_REALTOR));
            given(listingRepository.findById(LISTING_ID))
                    .willReturn(Optional.of(aListing(ListingStatus.ACTIVE)));
            given(proposalMapper.toEntity(any(ProposalCreationRequest.class)))
                    .willReturn(new Proposal());

            assertThatThrownBy(() -> proposalService.create(aRequest(null), authentication))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        private void givenProposal(Proposal proposal, User actor) {
            given(proposalRepository.findById(PROPOSAL_ID)).willReturn(Optional.of(proposal));
            given(userSecurity.getAuthenticatedUser(any())).willReturn(actor);
        }

        @Test
        @DisplayName("changeStatus - contraparte aceita proposta pendente - aceita e avisa quem propôs")
        void shouldLetCounterpartyAccept() {
            Proposal proposal = aProposal(ProposalInitiator.CUSTOMER, ProposalStatus.PENDING);
            User realtor = proposal.getListing().getRealtor();
            givenProposal(proposal, realtor);
            given(proposalRepository.save(proposal)).willReturn(proposal);

            proposalService.changeStatus(PROPOSAL_ID,
                    new ProposalStatusUpdateRequest(ProposalStatus.ACCEPTED), anAuthentication());

            assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
            then(notificationService).should().notify(eq(proposal.getCustomer()),
                    eq(NotificationType.PROPOSAL_STATUS_CHANGED), any());
        }

        @Test
        @DisplayName("changeStatus - quem propôs tentando aceitar - lança ForbiddenOperationException")
        void shouldRejectInitiatorAccepting() {
            // Sem esta separação, propor e aceitar viraria a mesma ação.
            Proposal proposal = aProposal(ProposalInitiator.CUSTOMER, ProposalStatus.PENDING);
            givenProposal(proposal, proposal.getCustomer());

            assertThatThrownBy(() -> proposalService.changeStatus(PROPOSAL_ID,
                    new ProposalStatusUpdateRequest(ProposalStatus.ACCEPTED), anAuthentication()))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("contraparte");

            then(proposalRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("changeStatus - quem propôs retira - retira")
        void shouldLetInitiatorWithdraw() {
            Proposal proposal = aProposal(ProposalInitiator.CUSTOMER, ProposalStatus.PENDING);
            givenProposal(proposal, proposal.getCustomer());
            given(proposalRepository.save(proposal)).willReturn(proposal);

            proposalService.changeStatus(PROPOSAL_ID,
                    new ProposalStatusUpdateRequest(ProposalStatus.WITHDRAWN), anAuthentication());

            assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("changeStatus - contraparte tentando retirar - lança ForbiddenOperationException")
        void shouldRejectCounterpartyWithdrawing() {
            Proposal proposal = aProposal(ProposalInitiator.CUSTOMER, ProposalStatus.PENDING);
            givenProposal(proposal, proposal.getListing().getRealtor());

            assertThatThrownBy(() -> proposalService.changeStatus(PROPOSAL_ID,
                    new ProposalStatusUpdateRequest(ProposalStatus.WITHDRAWN), anAuthentication()))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("changeStatus - proposta já aceita - lança InvalidProposalException")
        void shouldRejectDecidingTwice() {
            Proposal proposal = aProposal(ProposalInitiator.CUSTOMER, ProposalStatus.ACCEPTED);
            givenProposal(proposal, proposal.getListing().getRealtor());

            assertThatThrownBy(() -> proposalService.changeStatus(PROPOSAL_ID,
                    new ProposalStatusUpdateRequest(ProposalStatus.REJECTED), anAuthentication()))
                    .isInstanceOf(InvalidProposalException.class)
                    .hasMessageContaining("definitivos");
        }

        @Test
        @DisplayName("changeStatus - voltar para PENDING - lança InvalidProposalException")
        void shouldRejectReturningToPending() {
            Proposal proposal = aProposal(ProposalInitiator.REALTOR, ProposalStatus.PENDING);
            givenProposal(proposal, proposal.getCustomer());

            assertThatThrownBy(() -> proposalService.changeStatus(PROPOSAL_ID,
                    new ProposalStatusUpdateRequest(ProposalStatus.PENDING), anAuthentication()))
                    .isInstanceOf(InvalidProposalException.class);
        }
    }
}
