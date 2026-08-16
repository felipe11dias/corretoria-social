package com.corretoria.repository;

import com.corretoria.entity.Listing;
import com.corretoria.enums.ListingStatus;
import com.corretoria.enums.ListingType;
import com.corretoria.enums.OperationType;
import com.corretoria.enums.PropertyType;
import com.corretoria.enums.VehicleType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso ao agregado raiz {@link Listing}.
 *
 * <p>{@code Property}, {@code Vehicle} e {@code ListingPhoto} não têm repository próprio:
 * pertencem a este agregado e são gerenciados por cascade, mesmo padrão de {@code Address}.
 */
public interface ListingRepository extends JpaRepository<Listing, Long> {

    /**
     * Id do corretor dono do anúncio.
     *
     * <p>Projeção em vez de carregar o agregado: a checagem de posse roda a cada requisição
     * de escrita, fora de transação (o {@code open-in-view} está desligado), e navegar
     * {@code listing.getRealtor().getId()} traria um proxy já desanexado.
     */
    @Query("SELECT l.realtor.id FROM Listing l WHERE l.id = :id")
    Optional<Long> findRealtorIdById(@Param("id") Long id);

    /**
     * Busca do catálogo: filtros comuns, filtros do tipo e recorte por raio.
     *
     * <p>Todo critério é opcional — {@code null} significa "qualquer valor" —, o que permite
     * atender com uma única consulta desde a listagem sem filtro nenhum até a busca mais
     * específica. O alternativo seria uma explosão de <em>query methods</em> derivados, um por
     * combinação.
     *
     * <p>A distância usa a fórmula de Haversine: {@code 6371} é o raio médio da Terra em km e
     * {@code 0.017453292519943295} é π/180, a conversão de grau para radiano exigida pelas
     * funções trigonométricas. O {@code least(...)} protege o {@code acos} de estourar o
     * domínio {@code [-1, 1]} por erro de arredondamento em ponto flutuante — sem ele, buscar
     * pela coordenada exata de um anúncio devolveria {@code NaN}.
     *
     * <p>O recorte prévio por faixa de latitude não é redundante: é ele que usa o índice
     * {@code idx_listings_latitude_longitude}. Sem esse recorte, o Haversine seria calculado
     * linha a linha sobre a tabela inteira. Um grau de latitude equivale a ~111 km em qualquer
     * ponto do globo, o que torna a faixa um envelope seguro (nunca menor que o raio pedido).
     *
     * <p>A lista de parâmetros é longa por natureza: é a assinatura de uma consulta, não de uma
     * regra de negócio. Quem chama monta os critérios a partir de um único VO
     * ({@code ListingSearchRequest}) — o repository não conhece VO.
     */
    @Query("""
            SELECT l FROM Listing l
            LEFT JOIN l.property p
            LEFT JOIN l.vehicle v
            WHERE l.status = :status
              AND (:type IS NULL OR l.type = :type)
              AND (:operationType IS NULL OR l.operationType = :operationType)
              AND (:realtorId IS NULL OR l.realtor.id = :realtorId)
              AND (:city IS NULL OR lower(l.city) = lower(:city))
              AND (:state IS NULL OR lower(l.state) = lower(:state))
              AND (:neighborhood IS NULL OR lower(l.neighborhood) = lower(:neighborhood))
              AND (:minPrice IS NULL OR l.price >= :minPrice)
              AND (:maxPrice IS NULL OR l.price <= :maxPrice)
              AND (:propertyType IS NULL OR p.propertyType = :propertyType)
              AND (:minBedrooms IS NULL OR p.bedrooms >= :minBedrooms)
              AND (:minTotalArea IS NULL OR p.totalArea >= :minTotalArea)
              AND (:vehicleType IS NULL OR v.vehicleType = :vehicleType)
              AND (:brand IS NULL OR lower(v.brand) = lower(:brand))
              AND (:maxMileageKm IS NULL OR v.mileageKm <= :maxMileageKm)
              AND (:minManufactureYear IS NULL OR v.manufactureYear >= :minManufactureYear)
              AND (:radiusKm IS NULL OR (
                    l.latitude BETWEEN :latitude - (:radiusKm / 111.0)
                                   AND :latitude + (:radiusKm / 111.0)
                    AND 6371 * acos(least(1.0,
                            cos(:latitude * 0.017453292519943295)
                          * cos(l.latitude * 0.017453292519943295)
                          * cos((l.longitude - :longitude) * 0.017453292519943295)
                          + sin(:latitude * 0.017453292519943295)
                          * sin(l.latitude * 0.017453292519943295))) <= :radiusKm))
            """)
    Page<Listing> search(
            @Param("status") ListingStatus status,
            @Param("type") ListingType type,
            @Param("operationType") OperationType operationType,
            @Param("realtorId") Long realtorId,
            @Param("city") String city,
            @Param("state") String state,
            @Param("neighborhood") String neighborhood,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("propertyType") PropertyType propertyType,
            @Param("minBedrooms") Integer minBedrooms,
            @Param("minTotalArea") BigDecimal minTotalArea,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("brand") String brand,
            @Param("maxMileageKm") Integer maxMileageKm,
            @Param("minManufactureYear") Integer minManufactureYear,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusKm") Integer radiusKm,
            Pageable pageable);
}
