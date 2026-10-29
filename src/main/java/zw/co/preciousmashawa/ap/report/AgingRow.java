package zw.co.preciousmashawa.ap.report;

import java.math.BigDecimal;

/** One supplier's outstanding balance split into aging buckets (days past due date). */
public record AgingRow(
        String supplierCode,
        String supplierName,
        BigDecimal current,
        BigDecimal days1To30,
        BigDecimal days31To60,
        BigDecimal days61To90,
        BigDecimal over90,
        BigDecimal total) {
}
