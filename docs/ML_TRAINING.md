# Model provenance

**The model in Mehr Guard was not trained on real phishing data.** Earlier versions of this document said it was, and quoted a 10,000-URL dataset from PhishTank, OpenPhish and APWG, an AUC and training scripts. None of that exists in this repository, and those statements have been removed.

What is here:

- **The weights are set by hand.** `LogisticRegressionModel.kt` holds fifteen feature weights and a bias as constants, chosen so that features such as an IP-address host, an `@` in the URL or a risky TLD raise the score. `models/phishing_model_weights.json` holds the same values.
- **`scripts/generate_model.py` is a toy.** It builds 1,000 samples by drawing from ten fixed URLs (five safe, five phishing), fits a logistic regression to them and reports accuracy on that same data. It demonstrates the export format and is not evidence of model quality.
- **The "ensemble"** in `EnsembleModel.kt` blends that logistic score with ten hand-written threshold rules ("boosting stumps") and five more ("decision stumps") using fixed weights of 0.40, 0.35 and 0.25. Nothing in it is learned.

What this means for the results in [EVALUATION.md](EVALUATION.md): the measured 90.7% F1 on the committed 100-URL set comes mostly from the heuristics, and the scoring model adds a hand-tuned prior, not a learned one.

To make this a real model: collect labelled URLs from a public feed, hold out a test split before fitting anything, train, and report results on the held-out split only.
