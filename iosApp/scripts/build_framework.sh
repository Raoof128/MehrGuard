#!/bin/bash
# Build KMP iOS Framework for Simulator
# Run this before opening Xcode

set -e

echo "🔨 Building KMP iOS Framework for Simulator..."

IOSAPP_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Discover gradle root (supports running from iOS-only checkout or monorepo)
if [ -f "$IOSAPP_ROOT/gradlew" ]; then
  GRADLE_ROOT="$IOSAPP_ROOT"
elif [ -f "$IOSAPP_ROOT/../gradlew" ]; then
  GRADLE_ROOT="$(cd "$IOSAPP_ROOT/.." && pwd)"
else
  echo "❌ gradlew not found."
  echo "   Expected at:"
  echo "   - $IOSAPP_ROOT/gradlew"
  echo "   - $IOSAPP_ROOT/../gradlew"
  echo "   This repository appears to be iOS-only. Build the shared KMP framework in the full monorepo first."
  exit 1
fi

# Build the debug framework for iOS Simulator (Apple Silicon)
(
  cd "$GRADLE_ROOT"
  ./gradlew :common:linkDebugFrameworkIosSimulatorArm64 --no-daemon
)

# Copy framework to iosApp directory for Xcode linking
FRAMEWORK_SRC="$GRADLE_ROOT/common/build/bin/iosSimulatorArm64/debugFramework/common.framework"
FRAMEWORK_DST="$IOSAPP_ROOT/Frameworks"

if [ ! -d "$FRAMEWORK_SRC" ]; then
  echo "❌ Build finished but framework was not found at:"
  echo "   $FRAMEWORK_SRC"
  exit 1
fi

mkdir -p "$FRAMEWORK_DST"
rm -rf "$FRAMEWORK_DST/common.framework"
cp -R "$FRAMEWORK_SRC" "$FRAMEWORK_DST/"

echo ""
echo "✅ Framework built and copied to: $FRAMEWORK_DST/common.framework"
echo ""
echo "📱 Next steps:"
echo "   1. Open iosApp/MehrGuard.xcodeproj in Xcode"
echo "   2. Select 'MehrGuard' target → 'General' tab"
echo "   3. Under 'Frameworks, Libraries, and Embedded Content'"
echo "   4. Click '+' → 'Add Other...' → 'Add Files...'"
echo "   5. Navigate to iosApp/Frameworks/common.framework"
echo "   6. Set 'Embed' to 'Embed & Sign'"
echo "   7. Select iPhone Simulator and run!"
echo ""
