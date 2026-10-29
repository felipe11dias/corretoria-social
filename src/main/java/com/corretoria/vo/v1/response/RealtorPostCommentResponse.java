package com.corretoria.vo.v1.response;

import java.time.LocalDateTime;

/** Comentário em uma publicação do mural. */
public record RealtorPostCommentResponse(
        Long id,
        UserSummaryResponse author,
        String content,
        LocalDateTime createdAt) {
}
