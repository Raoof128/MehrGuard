# Scoring model

This describes what the code does. For how the weights came about, see [ML_TRAINING.md](ML_TRAINING.md): they are set by hand, not learned from data.

## Inputs

`FeatureExtractor.kt` turns a URL into fifteen numbers, including URL, host and path length, subdomain count, whether the scheme is HTTPS, whether the host is an IP address, the presence of `@`, digit and special-character ratios, and whether the TLD is on a risky list.

## Scoring

`EnsembleModel.kt` combines three scores with fixed weights:

| Component | Weight | What it is |
|---|---|---|
| Logistic score | 0.40 | A weighted sum of the fifteen features through a sigmoid (`LogisticRegressionModel.kt`), with hand-set weights |
| "Boosting" score | 0.35 | The sum of ten hand-written threshold rules, each on one feature |
| "Stump" score | 0.25 | Five more threshold rules |

The result is a number between 0 and 1. It is a weighted average, not a majority vote, and none of the components is fitted to data.

## How it is used

`PhishingEngine` runs the 25 heuristics in `HeuristicsEngine.kt`, the brand detector and this model, and `ScoreCalculator` turns their outputs into a verdict of SAFE, SUSPICIOUS or MALICIOUS. Some heuristics (a homograph host, an `@` in the URL) escalate the verdict on their own.

## Measured behaviour

See [EVALUATION.md](EVALUATION.md). No figure for this model in isolation is reported, because it has not been evaluated on its own.
