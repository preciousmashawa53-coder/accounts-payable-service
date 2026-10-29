package zw.co.preciousmashawa.ap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.preciousmashawa.ap.domain.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByCode(String code);

    boolean existsByCode(String code);

    List<Supplier> findAllByOrderByCodeAsc();
}
