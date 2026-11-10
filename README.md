# RestAssured API Automation

![API Tests](https://github.com/kanhiyaOG1997/restassured-api-automation/actions/workflows/api-tests.yml/badge.svg)

API test automation framework for the public [Restful-Booker API](https://restful-booker.herokuapp.com), built with **RestAssured**, **TestNG** and **Maven**.

## Tech Stack

| Tool | Purpose |
|---|---|
| Java 21 | Programming language |
| RestAssured 5.5 | Sending HTTP requests and validating responses |
| TestNG 7.10 | Test runner (priority, dependencies, assertions) |
| Jackson | POJO to JSON serialization and deserialization |
| Maven | Build and dependency management |
| GitHub Actions | CI: runs tests on every push |

## Project Structure

```
src/test/java/com/kanhaiya/api
├── base/BaseTest.java        # Common setup: request spec, auth token
├── utils/ConfigReader.java   # Reads config.properties
├── utils/TestDataBuilder.java # Random valid booking data
├── models/                   # POJOs: Booking, BookingDates
└── tests/
    ├── AuthTest.java         # POST /auth tests
    ├── BookingTest.java      # CRUD flow + negative test
    └── BookingEdgeCaseTest.java # Boundary values, filters, known API quirks
src/test/resources/config.properties   # Base URL and credentials
testng.xml                              # Test suite
.github/workflows/api-tests.yml         # CI pipeline
```

## How to Run

```bash
mvn clean test
```

Reports are generated in `target/surefire-reports/`.

## Test Scenarios

**20 tests** in total (8 core + 12 edge cases).

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

## Key Concepts Used

- **Request Specification** to reuse base URL and headers
- **Request chaining**: booking id from create is used in get/update/delete
- **POJO serialization/deserialization** with Jackson
- **Config-driven** test data (no hard-coded URLs)
- **Positive and negative** test cases
- **Edge cases** with documented real API behaviour (`// Known API behaviour`)
- **Groups** (`regression`) and response-time assertions
- **CI** with GitHub Actions
