package com.corretoria.vo.v1.response;

import java.time.LocalDateTime;

/** Corretor seguido, com a data em que o vínculo começou. */
public record RealtorFollowResponse(
        Long id,
        RealtorSummaryResponse realtor,
        LocalDateTime createdAt) {
}
