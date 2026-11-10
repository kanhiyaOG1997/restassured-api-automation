# RestAssured API Automation

![API Tests](https://github.com/kanhiyaOG1997/restassured-api-automation/actions/workflows/api-tests.yml/badge.svg)

**Live test report:** https://kanhiyaog1997.github.io/restassured-api-automation (Allure, updated on every push to `main`)

API test automation framework for the public [Restful-Booker API](https://restful-booker.herokuapp.com), built with **RestAssured**, **TestNG** and **Maven**.

## Tech Stack

| Tool | Purpose |
|---|---|
| Java 21 | Programming language |
| RestAssured 5.5 | Sending HTTP requests and validating responses |
| TestNG 7.10 | Test runner (priority, dependencies, assertions) |
| Jackson | POJO to JSON serialization and deserialization |
| Maven | Build and dependency management |
| Allure 2 | HTML report with every request and response attached |
| GitHub Actions | CI: runs tests on every push |

## Project Structure

```
src/test/java/com/kanhaiya/api
├── base/BaseTest.java        # Common setup: request spec, 15s timeout, auth token
├── listeners/                # RetryAnalyzer + RetryListener (retry failed tests once)
├── utils/ConfigReader.java   # Reads config.properties
├── utils/TestDataBuilder.java # Random valid booking data
├── models/                   # POJOs: Booking, BookingDates
└── tests/
    ├── AuthTest.java         # POST /auth tests
    ├── SmokeTest.java        # API is up, main endpoints answer
    ├── BookingTest.java      # CRUD flow + negative test
    ├── BookingEdgeCaseTest.java # Boundary values, filters, known API quirks
    ├── SanityTest.java       # Each feature works correctly (PATCH, filters, auth)
    ├── SchemaValidationTest.java  # Response structure vs JSON schemas
    ├── BookingDataDrivenTest.java # @DataProvider-driven cases + known API issues
    └── BookingE2ETest.java   # Full user journeys across endpoints
src/test/resources/config.properties   # Base URL and credentials
src/test/resources/schemas/            # JSON schema files
testng.xml                              # Test suite
.github/workflows/api-tests.yml         # CI pipeline
```

## How to Run

```bash
mvn clean test                    # all tests
mvn clean test -Dgroups=smoke     # only smoke (also: sanity, regression, e2e, known-issues)
mvn clean test -DbaseUrl=https://other-env.example.com   # override any config value
```

Any key in `config.properties` (`baseUrl`, `username`, `password`) can be overridden with `-Dkey=value`.
A missing key fails fast with a clear error. Failed tests are retried once automatically.

## Reports

**Allure** (recommended): every test shows its steps plus the full request and response.

```bash
mvn clean test        # run tests, raw results go to target/allure-results
mvn allure:serve      # build the report and open it in your browser
mvn allure:report     # or save it to target/site/allure-maven-plugin
```

The latest report from `main` is published to [GitHub Pages](https://kanhiyaog1997.github.io/restassured-api-automation), with trend charts across runs.
For pull requests, download the **allure-report** artifact from the workflow run, unzip it and serve the
folder (e.g. `npx http-server` inside it). Opening the HTML file directly from disk shows an
empty report because browsers block it from loading its data files.

Plain TestNG/Surefire reports are also in `target/surefire-reports/`.

## Test Scenarios

**62 test runs** in total: 8 core, 12 edge cases, 3 smoke, 10 sanity, 4 end-to-end, 7 schema, 18 data-driven (4 methods).

| # | Test | Endpoint | Expected |
|---|---|---|---|
| 1 | Valid login returns token | POST /auth | 200, token present |
| 2 | Wrong password | POST /auth | "Bad credentials" |
| 3 | Create booking | POST /booking | 200, booking id returned |
| 4 | Get booking | GET /booking/{id} | 200, data matches |
| 5 | Update booking (with token) | PUT /booking/{id} | 200, data updated |
| 6 | Delete booking (with token) | DELETE /booking/{id} | 201 |
| 7 | Get deleted booking | GET /booking/{id} | 404 |
| 8 | Update without token | PUT /booking/{id} | 403 |
| 9 | depositpaid=false, no additionalneeds | POST /booking | 200 |
| 10 | totalprice = 0 | POST /booking | 200, price echoed |
| 11 | 255-char firstname | POST /booking | 200 |
| 12 | Unicode name ("José Ñoño") | POST /booking | 200, name echoed |
| 13 | Checkout before checkin | POST /booking | 200 (no date validation) |
| 14 | Filter by checkin date | GET /booking?checkin= | 200, list |
| 15 | Filter by unknown name | GET /booking?firstname= | 200, empty list |
| 16 | Update with Basic auth header | PUT /booking/{id} | 200 |
| 17 | PATCH non-existent booking | PATCH /booking/{id} | 405 |
| 18 | Delete same booking twice | DELETE /booking/{id} | 201, then 405 |
| 19 | Content-Type text/plain | POST /booking | not 200 (500) |
| 20 | Auth response time | POST /auth | < 3 seconds |
| 21 | Health check | GET /ping | 201 |
| 22 | List all bookings | GET /booking | 200, non-empty list |
| 23 | Get an existing booking | GET /booking/{id} | 200, firstname present |
| 24 | Created booking matches all fields | POST + GET /booking | all 7 values match |
| 25 | PATCH changes only one field | PATCH /booking/{id} | 200, other fields unchanged |
| 26 | PUT is saved | PUT + GET /booking/{id} | GET shows new values |
| 27 | Filter by firstname + lastname | GET /booking?firstname=&lastname= | 200, booking found |
| 28 | Filter by checkout date | GET /booking?checkout= | 200, list |
| 29 | Get non-existent booking | GET /booking/{id} | 404 |
| 30 | Delete without token | DELETE /booking/{id} | 403 |
| 31 | PATCH without token | PATCH /booking/{id} | 403 |
| 32 | PUT with invalid token | PUT /booking/{id} | 403 |
| 33 | Content-Type header | GET /booking/{id} | application/json |
| 34 | E2E: full guest journey | auth → create → search → get → patch → delete | 404 at the end |
| 35 | E2E: change dates | PUT + GET /booking?checkin= | booking found by new date |
| 36 | E2E: one token, many actions | PATCH + DELETE x2 | all succeed |
| 37 | E2E: 3 bookings lifecycle | create x3 → filter → delete x3 → filter | 3 found, then 0 |
| 38 | Auth token schema | POST /auth | matches auth-token.json |
| 39 | Auth error schema | POST /auth | matches auth-error.json |
| 40 | Create booking schema | POST /booking | matches create-booking-response.json |
| 41 | Get booking schema | GET /booking/{id} | matches booking.json |
| 42 | Booking id list schema | GET /booking | matches booking-ids.json |
| 43 | PUT response schema | PUT /booking/{id} | matches booking.json |
| 44 | PATCH response schema | PATCH /booking/{id} | matches booking.json |
| 45–49 | Valid booking variations (5 rows) | POST + GET /booking | saved exactly as sent |
| 50–53 | Malformed JSON (4 rows) | POST /booking | 400 |
| 54–58 | Invalid login variations (5 rows) | POST /auth | "Bad credentials", no token |
| 59–62 | Known API issues (4 rows, group `known-issues`) | POST /booking | 500 / wrong types accepted |

## Key Concepts Used

- **Request Specification** to reuse base URL and headers
- **Request chaining**: booking id from create is used in get/update/delete
- **POJO serialization/deserialization** with Jackson
- **Config-driven** URLs and credentials, overridable with `-D` system properties
- **Allure reporting** with request/response attachments (via `AllureRestAssured` filter)
- **Retry once** on failure via a TestNG listener (handles flaky public API)
- **Positive and negative** test cases
- **Edge cases** with documented real API behaviour (`// Known API behaviour`)
- **Groups**: `smoke`, `sanity`, `regression`, `e2e`, `known-issues`
- **JSON schema validation** (required fields, types, `yyyy-MM-dd` date pattern, no extra fields)
- **Data-driven tests** with TestNG `@DataProvider`
- **Response-time** and header assertions
- **CI** with GitHub Actions
