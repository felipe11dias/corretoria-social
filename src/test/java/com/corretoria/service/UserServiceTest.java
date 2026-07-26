package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import com.corretoria.vo.v1.request.AddressRequest;
import com.corretoria.vo.v1.request.PasswordChangeRequest;
import com.corretoria.vo.v1.request.UserRegistrationRequest;
import com.corretoria.vo.v1.request.UserUpdateRequest;
import com.corretoria.vo.v1.response.UserResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String NAME = "João Silva";
    private static final String EMAIL = "joao.silva@email.com";
    private static final String LOGIN = "joao.silva";
    private static final String RAW_PASSWORD = "senhaSegura123";
    private static final String HASHED_PASSWORD = "$2a$10$hash-de-mentira-para-teste";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    // --- fixtures ---

    private static UserRegistrationRequest aRegistrationWith(String email, RoleName... roles) {
        return new UserRegistrationRequest(
                NAME, email, LOGIN, RAW_PASSWORD, Set.of(roles), List.of());
    }

    private static UserRegistrationRequest aRealtorRegistration() {
        return aRegistrationWith(EMAIL, RoleName.ROLE_REALTOR);
    }

    private static User anEmptyUser() {
        User user = new User();
        user.setId(USER_ID);
        return user;
    }

    private static Role aRole(RoleName name) {
        Role role = new Role(name);
        role.setId(1L);
        return role;
    }

    private static UserResponse aResponse() {
        return new UserResponse(USER_ID, NAME, EMAIL, LOGIN, Set.of(), List.of(), null, null);
    }

    private void givenMappingSucceeds(User mapped) {
        given(userMapper.toEntity(any())).willReturn(mapped);
        given(userRepository.save(any())).willAnswer(call -> call.getArgument(0));
        given(userMapper.toResponse(any())).willReturn(aResponse());
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("register - cadastro válido - grava o hash e nunca a senha em texto plano")
        void shouldHashPassword() {
            // arrange
            givenMappingSucceeds(anEmptyUser());
            given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
            given(roleRepository.findByName(RoleName.ROLE_REALTOR))
                    .willReturn(Optional.of(aRole(RoleName.ROLE_REALTOR)));

            // act
            userService.register(aRealtorRegistration());

            // assert
            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword())
                    .isEqualTo(HASHED_PASSWORD)
                    .isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        @DisplayName("register - corretor - recebe ROLE_REALTOR")
        void shouldAssignRealtorRole() {
            givenMappingSucceeds(anEmptyUser());
            given(roleRepository.findByName(RoleName.ROLE_REALTOR))
                    .willReturn(Optional.of(aRole(RoleName.ROLE_REALTOR)));

            userService.register(aRealtorRegistration());

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRoles())
                    .extracting(Role::getName)
                    .containsExactly(RoleName.ROLE_REALTOR);
        }

        @Test
        @DisplayName("register - cliente - recebe ROLE_CUSTOMER")
        void shouldAssignCustomerRole() {
            givenMappingSucceeds(anEmptyUser());
            given(roleRepository.findByName(RoleName.ROLE_CUSTOMER))
                    .willReturn(Optional.of(aRole(RoleName.ROLE_CUSTOMER)));

            userService.register(aRegistrationWith(EMAIL, RoleName.ROLE_CUSTOMER));

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRoles())
                    .extracting(Role::getName)
                    .containsExactly(RoleName.ROLE_CUSTOMER);
        }

        @Test
        @DisplayName("register - solicitando ROLE_ADMIN - lança ForbiddenOperationException")
        void shouldRejectRegistrationWhenRoleAdminIsRequested() {
            // O autocadastro é público: conceder papel privilegiado aqui seria escalonamento
            // de privilégio. Nada pode ser gravado nem consultado antes da recusa.
            assertThatThrownBy(() ->
                    userService.register(aRegistrationWith(EMAIL, RoleName.ROLE_ADMIN)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("ROLE_ADMIN");

            then(userRepository).shouldHaveNoInteractions();
            then(passwordEncoder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("register - e-mail já cadastrado - lança DuplicateResourceException")
        void shouldRejectDuplicateEmail() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            assertThatThrownBy(() -> userService.register(aRealtorRegistration()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(EMAIL);

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("register - login já cadastrado - lança DuplicateResourceException")
        void shouldRejectDuplicateLogin() {
            given(userRepository.existsByLogin(LOGIN)).willReturn(true);

            assertThatThrownBy(() -> userService.register(aRealtorRegistration()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(LOGIN);

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("register - e-mail em maiúsculas - checa unicidade já normalizado")
        void shouldNormalizeEmailBeforeUniquenessCheck() {
            // Sem a normalização, "JOAO.Silva@Email.COM" não colidiria com o registro
            // existente "joao.silva@email.com" e a regra de e-mail único estaria quebrada.
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            assertThatThrownBy(() -> userService.register(
                    aRegistrationWith("JOAO.Silva@Email.COM", RoleName.ROLE_REALTOR)))
                    .isInstanceOf(DuplicateResourceException.class);

            then(userRepository).should().existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("register - e-mail normalizado - é o valor gravado na entidade")
        void shouldPersistNormalizedEmail() {
            givenMappingSucceeds(anEmptyUser());
            given(roleRepository.findByName(RoleName.ROLE_REALTOR))
                    .willReturn(Optional.of(aRole(RoleName.ROLE_REALTOR)));

            userService.register(
                    aRegistrationWith("  JOAO.Silva@Email.COM ", RoleName.ROLE_REALTOR));

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("register - papel inexistente no banco - lança ResourceNotFoundException")
        void shouldFailWhenRoleIsNotSeeded() {
            givenMappingSucceedsWithoutSave(anEmptyUser());
            given(roleRepository.findByName(RoleName.ROLE_REALTOR)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.register(aRealtorRegistration()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ROLE_REALTOR");
        }

        @Test
        @DisplayName("register - CEP com máscara - grava apenas os 8 dígitos")
        void shouldNormalizeZipCode() {
            User mapped = anEmptyUser();
            mapped.addAddress(anAddressWithZipCode("60160-230"));
            givenMappingSucceeds(mapped);
            given(roleRepository.findByName(RoleName.ROLE_REALTOR))
                    .willReturn(Optional.of(aRole(RoleName.ROLE_REALTOR)));

            userService.register(aRealtorRegistration());

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getAddresses())
                    .extracting(Address::getZipCode)
                    .containsExactly("60160230");
        }

        private void givenMappingSucceedsWithoutSave(User mapped) {
            given(userMapper.toEntity(any())).willReturn(mapped);
        }

        private Address anAddressWithZipCode(String zipCode) {
            Address address = new Address();
            address.setZipCode(zipCode);
            return address;
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("update - dados válidos - atualiza e não toca na senha")
        void shouldNotChangePasswordOnUpdate() {
            User existing = anEmptyUser();
            existing.setPassword(HASHED_PASSWORD);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));
            given(addressMapper.toEntities(any())).willReturn(new ArrayList<>());
            given(userRepository.save(any())).willAnswer(call -> call.getArgument(0));
            given(userMapper.toResponse(any())).willReturn(aResponse());

            userService.update(USER_ID, new UserUpdateRequest(NAME, List.of()));

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo(HASHED_PASSWORD);
            then(passwordEncoder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("update - e-mail e login são imutáveis - permanecem os do cadastro")
        void shouldNotChangeEmailNorLogin() {
            // A atualização mexe apenas em nome e endereços. Como e-mail e login não podem
            // mudar, a unicidade não precisa ser revalidada aqui — e o repositório não é
            // consultado para isso.
            User existing = anEmptyUser();
            existing.setEmail(EMAIL);
            existing.setLogin(LOGIN);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));
            given(addressMapper.toEntities(any())).willReturn(new ArrayList<>());
            given(userRepository.save(any())).willAnswer(call -> call.getArgument(0));
            given(userMapper.toResponse(any())).willReturn(aResponse());

            userService.update(USER_ID, new UserUpdateRequest("Outro Nome", List.of()));

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo(EMAIL);
            assertThat(userCaptor.getValue().getLogin()).isEqualTo(LOGIN);
            assertThat(userCaptor.getValue().getName()).isEqualTo("Outro Nome");
        }

        @Test
        @DisplayName("update - id inexistente - lança ResourceNotFoundException")
        void shouldFailForUnknownId() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.update(USER_ID, new UserUpdateRequest(NAME, List.of())))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("update - endereços novos - substitui a lista do agregado")
        void shouldReplaceAddresses() {
            User existing = anEmptyUser();
            Address old = new Address();
            old.setZipCode("60000000");
            existing.addAddress(old);

            Address incoming = new Address();
            incoming.setZipCode("60160-230");

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));
            given(addressMapper.toEntities(any()))
                    .willReturn(new ArrayList<>(List.of(incoming)));
            given(userRepository.save(any())).willAnswer(call -> call.getArgument(0));
            given(userMapper.toResponse(any())).willReturn(aResponse());

            userService.update(USER_ID, new UserUpdateRequest(NAME,
                    List.of(new AddressRequest("Rua", "1", null, null, "Fortaleza", "CE",
                            "60160-230"))));

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getAddresses())
                    .extracting(Address::getZipCode)
                    .containsExactly("60160230");
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        private final PasswordChangeRequest validRequest =
                new PasswordChangeRequest(RAW_PASSWORD, "novaSenha456", "novaSenha456");

        @Test
        @DisplayName("changePassword - senha atual correta - grava o novo hash")
        void shouldChangePassword() {
            User existing = anEmptyUser();
            existing.setPassword(HASHED_PASSWORD);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));
            given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(true);
            given(passwordEncoder.encode("novaSenha456")).willReturn("$2a$10$novo-hash");

            userService.changePassword(USER_ID, validRequest);

            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$novo-hash");
        }

        @Test
        @DisplayName("changePassword - senha atual incorreta - lança InvalidPasswordException")
        void shouldRejectWrongCurrentPassword() {
            User existing = anEmptyUser();
            existing.setPassword(HASHED_PASSWORD);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword(USER_ID, validRequest))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("senha atual");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("changePassword - confirmação divergente - lança InvalidPasswordException")
        void shouldRejectMismatchedConfirmation() {
            User existing = anEmptyUser();
            existing.setPassword(HASHED_PASSWORD);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

            assertThatThrownBy(() -> userService.changePassword(USER_ID,
                    new PasswordChangeRequest(RAW_PASSWORD, "novaSenha456", "outraCoisa789")))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("não coincidem");

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("consultas e exclusao")
    class ConsultasEExclusao {

        @Test
        @DisplayName("findById - id existente - devolve o VO de saída")
        void shouldFindById() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(anEmptyUser()));
            given(userMapper.toResponse(any())).willReturn(aResponse());

            assertThat(userService.findById(USER_ID).id()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("findById - id inexistente - lança ResourceNotFoundException")
        void shouldFailToFindUnknownId() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(USER_ID));
        }

        @Test
        @DisplayName("delete - id inexistente - lança ResourceNotFoundException")
        void shouldFailToDeleteUnknownId() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(userRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("delete - id existente - remove o usuário")
        void shouldDelete() {
            User existing = anEmptyUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(existing));

            userService.delete(USER_ID);

            then(userRepository).should().delete(existing);
        }

        @Test
        @DisplayName("findByName - busca parcial - delega ao query method sem diferenciar caixa")
        void shouldSearchByNameIgnoringCase() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> page = new PageImpl<>(List.of(anEmptyUser()));
            given(userRepository.findByNameContainingIgnoreCase("jo", pageable))
                    .willReturn(page);
            given(userMapper.toResponse(any())).willReturn(aResponse());

            assertThat(userService.findByName("jo", pageable)).hasSize(1);
            then(userRepository).should().findByNameContainingIgnoreCase("jo", pageable);
        }

        @Test
        @DisplayName("findAll - listagem paginada - converte cada item em VO")
        void shouldListPaged() {
            Pageable pageable = PageRequest.of(0, 20);
            given(userRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(anEmptyUser(), anEmptyUser())));
            given(userMapper.toResponse(any())).willReturn(aResponse());

            assertThat(userService.findAll(pageable)).hasSize(2);
        }
    }
}
