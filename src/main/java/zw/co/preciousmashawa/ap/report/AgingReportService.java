package zw.co.preciousmashawa.ap.report;

import java.time.LocalDate;
import java.util.List;

/**
 * Accounts payable aging report. Two implementations:
 * the PostgreSQL one calls the ap_aging_report stored function,
 * the default one calculates the same buckets in Java (used with H2).
 */
public interface AgingReportService {

    List<AgingRow> agingAsOf(LocalDate asOf);
}
