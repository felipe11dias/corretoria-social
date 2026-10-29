package com.corretoria.mapper;

import com.corretoria.entity.Notification;
import com.corretoria.vo.v1.response.NotificationResponse;
import org.mapstruct.Mapper;

/**
 * Tradução do agregado {@link Notification} para o VO de contrato.
 *
 * <p>Só tem sentido de saída: notificação não é criada por requisição de ninguém — nasce de um
 * fato do sistema (anúncio compatível publicado, proposta recebida) e é montada no serviço.
 */
@Mapper
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
