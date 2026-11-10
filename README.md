# RestAssured API Automation

![API Tests](https://github.com/kanhiyaOG1997/restassured-api-automation/actions/workflows/api-tests.yml/badge.svg)

An API test automation framework I built for the public [Restful-Booker API](https://restful-booker.herokuapp.com), using RestAssured, TestNG and Maven.

Live Allure report (updated on every push to `main`): https://kanhiyaog1997.github.io/restassured-api-automation

## Tech stack

| Tool | What I use it for |
|---|---|
| Java 21 | Language |
| RestAssured 5.5 | Sending requests and validating responses |
| TestNG 7.10 | Test runner, groups, data providers, parallel runs |
| Jackson | Converting POJOs to JSON and back |
| Maven | Build and dependencies |
| Allure 2 | HTML report with each request and response attached |
| GitHub Actions | Runs the suite on every push and pull request |

## Project structure

```
src/test/java/com/kanhaiya/api
├── base/BaseTest.java           # request spec, timeout, token, shared helpers
├── listeners/                   # retry a failed test once
├── models/                      # Booking, BookingDates (POJOs)
├── utils/ConfigReader.java      # reads config, supports -D overrides
├── utils/TestDataBuilder.java   # random valid booking data
└── tests/
    ├── SmokeTest.java
    ├── AuthTest.java
    ├── BookingTest.java
    ├── SanityTest.java
    ├── BookingEdgeCaseTest.java
    ├── SchemaValidationTest.java
    ├── BookingDataDrivenTest.java
    └── BookingE2ETest.java

src/test/resources/config.properties   # base URL and credentials
src/test/resources/schemas/            # JSON schemas
testng.xml                             # suite, parallel setup, retry listener
.github/workflows/api-tests.yml        # CI
```

## Running the tests

```bash
mvn clean test                     # everything
mvn clean test -Dgroups=smoke      # just one group
```

Groups: `smoke`, `sanity`, `regression`, `e2e`, `known-issues`.

Values in `config.properties` (`baseUrl`, `username`, `password`) can be overridden from the command line, e.g. `mvn clean test -DbaseUrl=https://my-staging-url`. If a key is missing, the run stops with an error that says which key.

## Reports

After a run you can open the Allure report locally:

```bash
mvn allure:serve
```

It shows every test with the full request and response, grouped by area and severity. `mvn allure:report` saves it to `target/site/allure-maven-plugin` instead.

In CI the report from `main` is published to [GitHub Pages](https://kanhiyaog1997.github.io/restassured-api-automation). Pull requests only upload it as an `allure-report` artifact. If you download that one, serve the folder (for example `npx http-server`); opening the HTML file straight from disk shows an empty page.

The plain TestNG reports are in `target/surefire-reports/`.

## What's tested

61 test runs in total. The whole suite takes about 40 seconds.

**Smoke (3)**: the API answers at all. `GET /ping`, list bookings, fetch one booking.

**Auth (2)**: valid login returns a token; a wrong password returns "Bad credentials".

**Booking CRUD (6)**: one chained flow. Create → get → update → delete → confirm 404. Also checks that an update without a token returns 403.

**Sanity (9)**:
- PATCH changes only the field you send
- PUT is actually saved (checked with a GET)
- Filter by first + last name, and by checkout date
- 404 for a booking that doesn't exist
- 403 for DELETE/PATCH without a token and PUT with a bad token
- `Content-Type` is JSON

**Edge cases (12)**:
- Price 0, a 255-character name, Unicode names
- No `additionalneeds`
- Checkout before checkin
- Filters with no results
- Basic auth instead of a cookie token
- Deleting the same booking twice
- Wrong `Content-Type`
- Login responds in under 3 seconds

**Schema validation (7)**: auth (token and error), create, get, list, PUT and PATCH responses are checked against JSON schemas. Required fields, types, `yyyy-MM-dd` dates, and no unexpected fields.

**Data-driven (18 runs, 4 methods)**:
- 5 valid booking variations saved exactly as sent
- 4 kinds of malformed JSON → 400
- 5 invalid login variations
- 4 known API bugs (see below)

**End-to-end (4)**:
- A full guest journey: login, create, search, view, edit, cancel
- Changing dates and finding the booking by the new date
- One token used for several actions
- Creating 3 bookings, finding exactly 3, deleting them all

### Known API bugs

Restful-Booker has a few bugs. Where the real behaviour is wrong, the test asserts what the API actually does and has a `// Known API behaviour` comment, so the suite stays green and the bug is still written down:

- Missing required fields or an empty body return **500** instead of 400
- `totalprice: "abc"` is accepted and saved as `null`; `depositpaid: "yes"` is saved as `true`
- PATCH on a missing booking and a second DELETE return **405** instead of 404
- No validation that checkout is after checkin
- `/ping` returns 201, and a failed login returns 200

The four worst ones are in their own `known-issues` group, so they can be run (or skipped) separately.

## Design decisions

Some choices I made and why:

- **Tests create their own data.** Apart from the CRUD chain in `BookingTest` (which is chained on purpose) and the smoke tests (which only read), every test creates the booking it needs. So tests don't depend on each other or on other people's data, and that's also what makes running them in parallel safe.

- **Random test data.** Restful-Booker is shared by lots of people, so fixed names would clash with other people's bookings. `TestDataBuilder` generates fresh values each time. Tests that search by name use a unique suffix.

- **Retry once on failure.** The public API sometimes resets its data or responds slowly, which made a few runs fail for no real reason. A listener retries a failed test once. A real bug still fails twice and shows up in the report, so it doesn't hide anything.

- **Parallel runs (4 threads).** This took the suite from about 100s down to about 40s. Each class runs on its own thread, and the methods inside a class still run in order, so the CRUD chain still works.

- **Asserting the API's real behaviour.** If the test expected the "correct" status code, the suite would always be red and nobody would notice a new failure. Instead the current behaviour is asserted and commented, and the worst cases are grouped under `known-issues`.

- **15 second timeout.** Without it a hung request can block the whole run for minutes. With it, the test fails with a clear timeout and the rest keep going.

- **Config outside the code, with `-D` overrides.** No URLs or passwords in test classes. CI or another environment can change the base URL without editing files.

- **Schema validation on top of value checks.** Value checks only look at the fields I picked. A schema catches a missing field, a wrong type or a new field anywhere in the response.

- **Shared helpers in `BaseTest`.** `createBooking`, `getBooking` and `assertSameBooking` live in one place, because they were being copied into several test classes.

- **Allure with request/response logging.** When something fails in CI, I can see exactly what was sent and what came back, without re-running it locally.
