package zw.co.preciousmashawa.ap.report;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.preciousmashawa.ap.domain.Invoice;
import zw.co.preciousmashawa.ap.domain.InvoiceStatus;
import zw.co.preciousmashawa.ap.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Calculates the aging report in Java. Used when PostgreSQL is not active (for example with H2). */
@Service
@Profile("!postgres")
public class JpaAgingReportService implements AgingReportService {

    private final InvoiceRepository invoices;

    public JpaAgingReportService(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgingRow> agingAsOf(LocalDate asOf) {
        List<Invoice> outstanding = invoices.findByStatusInWithSupplier(
                List.of(InvoiceStatus.APPROVED, InvoiceStatus.PARTIALLY_PAID));

        Map<String, BigDecimal[]> buckets = new TreeMap<>();
        Map<String, String> names = new TreeMap<>();
        for (Invoice invoice : outstanding) {
            String code = invoice.getSupplier().getCode();
            names.put(code, invoice.getSupplier().getName());
            BigDecimal[] row = buckets.computeIfAbsent(code, k -> zeros());
            long daysPastDue = ChronoUnit.DAYS.between(invoice.getDueDate(), asOf);
            int bucket = bucketFor(daysPastDue);
            row[bucket] = row[bucket].add(invoice.getOutstanding());
            row[5] = row[5].add(invoice.getOutstanding());
        }

        List<AgingRow> report = new ArrayList<>();
        buckets.forEach((code, row) -> report.add(
                new AgingRow(code, names.get(code), row[0], row[1], row[2], row[3], row[4], row[5])));
        return report;
    }

    /** 0 = not yet due, 1 = 1-30 days, 2 = 31-60, 3 = 61-90, 4 = over 90. */
    static int bucketFor(long daysPastDue) {
        if (daysPastDue <= 0) {
            return 0;
        }
        if (daysPastDue <= 30) {
            return 1;
        }
        if (daysPastDue <= 60) {
            return 2;
        }
        if (daysPastDue <= 90) {
            return 3;
        }
        return 4;
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] row = new BigDecimal[6];
        java.util.Arrays.fill(row, BigDecimal.ZERO);
        return row;
    }
}
