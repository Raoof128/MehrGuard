# Malicious URL test resources

`malicious_urls.csv` holds 140 phishing-shaped URLs written by the project's author, grouped by attack category. `MaliciousUrlProofTest` runs the engine over them and fails if fewer than 80% are flagged.

```bash
./gradlew :common:desktopTest --tests "com.raouf.mehrguard.benchmark.MaliciousUrlProofTest" --info
```

The test prints the count and the rate per category. No figures are recorded here, because they change with the rules. The URLs were written by the same person as the rules, so the result is a regression guard and not a benchmark.
