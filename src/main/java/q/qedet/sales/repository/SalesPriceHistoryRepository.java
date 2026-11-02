package q.qedet.sales.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.sales.entity.SalesPriceHistory;

import java.util.List;
import java.util.UUID;

public interface SalesPriceHistoryRepository
        extends JpaRepository<SalesPriceHistory, UUID> {

    List<SalesPriceHistory> findAllBySalesPriceIdOrderByChangedAtDesc(
            UUID salesPriceId
    );
}