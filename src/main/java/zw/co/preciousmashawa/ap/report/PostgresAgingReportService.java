package zw.co.preciousmashawa.ap.report;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/** Calls the ap_aging_report stored function (see db/postgres/V100__ap_aging_report_function.sql). */
@Service
@Profile("postgres")
public class PostgresAgingReportService implements AgingReportService {

    private final JdbcTemplate jdbc;

    public PostgresAgingReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AgingRow> agingAsOf(LocalDate asOf) {
        return jdbc.query("SELECT * FROM ap_aging_report(?)",
                (rs, rowNum) -> new AgingRow(
                        rs.getString("supplier_code"),
                        rs.getString("supplier_name"),
                        rs.getBigDecimal("current_due"),
                        rs.getBigDecimal("days_1_to_30"),
                        rs.getBigDecimal("days_31_to_60"),
                        rs.getBigDecimal("days_61_to_90"),
                        rs.getBigDecimal("days_over_90"),
                        rs.getBigDecimal("total_outstanding")),
                Date.valueOf(asOf));
    }
}
