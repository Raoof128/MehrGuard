#!/bin/bash
# ==============================================================================
# Mehr Guard Judge Verification Suite
# ==============================================================================
# Runs the four check scripts in this directory and reports which passed.
# Each script says in its header what it does and does not show.
#
# Usage: ./judge/verify_all.sh
# ==============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    Mehr Guard verification scripts                   ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

PASS_COUNT=0
FAIL_COUNT=0

run_verification() {
    local name="$1"
    local script="$2"
    
    echo -e "${YELLOW}▶ $name${NC}"
    echo "─────────────────────────────────────────────────────"
    
    if bash "$script"; then
        echo -e "${GREEN}✅ $name PASSED${NC}"
        ((PASS_COUNT++))
    else
        echo -e "${RED}❌ $name FAILED${NC}"
        ((FAIL_COUNT++))
    fi
    echo ""
}

echo "Starting verification (estimated time: 3-5 minutes)..."
echo ""

# ============================================================================
# 1. OFFLINE CAPABILITY
# ============================================================================
run_verification "1️⃣  On-device analysis (source scan and timing check)" "./judge/verify_offline.sh"

# ============================================================================
# 2. PERFORMANCE CLAIMS
# ============================================================================
run_verification "2️⃣  Performance (latency assertions)" "./judge/verify_performance.sh"

# ============================================================================
# 3. DETECTION ACCURACY
# ============================================================================
run_verification "3️⃣  Detection Accuracy (measured on the committed dataset)" "./judge/verify_accuracy.sh"

# ============================================================================
# 4. PLATFORM PARITY
# ============================================================================
run_verification "4️⃣  Platform parity (JVM parity tests; other targets build)" "./judge/verify_parity.sh"

# ============================================================================
# SUMMARY
# ============================================================================
echo ""
echo -e "${CYAN}══════════════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                         VERIFICATION SUMMARY                          ${NC}"
echo -e "${CYAN}══════════════════════════════════════════════════════════════════════${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    ✅ ALL $PASS_COUNT VERIFICATIONS PASSED!                      ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "What passed:"
    echo "  • The analysis packages name no networking API (text scan), and analysis stays within a time bound"
    echo "  • The latency assertions in the performance tests hold on this machine"
    echo "  • The accuracy tests ran; the measured table is printed above by verify_accuracy.sh"
    echo "  • The parity tests pass on the JVM and the other targets build"
    echo ""
    exit 0
else
    echo -e "${RED}╔══════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║                    ❌ $FAIL_COUNT VERIFICATION(S) FAILED                         ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Please check the output above for details."
    exit 1
fi
