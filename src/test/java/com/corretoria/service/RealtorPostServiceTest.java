package com.corretoria.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.RealtorPost;
import com.corretoria.entity.RealtorPostComment;
import com.corretoria.entity.RealtorPostLike;
import com.corretoria.entity.User;
import com.corretoria.mapper.RealtorPostMapper;
import com.corretoria.repository.RealtorPostLikeRepository;
import com.corretoria.repository.RealtorPostRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.RealtorPostCommentCreationRequest;
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
class RealtorPostServiceTest {

    private static final Long POST_ID = 50L;
    private static final Long USER_ID = 2L;

    @Mock
    private RealtorPostRepository realtorPostRepository;

    @Mock
    private RealtorPostLikeRepository realtorPostLikeRepository;

    @Mock
    private RealtorPostMapper realtorPostMapper;

    @Mock
    private UserSecurity userSecurity;

    @InjectMocks
    private RealtorPostService realtorPostService;

    private static Authentication anAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                "cliente.demo", null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    private static User aUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Cliente Demonstracao");
        return user;
    }

    private static RealtorPost aPost() {
        RealtorPost post = new RealtorPost();
        post.setId(POST_ID);
        post.setContent("O mercado da Aldeota em agosto");
        return post;
    }

    @Nested
    @DisplayName("like")
    class Like {

        @Test
        @DisplayName("like - primeira curtida do usuário - registra o vínculo")
        void shouldRegisterFirstLike() {
            Authentication authentication = anAuthentication();
            given(realtorPostRepository.findById(POST_ID)).willReturn(Optional.of(aPost()));
            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(aUser());
            given(realtorPostLikeRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                    .willReturn(Optional.empty());

            realtorPostService.like(POST_ID, authentication);

            then(realtorPostLikeRepository).should().save(any(RealtorPostLike.class));
        }

        @Test
        @DisplayName("like - curtida repetida - não vira uma segunda curtida")
        void shouldBeIdempotent() {
            // O que a interface promete: uma curtida por pessoa, e não um contador que sobe a
            // cada clique. O UNIQUE garante o mesmo no banco.
            Authentication authentication = anAuthentication();
            given(realtorPostRepository.findById(POST_ID)).willReturn(Optional.of(aPost()));
            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(aUser());
            given(realtorPostLikeRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                    .willReturn(Optional.of(new RealtorPostLike()));

            realtorPostService.like(POST_ID, authentication);

            then(realtorPostLikeRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("unlike - curtida inexistente - não é erro")
        void shouldTolerateUnlikeWithoutLike() {
            Authentication authentication = anAuthentication();
            given(realtorPostRepository.findById(POST_ID)).willReturn(Optional.of(aPost()));
            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(aUser());
            given(realtorPostLikeRepository.findByPostIdAndUserId(POST_ID, USER_ID))
                    .willReturn(Optional.empty());

            realtorPostService.unlike(POST_ID, authentication);

            then(realtorPostLikeRepository).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("comment")
    class Comment {

        @Test
        @DisplayName("comment - qualquer autenticado - fecha autor e post, e descarrega o id")
        void shouldAttachCommentToPost() {
            Authentication authentication = anAuthentication();
            RealtorPost post = aPost();
            User author = aUser();
            RealtorPostComment comment = new RealtorPostComment();

            given(realtorPostRepository.findById(POST_ID)).willReturn(Optional.of(post));
            given(userSecurity.getAuthenticatedUser(authentication)).willReturn(author);
            given(realtorPostMapper.toEntity(any(RealtorPostCommentCreationRequest.class)))
                    .willReturn(comment);

            realtorPostService.comment(POST_ID,
                    new RealtorPostCommentCreationRequest("Ótima análise!"), authentication);

            org.assertj.core.api.Assertions.assertThat(post.getComments()).containsExactly(comment);
            org.assertj.core.api.Assertions.assertThat(comment.getAuthor()).isSameAs(author);
            // A resposta carrega o id gerado; sem o flush ele só existiria no commit.
            then(realtorPostRepository).should().flush();
        }
    }
}
