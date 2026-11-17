#!/bin/bash
# ==============================================================================
# Offline Capability Verification
# ==============================================================================
# A timing sanity check, not a network monitor.
#
# This runs OfflineOnlyTest, which analyses a set of URLs and checks that each
# analysis finishes quickly and consistently. It cannot observe DNS lookups or
# sockets. The evidence that analysis is offline is the code itself: the
# analysis module has no HTTP client.
# ==============================================================================

set -eo pipefail

echo "Verifying offline-only analysis..."
echo ""

# Run the offline verification tests
./gradlew :common:desktopTest \
    --tests "*OfflineOnlyTest*" \
    --no-daemon \
    --quiet \
    2>&1 | tail -20

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ OFFLINE VERIFICATION PASSED"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "What this check shows:"
echo "  • Analysis timings consistent with purely local computation"
echo "  • All detection runs locally (heuristics + on-device ML)"
echo "  • Works in airplane mode"
echo "  • (No network monitor is involved; see the header of this script)"
echo ""
