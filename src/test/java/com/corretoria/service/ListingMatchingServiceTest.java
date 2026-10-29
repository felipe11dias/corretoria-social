package com.corretoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.corretoria.entity.Listing;
import com.corretoria.entity.SavedSearch;
import com.corretoria.entity.User;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.NotificationType;
import com.corretoria.enums.OperationType;
import com.corretoria.event.ListingPublishedEvent;
import com.corretoria.repository.ListingRepository;
import com.corretoria.repository.SavedSearchRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListingMatchingServiceTest {

    private static final Long LISTING_ID = 10L;

    @Mock
    private SavedSearchRepository savedSearchRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ListingMatchingService listingMatchingService;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private static Listing aListing() {
        Listing listing = new Listing();
        listing.setId(LISTING_ID);
        listing.setType(ListingType.PROPERTY);
        listing.setOperationType(OperationType.SALE);
        listing.setTitle("Apartamento na Aldeota");
        listing.setPrice(new BigDecimal("450000.00"));
        listing.setNeighborhood("Aldeota");
        listing.setCity("Fortaleza");
        listing.setState("CE");
        return listing;
    }

    private static SavedSearch aSavedSearch(Long customerId, String title) {
        User customer = new User();
        customer.setId(customerId);

        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setId(customerId);
        savedSearch.setTitle(title);
        savedSearch.setCustomer(customer);
        return savedSearch;
    }

    @Nested
    @DisplayName("onListingPublished")
    class OnListingPublished {

        @Test
        @DisplayName("onListingPublished - buscas compatíveis - notifica cada dono uma vez")
        void shouldNotifyEveryMatchingCustomer() {
            Listing listing = aListing();
            SavedSearch first = aSavedSearch(1L, "Apartamento na Aldeota");
            SavedSearch second = aSavedSearch(2L, "Qualquer imóvel em Fortaleza");

            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(savedSearchRepository.findMatching(LISTING_ID))
                    .willReturn(List.of(first, second));

            listingMatchingService.onListingPublished(new ListingPublishedEvent(LISTING_ID));

            then(notificationService).should()
                    .notify(eq(first.getCustomer()), eq(NotificationType.SEARCH_MATCH), any());
            then(notificationService).should()
                    .notify(eq(second.getCustomer()), eq(NotificationType.SEARCH_MATCH), any());
        }

        @Test
        @DisplayName("onListingPublished - mensagem cita a busca e os dados do anúncio")
        void shouldDescribeSearchAndListingInMessage() {
            // A mensagem é gravada pronta: o histórico continua legível mesmo que o anúncio
            // mude de preço ou saia do ar depois.
            Listing listing = aListing();
            SavedSearch savedSearch = aSavedSearch(1L, "Apartamento na Aldeota");

            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(listing));
            given(savedSearchRepository.findMatching(LISTING_ID))
                    .willReturn(List.of(savedSearch));

            listingMatchingService.onListingPublished(new ListingPublishedEvent(LISTING_ID));

            then(notificationService).should().notify(any(), any(), messageCaptor.capture());
            assertThat(messageCaptor.getValue())
                    .contains("Apartamento na Aldeota")
                    .contains("450000.00")
                    .contains("Fortaleza")
                    .contains("/api/v1/listings/10");
        }

        @Test
        @DisplayName("onListingPublished - nenhuma busca compatível - não notifica ninguém")
        void shouldNotNotifyWhenThereIsNoMatch() {
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.of(aListing()));
            given(savedSearchRepository.findMatching(LISTING_ID)).willReturn(List.of());

            listingMatchingService.onListingPublished(new ListingPublishedEvent(LISTING_ID));

            then(notificationService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("onListingPublished - anúncio já não existe - encerra sem consultar buscas")
        void shouldStopWhenListingIsGone() {
            // O evento carrega id, não objeto: entre o commit e o processamento assíncrono o
            // anúncio pode ter sido removido.
            given(listingRepository.findById(LISTING_ID)).willReturn(Optional.empty());

            listingMatchingService.onListingPublished(new ListingPublishedEvent(LISTING_ID));

            then(savedSearchRepository).should(never()).findMatching(any());
            then(notificationService).shouldHaveNoInteractions();
        }
    }
}
