package com.corretoria.service;

import com.corretoria.entity.RealtorPost;
import com.corretoria.entity.RealtorPostComment;
import com.corretoria.entity.RealtorPostLike;
import com.corretoria.entity.User;
import com.corretoria.exception.ResourceNotFoundException;
import com.corretoria.mapper.RealtorPostMapper;
import com.corretoria.repository.RealtorPostLikeRepository;
import com.corretoria.repository.RealtorPostRepository;
import com.corretoria.security.UserSecurity;
import com.corretoria.vo.v1.request.RealtorPostCommentCreationRequest;
import com.corretoria.vo.v1.request.RealtorPostCreationRequest;
import com.corretoria.vo.v1.response.RealtorPostCommentResponse;
import com.corretoria.vo.v1.response.RealtorPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mural do corretor: publicações, comentários e curtidas.
 *
 * <p>Publicar é do corretor dono do mural; comentar e curtir são de <strong>qualquer usuário
 * autenticado</strong>, inclusive de outro corretor — conversa de mural é pública, ao
 * contrário do thread de uma proposta. Favoritar e acompanhar, esses sim, continuam exclusivos
 * do cliente.
 */
@Service
public class RealtorPostService {

    private final RealtorPostRepository realtorPostRepository;
    private final RealtorPostLikeRepository realtorPostLikeRepository;
    private final RealtorPostMapper realtorPostMapper;
    private final UserSecurity userSecurity;

    public RealtorPostService(
            RealtorPostRepository realtorPostRepository,
            RealtorPostLikeRepository realtorPostLikeRepository,
            RealtorPostMapper realtorPostMapper,
            UserSecurity userSecurity) {
        this.realtorPostRepository = realtorPostRepository;
        this.realtorPostLikeRepository = realtorPostLikeRepository;
        this.realtorPostMapper = realtorPostMapper;
        this.userSecurity = userSecurity;
    }

    @Transactional
    public RealtorPostResponse publish(
            RealtorPostCreationRequest request, Authentication authentication) {

        User realtor = userSecurity.getAuthenticatedUser(authentication);

        RealtorPost post = realtorPostMapper.toEntity(request);
        post.setRealtor(realtor);

        return toResponse(realtorPostRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<RealtorPostResponse> findByRealtor(Long realtorId, Pageable pageable) {
        return realtorPostRepository.findByRealtorId(realtorId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public RealtorPostCommentResponse comment(
            Long postId, RealtorPostCommentCreationRequest request, Authentication authentication) {

        RealtorPost post = getPostOrThrow(postId);
        User author = userSecurity.getAuthenticatedUser(authentication);

        RealtorPostComment comment = realtorPostMapper.toEntity(request);
        comment.setAuthor(author);
        post.addComment(comment);

        // flush, e não save: ver ListingService.addPhoto — a resposta carrega o id gerado.
        realtorPostRepository.flush();

        return realtorPostMapper.toCommentResponse(comment);
    }

    /**
     * Curtir é idempotente: a segunda chamada não vira uma segunda curtida.
     *
     * <p>A restrição {@code UNIQUE} garante o mesmo no banco — é o que impede um contador que
     * sobe a cada clique.
     */
    @Transactional
    public RealtorPostResponse like(Long postId, Authentication authentication) {
        RealtorPost post = getPostOrThrow(postId);
        User user = userSecurity.getAuthenticatedUser(authentication);

        if (realtorPostLikeRepository.findByPostIdAndUserId(postId, user.getId()).isEmpty()) {
            RealtorPostLike like = new RealtorPostLike();
            like.setPost(post);
            like.setUser(user);
            realtorPostLikeRepository.save(like);
        }

        return toResponse(post);
    }

    @Transactional
    public RealtorPostResponse unlike(Long postId, Authentication authentication) {
        RealtorPost post = getPostOrThrow(postId);
        User user = userSecurity.getAuthenticatedUser(authentication);

        realtorPostLikeRepository.findByPostIdAndUserId(postId, user.getId())
                .ifPresent(realtorPostLikeRepository::delete);

        return toResponse(post);
    }

    /**
     * O total de curtidas é contado no banco, e não pelo tamanho de uma coleção mapeada: uma
     * publicação popular carregaria milhares de vínculos só para exibir um número.
     */
    private RealtorPostResponse toResponse(RealtorPost post) {
        return realtorPostMapper.toResponse(
                post, realtorPostLikeRepository.countByPostId(post.getId()));
    }

    private RealtorPost getPostOrThrow(Long postId) {
        return realtorPostRepository.findById(postId)
                .orElseThrow(() -> ResourceNotFoundException.realtorPost(postId));
    }
}
