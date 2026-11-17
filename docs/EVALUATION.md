# Evaluation

What has been measured, how to reproduce it, and what it does not show. Every figure on this page is printed by a test that CI runs on each push. None is typed in from elsewhere.

## The committed dataset

[`data/test_urls.csv`](../data/test_urls.csv) holds 100 labelled URLs: 50 legitimate sites and 50 constructed phishing patterns on fake domains. The project's author wrote all of them. [`DatasetEvaluationTest`](../common/src/desktopTest/kotlin/com/raouf/mehrguard/core/DatasetEvaluationTest.kt) runs the engine over the file and prints the confusion matrix. A phishing row counts as detected when the verdict is SUSPICIOUS or MALICIOUS. A legitimate row counts as correct only when it is SAFE.

Measured in CI on 17 November 2026, before and after the bug fix described below:

| | Before the fix | After the fix |
|---|---|---|
| Phishing rows flagged | 49 of 50 | 50 of 50 |
| Legitimate rows flagged | 9 of 50 | 0 of 50 |
| Precision | 84.5% | 100.0% |
| Recall | 98.0% | 100.0% |
| F1 | 90.7% | 100.0% |
| Accuracy | 90.0% | 100.0% |

Of the 50 phishing rows, 46 are returned as SUSPICIOUS and 4 as MALICIOUS.

### Why the second column is weak evidence

A perfect score on this set does not mean the engine is right about URLs in general.

- **The set is not held out.** The author wrote the URLs and the rules. The table shows that the rules fire on the patterns they were written for.
- **The last ten errors were fixed by looking at them.** The fix below was made after reading which rows failed. Any improvement measured on those same rows is expected, and says little about unseen sites.
- **The set can no longer tell a good change from a bad one.** With every row correct, it works only as a regression guard. `DatasetEvaluationTest` now fails if a row is misclassified.
- **No external data.** No PhishTank, OpenPhish or other live feed has been evaluated, so there is no estimate of real-world recall or of the false-alarm rate on ordinary browsing.

### The bug that was fixed

Before the fix, the nine false alarms and the one miss had three causes. The test prints the engine's signals for each misclassified row, which is how they were found.

1. **Hosts were split in the wrong place.** Three components took the last two labels of a host as the site, so for `www.commbank.com.au` the site name was `com`. That is within two edits of the brand `zoom`, so the brand check reported every site under `.com.au`, `.co.uk`, `.co.jp` and similar suffixes as a lookalike. All four Australian banks in the set were flagged this way. The repository already had a public-suffix list. These components now use it.
2. **Short names matched almost anything.** The edit-distance check allowed two edits whatever the length of the brand, so `cnn` matched `anz`, `bbc` matched `hsbc` and `etsy` matched `ebay`. It also ran against a brand's own site, so `spotify.com` was reported as a misspelling of `shopify`. The allowed distance now scales with the length of the brand (one edit for three to five letters, two for longer names), and the check is skipped when the site is a listed brand's official domain.
3. **Internationalised hosts were not decoded.** `xn--pypal-4ve.com` is the ASCII form of `paypal.com` with a Cyrillic letter. The lookalike list is Unicode text, so the ASCII form never matched. Hosts are now decoded from Punycode first.

A review of the first version of this fix found that it exempted too much, and two rules were added in response. A brand's own site is exempt only from checks for that brand, and hosts under shared hosting suffixes such as `github.io` or `cloudapp.azure.com` are never exempt. The brand's exact name used as a site name on a domain the brand does not own (`paypal-security.com`, `commbank.co.uk`) is now reported.

These are fixes to logic that was wrong for any host of the same shape, and the regression tests ([`BrandDetectorFalseAlarmTest`](../common/src/commonTest/kotlin/com/raouf/mehrguard/engine/BrandDetectorFalseAlarmTest.kt), [`PublicSuffixHostSplitTest`](../common/src/commonTest/kotlin/com/raouf/mehrguard/core/PublicSuffixHostSplitTest.kt)) use hosts that are mostly outside the dataset, for example `woolworths.com.au`, `bbc.co.uk`, `nba.com` and `ibm.com`. No weight or threshold was adjusted to move a dataset row. The exact-name rule is new behaviour and has a cost that has not been measured: see [LIMITATIONS.md](LIMITATIONS.md).

## The smaller in-test set

[`AccuracyVerificationTest`](../common/src/commonTest/kotlin/com/raouf/mehrguard/core/AccuracyVerificationTest.kt) holds 41 author-written URLs inside the test (21 phishing-shaped, 20 legitimate) and fails if precision drops below 80%, recall below 85% or F1 below 80%.

| Measured in CI, 17 Nov 2026 | Before the fix | After the fix |
|---|---|---|
| Precision | 90.9% | 100.0% |
| Recall | 95.2% | 95.2% |
| F1 | 93.0% | 97.5% |

One of its 21 phishing-shaped URLs is still returned as SAFE. The same caveats apply.

## Latency

`PerformanceRegressionTest` and `PerformanceBenchmarkTest` time the engine on the CI runner's JVM and fail if a full analysis of one URL exceeds 50 ms at the 99th percentile. The measured values are in the test output of each CI run. Nothing has been measured on a phone or in a browser.

## Reproduce

```bash
./scripts/eval.sh                 # runs DatasetEvaluationTest and prints its report
./judge/verify_accuracy.sh        # both accuracy tests, then the report
./gradlew :common:desktopTest --tests "*DatasetEvaluationTest*" --info
```

The report is also written to `common/build/reports/dataset-evaluation.txt`, and it appears in the "Measure Dataset Metrics" step of every CI run.

## Figures in earlier documents

Documents written for the December 2025 contest submission quote 87% F1, a 330-URL corpus, a 2,500-URL confusion matrix and other results. Those sets are not in this repository and the figures cannot be reproduced. [docs/contest/README.md](contest/README.md) lists them with their corrections.

## What a real evaluation would need

Labelled URLs from a public feed, collected by someone other than the rule author. A test split set aside before any rule is changed. Results reported on that split only, with the false-alarm rate measured on a large sample of ordinary sites.
