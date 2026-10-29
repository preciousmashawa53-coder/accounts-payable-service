# Accounts Payable Service

An ERP-style **accounts payable** back end built with **Java 21 and Spring Boot**. It covers the supplier invoice lifecycle that finance teams run every day: capture, approval, payment and aging, with the controls a bank or auditor expects.

## Features

- **Suppliers and invoices**: capture supplier invoices against purchase order numbers.
- **Approval workflow** with **segregation of duties**: whoever captured an invoice cannot approve it.
- **Payments**: partial and full payments, with overpayment blocked and the balance tracked.
- **Duplicate invoice control**: the same invoice number cannot be captured twice for one supplier.
- **Aging report**: outstanding balances per supplier in Current, 1–30, 31–60, 61–90 and 90+ day buckets.
  On PostgreSQL this runs as a **stored function** (`ap_aging_report`).
- **Audit trail**: every change records who did it, what changed and when.
- **REST API** for all operations and a **GraphQL** endpoint for reporting queries.
- **Role-based security**: Clerk, Approver and Treasury roles (HTTP Basic, stateless).
- **Database migrations** with Flyway; runs on **PostgreSQL** or in-memory **H2**.
- **Automated tests** (JUnit 5, MockMvc) covering the workflow, rules, security, reports and GraphQL.
- **CI/CD**: a **Jenkinsfile** (build, test, package, Docker image, deploy to staging) and a GitHub Actions workflow.

## Tech stack

Java 21 · Spring Boot 3 (Web, Data JPA, Validation, Security, GraphQL) · Hibernate · Flyway · PostgreSQL · H2 · JUnit 5 · Maven · Jenkins · GitHub Actions · Docker

## Project structure

```
src/main/java/zw/co/preciousmashawa/ap/
  domain/       Entities and business rules (Invoice holds the approval and payment rules)
  repository/   Spring Data JPA repositories
  service/      AccountsPayableService: the workflow, transactions and audit trail
  report/       Aging report: Java version and PostgreSQL stored-function version
  web/          REST controllers, request/response records, error handling
  graphql/      GraphQL query controller
  config/       Security roles and demo data
src/main/resources/
  db/migration/ Flyway schema (tables, constraints, indexes)
  db/postgres/  PostgreSQL stored function for the aging report
  graphql/      GraphQL schema
src/test/java/  API and unit tests
Jenkinsfile     Jenkins pipeline
```

## Run it

Requires Java 21 and Maven.

```bash
mvn spring-boot:run          # starts on http://localhost:8080 with H2 and sample data
mvn verify                   # runs all tests
```

With PostgreSQL (uses the stored function):

```bash
docker compose up --build
```

### Demo users (local development only)

| User | Password | Role |
| --- | --- | --- |
| clerk | clerk-pass | Capture suppliers and invoices |
| approver | approver-pass | Approve or reject invoices |
| treasury | treasury-pass | Record payments |
| supervisor | supervisor-pass | Capture and approve (cannot approve own invoices) |

Set `AP_CLERK_PASSWORD`, `AP_APPROVER_PASSWORD`, `AP_TREASURY_PASSWORD` and `AP_SUPERVISOR_PASSWORD` to change them.

## Try the API

```bash
# Capture an invoice
curl -u clerk:clerk-pass -H "Content-Type: application/json" -X POST localhost:8080/api/invoices \
  -d '{"supplierCode":"SUP-001","invoiceNumber":"INV-2001","poNumber":"PO-777","invoiceDate":"2026-10-01","dueDate":"2026-10-31","amount":1500.00}'

# Approve it (use the id returned above)
curl -u approver:approver-pass -X POST localhost:8080/api/invoices/4/approve

# Pay part of it
curl -u treasury:treasury-pass -H "Content-Type: application/json" -X POST localhost:8080/api/invoices/4/payments \
  -d '{"amount":500.00,"paymentDate":"2026-10-28","reference":"EFT-1001"}'

# Aging report and audit trail
curl -u clerk:clerk-pass "localhost:8080/api/reports/aging?asOf=2026-10-31"
curl -u clerk:clerk-pass localhost:8080/api/audit
```

| Method | Path | Role |
| --- | --- | --- |
| GET / POST | `/api/suppliers` | any / CLERK |
| GET / POST | `/api/invoices` (`?status=APPROVED`) | any / CLERK |
| GET | `/api/invoices/{id}` | any |
| POST | `/api/invoices/{id}/approve` · `/reject` | APPROVER |
| GET / POST | `/api/invoices/{id}/payments` | any / TREASURY |
| GET | `/api/reports/aging?asOf=YYYY-MM-DD` | any |
| GET | `/api/audit` | any |
| POST | `/graphql` | any |

## GraphQL

Open `http://localhost:8080/graphiql` (sign in as any user), or post a query:

```graphql
{
  invoices(status: "APPROVED") { invoiceNumber supplierName amount outstanding dueDate }
  agingReport(asOf: "2026-10-31") { supplierCode current days1To30 days31To60 over90 total }
}
```

## Error responses

| Status | When |
| --- | --- |
| 400 | Invalid input (e.g. negative amount, missing field) |
| 401 / 403 | Not signed in / wrong role |
| 404 | Supplier or invoice not found |
| 409 | Duplicate supplier code or invoice number |
| 422 | Business rule broken (self-approval, overpayment, paying an unapproved invoice) |

## Author

Precious Mashawa, Harare, Zimbabwe · [preciousmashawa.co.zw](https://preciousmashawa.co.zw)
