-- Stored function: accounts payable aging report as at a given date.
-- Groups outstanding approved invoices by supplier into standard aging buckets.
-- Used by PostgresAgingReportService when the "postgres" profile is active.

CREATE OR REPLACE FUNCTION ap_aging_report(as_of DATE)
RETURNS TABLE (
    supplier_code     VARCHAR,
    supplier_name     VARCHAR,
    current_due       NUMERIC,
    days_1_to_30      NUMERIC,
    days_31_to_60     NUMERIC,
    days_61_to_90     NUMERIC,
    days_over_90      NUMERIC,
    total_outstanding NUMERIC
)
LANGUAGE sql
STABLE
AS $$
    SELECT s.code,
           s.name,
           COALESCE(SUM(i.amount - i.amount_paid) FILTER (WHERE as_of - i.due_date <= 0), 0),
           COALESCE(SUM(i.amount - i.amount_paid) FILTER (WHERE as_of - i.due_date BETWEEN 1 AND 30), 0),
           COALESCE(SUM(i.amount - i.amount_paid) FILTER (WHERE as_of - i.due_date BETWEEN 31 AND 60), 0),
           COALESCE(SUM(i.amount - i.amount_paid) FILTER (WHERE as_of - i.due_date BETWEEN 61 AND 90), 0),
           COALESCE(SUM(i.amount - i.amount_paid) FILTER (WHERE as_of - i.due_date > 90), 0),
           COALESCE(SUM(i.amount - i.amount_paid), 0)
    FROM invoices i
    JOIN suppliers s ON s.id = i.supplier_id
    WHERE i.status IN ('APPROVED', 'PARTIALLY_PAID')
    GROUP BY s.code, s.name
    ORDER BY s.code;
$$;
