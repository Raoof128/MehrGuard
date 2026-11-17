#!/bin/bash
# ==============================================================================
# Performance Verification
# ==============================================================================
# Runs PerformanceBenchmarkTest on this machine's JVM. The test warms up, times
# repeated analyses and fails if a latency threshold written in the test is
# exceeded (for a full analysis of one URL: P99 under 50 ms). The measured
# values are printed by the test. Latency on phones is not measured here.
# ==============================================================================

set -eo pipefail

echo "Verifying performance claims..."
echo ""

# Run the performance benchmark tests
./gradlew :common:desktopTest \
    --tests "*PerformanceBenchmarkTest*" \
    --no-daemon \
    --quiet \
    2>&1 | tail -25

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ PERFORMANCE VERIFICATION PASSED"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "What the tests assert (see PerformanceBenchmarkTest and PerformanceRegressionTest):"
echo "  • Full analysis of one URL: P99 under 50 ms"
echo "  • The measured values are in the test output above, not in this script"
echo ""
echo "Why this matters:"
echo "  • Real-time feedback during QR scanning"
echo "  • No UI jank or delay"
echo "  • Battery efficient (minimal CPU time)"
echo ""
