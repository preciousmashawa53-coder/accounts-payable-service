package zw.co.preciousmashawa.ap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.preciousmashawa.ap.domain.Payment;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
