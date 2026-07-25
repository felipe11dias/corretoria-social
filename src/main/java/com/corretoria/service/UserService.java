package com.corretoria.service;

import com.corretoria.entity.Address;
import com.corretoria.entity.Role;
import com.corretoria.entity.User;
import com.corretoria.enums.RoleName;
import com.corretoria.exception.DuplicateResourceException;
import com.corretoria.exception.ForbiddenOperationException;
import com.corretoria.exception.InvalidPasswordException;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.AddressMapper;
import com.corretoria.mapper.UserMapper;
import com.corretoria.repository.RoleRepository;
import com.corretoria.repository.UserRepository;
import com.corretoria.vo.shared.Email;
import com.corretoria.vo.shared.ZipCode;
import com.corretoria.vo.v1.request.PasswordChangeRequest;
import com.corretoria.vo.v1.request.UserRegistrationRequest;
import com.corretoria.vo.v1.request.UserUpdateRequest;
import com.corretoria.vo.v1.response.UserResponse;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio do cadastro de usuários.
 *
 * <p>Lança exceções de domínio sem conhecer código de status: a tradução para HTTP é do
 * {@code GlobalExceptionHandler}.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            AddressMapper addressMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
    }

    /**
     * Autocadastro de corretor ou cliente.
     *
     * <p>Rejeita explicitamente {@code ROLE_ADMIN}: o endpoint é público, e conceder papel
     * privilegiado a quem se cadastra sozinho é escalonamento de privilégio.
     */
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        ensureSelfRegistrationDoesNotRequestAdmin(request.roles());

        Email email = new Email(request.email());
        ensureEmailIsAvailable(email.value());
        ensureLoginIsAvailable(request.login());

        User user = userMapper.toEntity(request);
        user.setEmail(email.value());
        user.setPassword(passwordEncoder.encode(request.password()));
        request.roles().forEach(roleName -> user.addRole(resolveRole(roleName)));
        normalizeZipCodes(user);

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Atualiza dados cadastrais. <strong>Não</strong> altera a senha nem os papéis — cada um
     * tem sua própria via.
     */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUserOrThrow(id);

        Email email = new Email(request.email());
        ensureEmailIsAvailableForOthers(email.value(), id);
        ensureLoginIsAvailableForOthers(request.login(), id);

        user.setName(request.name());
        user.setEmail(email.value());
        user.setLogin(request.login());
        user.replaceAddresses(addressMapper.toEntities(request.addresses()));
        normalizeZipCodes(user);

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Troca a própria senha.
     *
     * <p>Exige a senha atual mesmo com o usuário autenticado: um token vazado não deve bastar
     * para tomar a conta.
     */
    @Transactional
    public void changePassword(Long id, PasswordChangeRequest request) {
        User user = getUserOrThrow(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw InvalidPasswordException.currentPasswordDoesNotMatch();
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw InvalidPasswordException.confirmationDoesNotMatch();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** Remove o usuário e, por cascade, seus endereços, tokens e vínculos de papel. */
    @Transactional
    public void delete(Long id) {
        userRepository.delete(getUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(getUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findByName(String name, Pageable pageable) {
        return userRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(userMapper::toResponse);
    }

    private void ensureSelfRegistrationDoesNotRequestAdmin(Set<RoleName> roles) {
        if (roles.contains(RoleName.ROLE_ADMIN)) {
            throw ForbiddenOperationException.selfRegistrationCannotGrantAdmin();
        }
    }

    /**
     * A checagem recebe o e-mail <strong>já normalizado</strong> pelo VO {@link Email}. Com o
     * valor cru, {@code Joao@x.com} e {@code joao@x.com} passariam como distintos e a regra
     * de unicidade estaria quebrada.
     */
    private void ensureEmailIsAvailable(String normalizedEmail) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw DuplicateResourceException.email(normalizedEmail);
        }
    }

    private void ensureLoginIsAvailable(String login) {
        if (userRepository.existsByLogin(login)) {
            throw DuplicateResourceException.login(login);
        }
    }

    private void ensureEmailIsAvailableForOthers(String normalizedEmail, Long id) {
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw DuplicateResourceException.email(normalizedEmail);
        }
    }

    private void ensureLoginIsAvailableForOthers(String login, Long id) {
        if (userRepository.existsByLoginAndIdNot(login, id)) {
            throw DuplicateResourceException.login(login);
        }
    }

    private Role resolveRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> ResourceNotFoundException.role(name));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.user(id));
    }

    /** Guarda o CEP com 8 dígitos, a mesma forma exigida pela constraint do banco. */
    private void normalizeZipCodes(User user) {
        for (Address address : user.getAddresses()) {
            address.setZipCode(new ZipCode(address.getZipCode()).value());
        }
    }
}
