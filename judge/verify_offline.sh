#!/bin/bash
# ==============================================================================
# On-device analysis check
# ==============================================================================
# Runs two tests:
#
# 1. AnalysisCodeHasNoNetworkApiTest scans the source of the analysis packages
#    (core, engine, ml, model, security) and fails if any file names a
#    networking API. This is a text scan of the code, not a network monitor.
# 2. LocalAnalysisSanityTest checks that analysis returns for every kind of
#    input, gives the same result every time, and stays within a time bound.
#
# Neither test observes sockets or DNS. Separately from analysis, the Android
# app checks GitHub Pages at startup for updated detection rules.
# ==============================================================================

set -eo pipefail

echo "Checking that the analysis code names no networking API..."
echo ""

./gradlew :common:desktopTest \
    --tests "*AnalysisCodeHasNoNetworkApiTest*" \
    --tests "*LocalAnalysisSanityTest*" \
    --no-daemon \
    --quiet \
    2>&1 | tail -20

echo ""
echo "Both tests passed."
echo ""
echo "What this shows:"
echo "  • No file in the analysis packages names a networking API (text scan)"
echo "  • Analysis returns for every input, deterministically, within a time bound"
echo "What it does not show:"
echo "  • Anything observed on the network: no monitor is involved"
echo ""
