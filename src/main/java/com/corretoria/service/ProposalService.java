package com.corretoria.service;

import com.corretoria.entity.Listing;
import com.corretoria.entity.Proposal;
import com.corretoria.entity.ProposalMessage;
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
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.ProposalMapper;
import com.corretoria.repository.ListingRepository;
import com.corretoria.repository.ProposalRepository;
import com.corretoria.repository.SavedSearchRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.ProposalCreationRequest;
import com.corretoria.vo.v1.request.ProposalMessageCreationRequest;
import com.corretoria.vo.v1.request.ProposalStatusUpdateRequest;
import com.corretoria.vo.v1.response.ProposalMessageResponse;
import com.corretoria.vo.v1.response.ProposalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio da negociação.
 *
 * <p>A proposta é bidirecional, e a direção <strong>não vem do corpo da requisição</strong>:
 * ela é deduzida de quem está autenticado. Se quem propõe é o dono do anúncio, é o corretor
 * abordando um cliente a partir da vitrine de procura; se não é, é o cliente propondo sobre um
 * anúncio que viu. Deduzir em vez de perguntar elimina a possibilidade de alguém se declarar
 * do lado errado da mesa.
 *
 * <p>A participação nas escritas é barrada antes, no {@code @PreAuthorize} do controller, pelo
 * {@code ProposalSecurity}. Aqui ficam as regras que dependem do conteúdo da negociação: quem
 * decide o quê, e a partir de qual estado.
 */
@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ListingRepository listingRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final ProposalMapper proposalMapper;
    private final NotificationService notificationService;
    private final UserSecurity userSecurity;

    public ProposalService(
            ProposalRepository proposalRepository,
            ListingRepository listingRepository,
            SavedSearchRepository savedSearchRepository,
            ProposalMapper proposalMapper,
            NotificationService notificationService,
            UserSecurity userSecurity) {
        this.proposalRepository = proposalRepository;
        this.listingRepository = listingRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.proposalMapper = proposalMapper;
        this.notificationService = notificationService;
        this.userSecurity = userSecurity;
    }

    /**
     * Abre uma negociação, em qualquer das duas direções.
     *
     * <p>Anúncio fora do ar não aceita proposta: pausar ou concluir é justamente a forma de o
     * corretor dizer que aquilo não está mais em negociação.
     */
    @Transactional
    public ProposalResponse create(ProposalCreationRequest request, Authentication authentication) {
        User author = userSecurity.getAuthenticatedUser(authentication);
        Listing listing = getListingOrThrow(request.listingId());
        ensureListingIsActive(listing);

        Proposal proposal = proposalMapper.toEntity(request);
        proposal.setListing(listing);

        if (isOwnerOf(listing, author)) {
            prepareRealtorProposal(proposal, request, author);
        } else {
            prepareCustomerProposal(proposal, request, author);
        }

        Proposal created = proposalRepository.save(proposal);
        notifyCounterparty(created, NotificationType.NEW_PROPOSAL, buildCreationMessage(created));

        return proposalMapper.toResponse(created);
    }

    @Transactional(readOnly = true)
    public Page<ProposalResponse> findMine(Authentication authentication, Pageable pageable) {
        User participant = userSecurity.getAuthenticatedUser(authentication);

        return proposalRepository.findByParticipantId(participant.getId(), pageable)
                .map(proposalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProposalResponse findById(Long id) {
        return proposalMapper.toResponse(getProposalOrThrow(id));
    }

    /**
     * Aceita, recusa ou retira a proposta.
     *
     * <p>Quem pode o quê depende do lado da mesa: a contraparte decide, o iniciador retira.
     * Sem essa separação, quem propôs poderia aceitar a própria proposta.
     */
    @Transactional
    public ProposalResponse changeStatus(
            Long id, ProposalStatusUpdateRequest request, Authentication authentication) {

        Proposal proposal = getProposalOrThrow(id);
        User actor = userSecurity.getAuthenticatedUser(authentication);

        ensureProposalIsPending(proposal);
        ensureTransitionIsAllowed(proposal, request.status(), sideOf(proposal, actor));

        proposal.setStatus(request.status());
        Proposal decided = proposalRepository.save(proposal);

        notifyOther(decided, actor, NotificationType.PROPOSAL_STATUS_CHANGED,
                buildStatusMessage(decided));

        return proposalMapper.toResponse(decided);
    }

    /**
     * Acrescenta uma mensagem ao thread.
     *
     * <p>É também o mecanismo de contraproposta: sugerir outro valor é escrever aqui. O thread
     * continua aberto depois do desfecho — combinar detalhes de uma proposta aceita é conversa
     * legítima.
     */
    @Transactional
    public ProposalMessageResponse addMessage(
            Long id, ProposalMessageCreationRequest request, Authentication authentication) {

        Proposal proposal = getProposalOrThrow(id);
        User author = userSecurity.getAuthenticatedUser(authentication);

        ProposalMessage message = proposalMapper.toEntity(request);
        message.setAuthor(author);
        proposal.addMessage(message);

        // flush, e não save: ver ListingService.addPhoto — a resposta carrega o id gerado, e
        // save() sobre entidade com id é merge, que gravaria uma cópia do filho transiente.
        proposalRepository.flush();

        notifyOther(proposal, author, NotificationType.NEW_PROPOSAL_MESSAGE,
                buildMessageNotification(proposal, author));

        return proposalMapper.toMessageResponse(message);
    }

    /**
     * Proposta do corretor: ele oferece o próprio anúncio a quem publicou uma procura.
     *
     * <p>A busca pública é obrigatória aqui porque é ela que diz <em>a quem</em> a proposta se
     * destina — e é o que autoriza a abordagem: quem tornou a procura pública convidou.
     */
    private void prepareRealtorProposal(
            Proposal proposal, ProposalCreationRequest request, User realtor) {

        if (request.savedSearchId() == null) {
            throw InvalidProposalException.savedSearchRequiredForRealtor();
        }

        SavedSearch origin = savedSearchRepository.findById(request.savedSearchId())
                .orElseThrow(() -> ResourceNotFoundException.savedSearch(request.savedSearchId()));
        ensureSavedSearchIsPublic(origin);
        ensureCounterpartiesDiffer(origin.getCustomer(), realtor);

        proposal.setInitiator(ProposalInitiator.REALTOR);
        proposal.setCustomer(origin.getCustomer());
        proposal.setOriginSavedSearch(origin);
    }

    /** Proposta do cliente: ele propõe sobre um anúncio que encontrou no catálogo. */
    private void prepareCustomerProposal(
            Proposal proposal, ProposalCreationRequest request, User customer) {

        if (request.savedSearchId() != null) {
            throw InvalidProposalException.savedSearchNotAllowedForCustomer();
        }
        ensureCustomerRole(customer);

        proposal.setInitiator(ProposalInitiator.CUSTOMER);
        proposal.setCustomer(customer);
    }

    /**
     * De que lado da mesa está quem chama.
     *
     * <p>Só há dois lados, e a participação já foi conferida no {@code @PreAuthorize}: quem não
     * é o cliente da negociação é o corretor dono do anúncio.
     */
    private ProposalInitiator sideOf(Proposal proposal, User actor) {
        return proposal.getCustomer().getId().equals(actor.getId())
                ? ProposalInitiator.CUSTOMER
                : ProposalInitiator.REALTOR;
    }

    private void ensureTransitionIsAllowed(
            Proposal proposal, ProposalStatus target, ProposalInitiator actorSide) {

        switch (target) {
            case ACCEPTED, REJECTED -> {
                if (actorSide == proposal.getInitiator()) {
                    throw ForbiddenOperationException.onlyCounterpartyCanDecide();
                }
            }
            case WITHDRAWN -> {
                if (actorSide != proposal.getInitiator()) {
                    throw ForbiddenOperationException.onlyInitiatorCanWithdraw();
                }
            }
            case PENDING -> throw InvalidProposalException.cannotReturnToPending();
        }
    }

    private void ensureProposalIsPending(Proposal proposal) {
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw InvalidProposalException.alreadyDecided();
        }
    }

    private void ensureListingIsActive(Listing listing) {
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw InvalidProposalException.listingIsNotActive();
        }
    }

    private void ensureSavedSearchIsPublic(SavedSearch savedSearch) {
        if (savedSearch.getVisibility() != SearchVisibility.PUBLIC || !savedSearch.isActive()) {
            throw InvalidProposalException.savedSearchIsNotPublic();
        }
    }

    /**
     * Ninguém negocia consigo mesmo.
     *
     * <p>A checagem importa porque um mesmo usuário pode acumular os dois papéis: sem ela, um
     * corretor com {@code ROLE_CUSTOMER} proporia sobre o próprio anúncio.
     */
    private void ensureCounterpartiesDiffer(User customer, User realtor) {
        if (customer.getId().equals(realtor.getId())) {
            throw InvalidProposalException.cannotProposeOnOwnListing();
        }
    }

    private void ensureCustomerRole(User customer) {
        boolean hasCustomerRole = customer.getRoles().stream()
                .map(Role::getName)
                .anyMatch(RoleName.ROLE_CUSTOMER::equals);

        if (!hasCustomerRole) {
            throw ForbiddenOperationException.realtorCanOnlyProposeOwnListing();
        }
    }

    private boolean isOwnerOf(Listing listing, User user) {
        return listing.getRealtor().getId().equals(user.getId());
    }

    /** Avisa o lado que não abriu a negociação. */
    private void notifyCounterparty(Proposal proposal, NotificationType type, String message) {
        User recipient = proposal.getInitiator() == ProposalInitiator.CUSTOMER
                ? proposal.getListing().getRealtor()
                : proposal.getCustomer();

        notificationService.notify(recipient, type, message);
    }

    /** Avisa o participante que não é o autor da ação. */
    private void notifyOther(
            Proposal proposal, User actor, NotificationType type, String message) {

        User customer = proposal.getCustomer();
        User realtor = proposal.getListing().getRealtor();
        User recipient = customer.getId().equals(actor.getId()) ? realtor : customer;

        notificationService.notify(recipient, type, message);
    }

    private String buildCreationMessage(Proposal proposal) {
        return """
                Nova proposta de R$ %s sobre o anúncio "%s".
                Veja em /api/v1/proposals/%d."""
                .formatted(
                        proposal.getAmount().toPlainString(),
                        proposal.getListing().getTitle(),
                        proposal.getId());
    }

    private String buildStatusMessage(Proposal proposal) {
        return """
                A proposta de R$ %s sobre o anúncio "%s" agora está %s.
                Veja em /api/v1/proposals/%d."""
                .formatted(
                        proposal.getAmount().toPlainString(),
                        proposal.getListing().getTitle(),
                        proposal.getStatus(),
                        proposal.getId());
    }

    private String buildMessageNotification(Proposal proposal, User author) {
        return """
                %s enviou uma mensagem na proposta sobre o anúncio "%s".
                Veja em /api/v1/proposals/%d."""
                .formatted(
                        author.getName(),
                        proposal.getListing().getTitle(),
                        proposal.getId());
    }

    private Listing getListingOrThrow(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.listing(id));
    }

    private Proposal getProposalOrThrow(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.proposal(id));
    }
}
