# Funnel1pg - Playwright BDD Automation Framework

Java + Playwright + Cucumber BDD smoke test for the Power Pro Heat funnel checkout.

## Stack
- Java 11, Maven
- Playwright 1.41
- Cucumber 7.15 + JUnit 5

## Run
```bash
mvn test                    # run smoke tests
mvn test -Dheadless=true    # CI/CD headless
mvn test -Dbrowser=firefox  # cross-browser
```

## First-time browser install
```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

## Reports
- `reports/cucumber-report.html`
- `reports/screenshots/` (on failure)
