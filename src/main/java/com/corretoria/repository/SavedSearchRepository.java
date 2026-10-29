package com.corretoria.repository;

import com.corretoria.entity.SavedSearch;
import com.corretoria.enums.SearchVisibility;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso ao agregado raiz {@link SavedSearch}.
 *
 * <p>{@code PropertySearchCriteria} e {@code VehicleSearchCriteria} não têm repository próprio:
 * pertencem a este agregado e são gerenciados por cascade.
 */
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {

    Page<SavedSearch> findByCustomerId(Long customerId, Pageable pageable);

    /** Vitrine de procura: só busca pública e ativa aparece para os corretores. */
    Page<SavedSearch> findByVisibilityAndActiveTrue(
            SearchVisibility visibility, Pageable pageable);

    /** Ver {@code ListingRepository.findRealtorIdById}: projeção para a checagem de posse. */
    @Query("SELECT s.customer.id FROM SavedSearch s WHERE s.id = :id")
    Optional<Long> findCustomerIdById(@Param("id") Long id);

    /**
     * Buscas salvas ativas que casam com um anúncio recém-publicado.
     *
     * <p>É a consulta espelhada da busca do catálogo: lá o usuário fixa critérios e o banco
     * procura anúncios; aqui o anúncio é fixo e o banco procura critérios que o contenham.
     * Cada condição segue o mesmo formato — critério nulo na busca salva significa "qualquer
     * valor" e por isso casa.
     *
     * <p>O anúncio entra por {@code join ... on}, e não por uma lista de parâmetros com seus
     * quinze atributos: além de encurtar a assinatura, deixa a comparação inteira no banco, com
     * os dois lados como coluna. De quebra, nenhuma comparação de texto recebe parâmetro, o que
     * evita de saída o problema de tipo do nulo dentro de {@code lower} (ver
     * {@code ListingRepository.search}).
     *
     * <p>O corretor dono nunca é notificado da própria publicação — daí a comparação entre
     * {@code customer} e {@code realtor}, que importa porque um mesmo usuário pode acumular os
     * dois papéis.
     */
    @Query("""
            SELECT s FROM SavedSearch s
            LEFT JOIN s.propertyCriteria pc
            LEFT JOIN s.vehicleCriteria vc
            JOIN Listing l ON l.id = :listingId
            LEFT JOIN l.property lp
            LEFT JOIN l.vehicle lv
            WHERE s.active = true
              AND s.type = l.type
              AND s.customer.id <> l.realtor.id
              AND (s.operationType IS NULL OR s.operationType = l.operationType)
              AND (s.minPrice IS NULL OR l.price >= s.minPrice)
              AND (s.maxPrice IS NULL OR l.price <= s.maxPrice)
              AND (s.city IS NULL OR lower(s.city) = lower(l.city))
              AND (s.state IS NULL OR lower(s.state) = lower(l.state))
              AND (s.neighborhood IS NULL OR lower(s.neighborhood) = lower(l.neighborhood))
              AND (s.radiusKm IS NULL OR
                    6371 * acos(least(1.0,
                            cos(s.latitude * 0.017453292519943295)
                          * cos(l.latitude * 0.017453292519943295)
                          * cos((l.longitude - s.longitude) * 0.017453292519943295)
                          + sin(s.latitude * 0.017453292519943295)
                          * sin(l.latitude * 0.017453292519943295))) <= s.radiusKm)
              AND (pc IS NULL OR (
                    (pc.propertyType IS NULL OR pc.propertyType = lp.propertyType)
                AND (pc.minBedrooms IS NULL OR lp.bedrooms >= pc.minBedrooms)
                AND (pc.minBathrooms IS NULL OR lp.bathrooms >= pc.minBathrooms)
                AND (pc.minTotalArea IS NULL OR lp.totalArea >= pc.minTotalArea)))
              AND (vc IS NULL OR (
                    (vc.vehicleType IS NULL OR vc.vehicleType = lv.vehicleType)
                AND (vc.brand IS NULL OR lower(vc.brand) = lower(lv.brand))
                AND (vc.maxMileageKm IS NULL OR lv.mileageKm <= vc.maxMileageKm)
                AND (vc.minManufactureYear IS NULL
                     OR lv.manufactureYear >= vc.minManufactureYear)))
            """)
    List<SavedSearch> findMatching(@Param("listingId") Long listingId);
}
