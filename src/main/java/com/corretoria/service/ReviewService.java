package com.corretoria.service;

import com.corretoria.entity.Review;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.exception.ReviewNotAllowedException;
import com.corretoria.mapper.ReviewMapper;
import com.corretoria.repository.ProposalRepository;
import com.corretoria.repository.ReviewRepository;
import com.corretoria.repository.UserRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.ReviewRequest;
import com.corretoria.vo.v1.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio da reputação do corretor.
 *
 * <p>A nota só vale se for difícil de fabricar: avaliar exige ter negociado com o corretor —
 * basta a proposta ter existido, sem precisar ter sido aceita, porque um corretor que sumiu
 * depois de receber a proposta também merece ser avaliado.
 *
 * <p>Avaliar duas vezes o mesmo corretor <strong>substitui</strong> a nota. Não há histórico de
 * avaliações: o que a plataforma publica é a opinião atual de cada cliente.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final NotificationService notificationService;
    private final UserSecurity userSecurity;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProposalRepository proposalRepository,
            UserRepository userRepository,
            ReviewMapper reviewMapper,
            NotificationService notificationService,
            UserSecurity userSecurity) {
        this.reviewRepository = reviewRepository;
        this.proposalRepository = proposalRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
        this.notificationService = notificationService;
        this.userSecurity = userSecurity;
    }

    /**
     * Cria ou substitui a avaliação do cliente autenticado sobre o corretor.
     *
     * <p>Uma operação só, porque para o domínio é uma só: o cliente tem, a cada momento, uma
     * opinião sobre aquele corretor.
     */
    @Transactional
    public ReviewResponse review(Long realtorId, ReviewRequest request, Authentication authentication) {
        User customer = userSecurity.getAuthenticatedUser(authentication);
        User realtor = getUserOrThrow(realtorId);

        ensureCustomerRole(customer);
        ensureDistinctParties(customer, realtor);
        ensurePreviousNegotiation(customer, realtor);

        Review review = reviewRepository
                .findByRealtorIdAndCustomerId(realtorId, customer.getId())
                .orElseGet(() -> newReviewFor(realtor, customer));
        reviewMapper.updateEntity(review, request);

        Review saved = reviewRepository.save(review);
        notificationService.notify(realtor, NotificationType.NEW_REVIEW, buildMessage(customer, saved));

        return reviewMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> findByRealtor(Long realtorId, Pageable pageable) {
        return reviewRepository.findByRealtorId(realtorId, pageable)
                .map(reviewMapper::toResponse);
    }

    /** Média de todas as avaliações do corretor; zero quando ainda não há nenhuma. */
    @Transactional(readOnly = true)
    public double averageScoreOf(Long realtorId) {
        return reviewRepository.findAverageScoreByRealtorId(realtorId).orElse(0.0);
    }

    private Review newReviewFor(User realtor, User customer) {
        Review review = new Review();
        review.setRealtor(realtor);
        review.setCustomer(customer);
        return review;
    }

    private void ensurePreviousNegotiation(User customer, User realtor) {
        if (!proposalRepository.hasNegotiationBetween(customer.getId(), realtor.getId())) {
            throw ReviewNotAllowedException.requiresPreviousNegotiation();
        }
    }

    private void ensureDistinctParties(User customer, User realtor) {
        if (customer.getId().equals(realtor.getId())) {
            throw ReviewNotAllowedException.cannotReviewYourself();
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

    private String buildMessage(User customer, Review review) {
        return "%s avaliou você com nota %d.".formatted(customer.getName(), review.getScore());
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.user(id));
    }
}
