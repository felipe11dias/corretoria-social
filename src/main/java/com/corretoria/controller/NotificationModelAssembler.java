package com.corretoria.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.corretoria.vo.v1.response.NotificationResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Enriquece a notificação com os links de navegação.
 *
 * <p>O link {@code read} é a única ação possível sobre uma notificação — e some quando ela já
 * foi lida, porque um link para o que não faz mais efeito é ruído para o cliente da API.
 */
@Component
public class NotificationModelAssembler
        implements RepresentationModelAssembler<NotificationResponse,
        EntityModel<NotificationResponse>> {

    private static final String NOTIFICATIONS_RELATION = "notifications";
    private static final String READ_RELATION = "read";

    @Override
    @NonNull
    public EntityModel<NotificationResponse> toModel(@NonNull NotificationResponse notification) {
        EntityModel<NotificationResponse> model = EntityModel.of(notification,
                linkTo(NotificationController.class).withRel(NOTIFICATIONS_RELATION));

        if (!notification.read()) {
            model.add(linkTo(methodOn(NotificationController.class)
                    .markAsRead(notification.id())).withRel(READ_RELATION));
        }
        return model;
    }
}
