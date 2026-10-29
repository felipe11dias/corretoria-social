package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.RealtorFollow;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.mapper.SocialMapper;
import com.corretoria.repository.RealtorFollowRepository;
import com.corretoria.repository.UserRepository;
import com.corretoria.security.UserSecurity;
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
class RealtorFollowServiceTest {

    private static final Long REALTOR_ID = 1L;
    private static final Long CUSTOMER_ID = 2L;

    @Mock
    private RealtorFollowRepository realtorFollowRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialMapper socialMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private RealtorFollowService realtorFollowService;

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

    @Nested
    @DisplayName("follow")
    class Follow {

        @Test
        @DisplayName("follow - vínculo novo - cria e avisa o corretor")
        void shouldNotifyOnFirstFollow() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
            given(realtorFollowRepository.findByCustomerIdAndRealtorId(CUSTOMER_ID, REALTOR_ID))
                    .willReturn(Optional.empty());
            given(userRepository.findById(REALTOR_ID))
                    .willReturn(Optional.of(aUser(REALTOR_ID, RoleName.ROLE_REALTOR)));
            given(realtorFollowRepository.save(any())).willAnswer(call -> call.getArgument(0));

            realtorFollowService.follow(REALTOR_ID, authentication);

            then(notificationService).should()
                    .notify(any(), eq(NotificationType.NEW_FOLLOWER), any());
        }

        @Test
        @DisplayName("follow - já seguia - não cria outro vínculo nem notifica de novo")
        void shouldNotNotifyTwice() {
            // Um cliente indeciso não deve encher a caixa do corretor.
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_CUSTOMER));
            given(realtorFollowRepository.findByCustomerIdAndRealtorId(CUSTOMER_ID, REALTOR_ID))
                    .willReturn(Optional.of(new RealtorFollow()));

            realtorFollowService.follow(REALTOR_ID, authentication);

            then(realtorFollowRepository).should(never()).save(any());
            then(notificationService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("follow - a si mesmo - lança ForbiddenOperationException")
        void shouldRejectSelfFollow() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(REALTOR_ID, RoleName.ROLE_CUSTOMER, RoleName.ROLE_REALTOR));

            assertThatThrownBy(() -> realtorFollowService.follow(REALTOR_ID, authentication))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("follow - corretor tentando seguir - lança ForbiddenOperationException")
        void shouldRejectNonCustomer() {
            Authentication authentication = anAuthentication();
            given(userSecurity.getAuthenticatedUser(authentication))
                    .willReturn(aUser(CUSTOMER_ID, RoleName.ROLE_REALTOR));

            assertThatThrownBy(() -> realtorFollowService.follow(REALTOR_ID, authentication))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }
}
