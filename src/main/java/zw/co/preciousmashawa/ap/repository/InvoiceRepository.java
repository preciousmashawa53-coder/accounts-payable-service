package zw.co.preciousmashawa.ap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zw.co.preciousmashawa.ap.domain.Invoice;
import zw.co.preciousmashawa.ap.domain.InvoiceStatus;

import java.util.Collection;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsBySupplierIdAndInvoiceNumber(Long supplierId, String invoiceNumber);

    @Query("select i from Invoice i join fetch i.supplier order by i.id desc")
    List<Invoice> findAllWithSupplier();

    @Query("select i from Invoice i join fetch i.supplier where i.status = :status order by i.id desc")
    List<Invoice> findByStatusWithSupplier(@Param("status") InvoiceStatus status);

    @Query("select i from Invoice i join fetch i.supplier where i.status in :statuses")
    List<Invoice> findByStatusInWithSupplier(@Param("statuses") Collection<InvoiceStatus> statuses);
}
