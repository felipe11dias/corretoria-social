package com.corretoria.service;

import com.corretoria.entity.Listing;
import com.corretoria.entity.SavedSearch;
import com.corretoria.enums.NotificationType;
import com.corretoria.event.ListingPublishedEvent;
import com.corretoria.repository.ListingRepository;
import com.corretoria.repository.SavedSearchRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Casamento entre um anúncio recém-publicado e as buscas salvas ativas.
 *
 * <p>Vive em serviço próprio, e não dentro do {@code ListingService}: publicar um anúncio e
 * avisar quem procurava mudam por razões diferentes, e o corretor não deve esperar pelo
 * segundo para receber a resposta do primeiro.
 *
 * <p>Reage <strong>depois do commit</strong> ({@code AFTER_COMMIT}): antes disso o anúncio
 * ainda pode não existir para outra transação, e notificar sobre uma publicação que acabou
 * revertida seria pior que não notificar.
 */
@Service
public class ListingMatchingService {

    private static final Logger log = LoggerFactory.getLogger(ListingMatchingService.class);

    private final SavedSearchRepository savedSearchRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    public ListingMatchingService(
            SavedSearchRepository savedSearchRepository,
            ListingRepository listingRepository,
            NotificationService notificationService) {
        this.savedSearchRepository = savedSearchRepository;
        this.listingRepository = listingRepository;
        this.notificationService = notificationService;
    }

    /**
     * Notifica os donos das buscas salvas compatíveis com o anúncio publicado.
     *
     * <p>Roda em outra thread ({@code @Async}) e em transação própria
     * ({@code REQUIRES_NEW}): a transação que publicou o anúncio já terminou quando este método
     * começa, então não há o que herdar.
     *
     * <p>O anúncio é relido pelo id — o evento carrega identificador, não objeto. Se ele já não
     * existir (excluído entre o commit e o processamento), não há o que casar.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onListingPublished(ListingPublishedEvent event) {
        listingRepository.findById(event.listingId())
                .ifPresent(this::notifyMatchingSearches);
    }

    private void notifyMatchingSearches(Listing listing) {
        List<SavedSearch> matches = savedSearchRepository.findMatching(listing.getId());
        log.debug("Anúncio {} casou com {} busca(s) salva(s)", listing.getId(), matches.size());

        for (SavedSearch search : matches) {
            notificationService.notify(
                    search.getCustomer(),
                    NotificationType.SEARCH_MATCH,
                    buildMessage(search, listing));
        }
    }

    private String buildMessage(SavedSearch search, Listing listing) {
        return """
                Um anúncio novo casou com a sua busca "%s": %s — R$ %s, em %s, %s/%s.
                Veja em /api/v1/listings/%d."""
                .formatted(
                        search.getTitle(),
                        listing.getTitle(),
                        listing.getPrice().toPlainString(),
                        listing.getNeighborhood(),
                        listing.getCity(),
                        listing.getState(),
                        listing.getId());
    }
}
