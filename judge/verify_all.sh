#!/bin/bash
# ==============================================================================
# Mehr Guard Judge Verification Suite
# ==============================================================================
# This script verifies ALL reproducible claims made in the competition submission.
# Run time: ~3 minutes on Apple Silicon, ~5 minutes on Intel.
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
echo -e "${CYAN}║           🧪 Mehr Guard Judge Verification Suite                      ║${NC}"
echo -e "${CYAN}║                  KotlinConf 2026 Competition                         ║${NC}"
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
run_verification "1️⃣  Offline Analysis (Zero Network Calls)" "./judge/verify_offline.sh"

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
run_verification "4️⃣  Platform Parity (Identical Verdicts)" "./judge/verify_parity.sh"

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
    echo "All claims in the submission are reproducible."
    echo ""
    echo "Verified:"
    echo "  • Offline-first: Zero network calls during analysis"
    echo "  • Performance: <5ms median latency (10-50x target)"
    echo "  • Accuracy: 87%+ F1 score on red team corpus"
    echo "  • Parity: Identical verdicts across JVM/JS/Native"
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
