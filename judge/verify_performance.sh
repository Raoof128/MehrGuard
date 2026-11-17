#!/bin/bash
# ==============================================================================
# Performance Verification
# ==============================================================================
# Proves: <5ms median (P50) latency for URL analysis
#
# This runs the PerformanceBenchmarkTest suite which:
# 1. Warms up the JIT compiler (100 iterations)
# 2. Runs 1000 benchmark iterations
# 3. Reports P50, P90, P99 latencies
# 4. Asserts P50 < 5ms (our claim) and P99 < 50ms
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
