#!/bin/bash
#
# Mehr Guard evaluation
#
# Runs the detection engine over data/test_urls.csv and prints the measured
# confusion matrix, precision, recall and F1. Every figure comes from the test
# run; this script contains none.
#
# Usage: ./scripts/eval.sh
#
# Copyright 2025-2026 Mehr Guard Contributors
# Licensed under Apache 2.0
#

set -euo pipefail

DATASET_FILE="data/test_urls.csv"

if [ ! -f "$DATASET_FILE" ]; then
    echo "Error: dataset not found at $DATASET_FILE" >&2
    exit 1
fi

echo "Running DatasetEvaluationTest over $DATASET_FILE ..."
echo ""

./gradlew :common:desktopTest --tests "*DatasetEvaluationTest*" --no-daemon --quiet

cat common/build/reports/dataset-evaluation.txt
echo ""
echo "The dataset was written by the project's author. See docs/EVALUATION.md for what these figures do and do not show."
