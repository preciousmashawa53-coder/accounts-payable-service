package zw.co.preciousmashawa.ap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.preciousmashawa.ap.domain.AuditEvent;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findTop100ByOrderByIdDesc();

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByIdAsc(String entityType, Long entityId);
}
