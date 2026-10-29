package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.Review;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.ReviewNotAllowedException;
import com.corretoria.mapper.ReviewMapper;
import com.corretoria.repository.ProposalRepository;
import com.corretoria.repository.ReviewRepository;
import com.corretoria.repository.UserRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.ReviewRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long REALTOR_ID = 1L;
    private static final Long CUSTOMER_ID = 2L;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private ReviewService reviewService;

    @Captor
    private ArgumentCaptor<Review> reviewCaptor;

    private static Authentication anAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "cliente.demo", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    private static User aUser(Long id, RoleName... roles) {
        User user = new User();
        user.setId(id);
        user.setName("Usuario " + id);
        for (RoleName role : roles) {
            user.addRole(new Role(role));
        }
        return user;
    }

    private static ReviewRequest aRequest() {
        return new ReviewRequest(5, "Atendimento impecável.");
    }

    private void givenEligibleCustomer(Authentication authentication) {
        given(userSecurity.getAuthenticatedUser(authentication))
                .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
        given(userRepository.findById(REALTOR_ID))
                .willReturn(Optional.of(aUser(REALTOR_ID, RoleName.ROLE_REALTOR)));
        given(proposalRepository.hasNegotiationBetween(CUSTOMER_ID, REALTOR_ID)).willReturn(true);
    }

    @Nested
    @DisplayName("review")
    class ReviewOperacao {

        @Test
        @DisplayName("review - cliente que negociou - registra e avisa o corretor")
        void shouldCreateReviewForEligibleCustomer() {
            Authentication authentication = anAuthentication();
            givenEligibleCustomer(authentication);
            given(reviewRepository.findByRealtorIdAndCustomerId(REALTOR_ID, CUSTOMER_ID))
                    .willReturn(Optional.empty());
            given(reviewRepository.save(any())).willAnswer(call -> call.getArgument(0));

            reviewService.review(REALTOR_ID, aRequest(), authentication);

            then(reviewRepository).should().save(reviewCaptor.capture());
            Review saved = reviewCaptor.getValue();
            assertThat(saved.getRealtor().getId()).isEqualTo(REALTOR_ID);
            assertThat(saved.getCustomer().getId()).isEqualTo(CUSTOMER_ID);
            then(notificationService).should()
                    .notify(any(), eq(NotificationType.NEW_REVIEW), any());
        }

        @Test
        @DisplayName("review - segunda avaliação do mesmo par - reaproveita o registro existente")
        void shouldUpdateExistingReview() {
            // Uma avaliação por par: a segunda substitui a nota, e não acumula outra linha.
            Authentication authentication = anAuthentication();
            givenEligibleCustomer(authentication);

            Review existing = new Review();
            existing.setRealtor(aUser(REALTOR_ID, RoleName.ROLE_REALTOR));
            existing.setCustomer(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
            existing.setScore(2);
            given(reviewRepository.findByRealtorIdAndCustomerId(REALTOR_ID, CUSTOMER_ID))
                    .willReturn(Optional.of(existing));
            given(reviewRepository.save(existing)).willReturn(existing);

            ReviewRequest request = aRequest();
            reviewService.review(REALTOR_ID, request, authentication);

            then(reviewMapper).should().updateEntity(existing, request);
            then(reviewRepository).should().save(existing);
        }

        @Test
        @DisplayName("review - sem negociação prévia - lança ReviewNotAllowedException")
        void shouldRejectReviewWithoutNegotiation() {
            // A reputação só vale se for difícil de fabricar: é o "compra verificada" daqui.
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
            given(userRepository.findById(REALTOR_ID))
                    .willReturn(Optional.of(aUser(REALTOR_ID, RoleName.ROLE_REALTOR)));
            given(proposalRepository.hasNegotiationBetween(CUSTOMER_ID, REALTOR_ID))
                    .willReturn(false);

            assertThatThrownBy(() -> reviewService.review(REALTOR_ID, aRequest(), authentication))
                    .isInstanceOf(ReviewNotAllowedException.class)
                    .hasMessageContaining("negociado");

            then(reviewRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("review - corretor avaliando - lança ForbiddenOperationException")
        void shouldRejectReviewFromNonCustomer() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_REALTOR));
            given(userRepository.findById(REALTOR_ID))
                    .willReturn(Optional.of(aUser(REALTOR_ID, RoleName.ROLE_REALTOR)));

            assertThatThrownBy(() -> reviewService.review(REALTOR_ID, aRequest(), authentication))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("review - avaliando a si mesmo - lança ReviewNotAllowedException")
        void shouldRejectSelfReview() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_CUSTOMER, RoleName.ROLE_REALTOR));
            given(userRepository.findById(REALTOR_ID))
                    .willReturn(Optional.of(aUser(REALTOR_ID, RoleName.ROLE_REALTOR)));

            assertThatThrownBy(() -> reviewService.review(REALTOR_ID, aRequest(), authentication))
                    .isInstanceOf(ReviewNotAllowedException.class)
                    .hasMessageContaining("si mesmo");
        }
    }

    @Nested
    @DisplayName("averageScoreOf")
    class AverageScoreOf {

        @Test
        @DisplayName("averageScoreOf - corretor sem avaliações - zero")
        void shouldReturnZeroWhenThereIsNoReview() {
            given(reviewRepository.findAverageScoreByRealtorId(REALTOR_ID))
                    .willReturn(Optional.empty());

            assertThat(reviewService.averageScoreOf(REALTOR_ID)).isZero();
        }
    }
}
