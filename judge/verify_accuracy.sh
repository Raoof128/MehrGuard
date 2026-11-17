#!/bin/bash
# ==============================================================================
# Detection Accuracy Verification
# ==============================================================================
# Runs two tests and prints what they measure. Nothing below is a fixed figure:
#
# 1. AccuracyVerificationTest: 41 URLs written into the test (21 phishing,
#    20 legitimate); asserts precision, recall and F1 floors.
# 2. DatasetEvaluationTest: the committed dataset data/test_urls.csv
#    (50 legitimate, 50 constructed phishing URLs); prints the confusion
#    matrix, the metrics, a per-category breakdown and every misclassified row.
#
# Both sets were written by the project's author, who also wrote the rules.
# These are checks on known patterns, not a held-out benchmark.
# ==============================================================================

set -euo pipefail

echo "Running the accuracy tests..."
echo ""

./gradlew :common:desktopTest \
    --tests "*AccuracyVerificationTest*" \
    --tests "*DatasetEvaluationTest*" \
    --no-daemon \
    --quiet

echo ""
echo "Measured on data/test_urls.csv:"
echo ""
cat common/build/reports/dataset-evaluation.txt
echo ""
