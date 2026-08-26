package com.corretoria.service;

import com.corretoria.entity.RealtorFollow;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.SocialMapper;
import com.corretoria.repository.RealtorFollowRepository;
import com.corretoria.repository.UserRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.response.RealtorFollowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vínculo de acompanhamento entre cliente e corretor.
 *
 * <p>Idempotente nas duas pontas, como o favorito. O corretor é avisado apenas quando o
 * vínculo <strong>nasce</strong>: repetir a chamada não gera uma nova notificação, senão um
 * cliente indeciso encheria a caixa dele.
 */
@Service
public class RealtorFollowService {

    private final RealtorFollowRepository realtorFollowRepository;
    private final UserRepository userRepository;
    private final SocialMapper socialMapper;
    private final NotificationService notificationService;
    private final UserSecurity userSecurity;

    public RealtorFollowService(
            RealtorFollowRepository realtorFollowRepository,
            UserRepository userRepository,
            SocialMapper socialMapper,
            NotificationService notificationService,
            UserSecurity userSecurity) {
        this.realtorFollowRepository = realtorFollowRepository;
        this.userRepository = userRepository;
        this.socialMapper = socialMapper;
        this.notificationService = notificationService;
        this.userSecurity = userSecurity;
    }

    @Transactional
    public RealtorFollowResponse follow(Long realtorId, Authentication authentication) {
        User customer = userSecurity.getAuthenticatedUser(authentication);
        ensureCustomerRole(customer);
        ensureDistinctParties(customer, realtorId);

        return realtorFollowRepository
                .findByCustomerIdAndRealtorId(customer.getId(), realtorId)
                .map(socialMapper::toResponse)
                .orElseGet(() -> startFollowing(customer, realtorId));
    }

    @Transactional
    public void unfollow(Long realtorId, Authentication authentication) {
        User customer = userSecurity.getAuthenticatedUser(authentication);

        realtorFollowRepository
                .findByCustomerIdAndRealtorId(customer.getId(), realtorId)
                .ifPresent(realtorFollowRepository::delete);
    }

    @Transactional(readOnly = true)
    public Page<RealtorFollowResponse> findMine(Authentication authentication, Pageable pageable) {
        User customer = userSecurity.getAuthenticatedUser(authentication);

        return realtorFollowRepository.findByCustomerId(customer.getId(), pageable)
                .map(socialMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long countFollowersOf(Long realtorId) {
        return realtorFollowRepository.countByRealtorId(realtorId);
    }

    private RealtorFollowResponse startFollowing(User customer, Long realtorId) {
        User realtor = userRepository.findById(realtorId)
                .orElseThrow(() -> ResourceNotFoundException.user(realtorId));

        RealtorFollow follow = new RealtorFollow();
        follow.setCustomer(customer);
        follow.setRealtor(realtor);
        RealtorFollow saved = realtorFollowRepository.save(follow);

        notificationService.notify(realtor, NotificationType.NEW_FOLLOWER,
                "%s começou a acompanhar você.".formatted(customer.getName()));

        return socialMapper.toResponse(saved);
    }

    private void ensureDistinctParties(User customer, Long realtorId) {
        if (customer.getId().equals(realtorId)) {
            throw ForbiddenOperationException.cannotFollowYourself();
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
}
