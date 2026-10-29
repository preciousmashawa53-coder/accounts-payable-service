package zw.co.preciousmashawa.ap.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.co.preciousmashawa.ap.report.AgingReportService;
import zw.co.preciousmashawa.ap.report.AgingRow;
import zw.co.preciousmashawa.ap.service.AccountsPayableService;
import zw.co.preciousmashawa.ap.web.Dtos.AuditResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final AgingReportService aging;
    private final AccountsPayableService service;

    public ReportController(AgingReportService aging, AccountsPayableService service) {
        this.aging = aging;
        this.service = service;
    }

    /** Accounts payable aging. Defaults to today when no date is given. */
    @GetMapping("/reports/aging")
    public List<AgingRow> aging(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return aging.agingAsOf(asOf == null ? LocalDate.now() : asOf);
    }

    @GetMapping("/audit")
    public List<AuditResponse> audit() {
        return service.recentAudit();
    }
}
