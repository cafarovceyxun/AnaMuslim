#!/bin/sh
# Xcode Cloud runs this after ci_post_clone.sh and immediately before each xcodebuild invocation.
# Location rule, and why it moved out of the repository root, are explained in ci_post_clone.sh.
#
# Why it exists: the app's CFBundleVersion is not written anywhere by hand. The target sets
# `GENERATE_INFOPLIST_FILE = YES` and the checked-in `iosApp/Info.plist` carries no CFBundleVersion
# key, so the value is generated from the `CURRENT_PROJECT_VERSION` build setting - which is
# committed as a literal `1` in both configurations. App Store Connect refuses an upload that
# reuses a build number already seen for the same version, so every archive after the first would
# be rejected at upload, i.e. after the whole ~20-minute build had already been paid for.
#
# CI_BUILD_NUMBER is Xcode Cloud's own per-workflow counter and never repeats, which is exactly
# the property the build number needs. MARKETING_VERSION is deliberately left alone - the release
# version is a human decision (currently 2026.08.08).
#
# The pbxproj is edited in place rather than through agvtool: agvtool expects VERSIONING_SYSTEM to
# be configured, which this project does not use, and the Xcode Cloud working copy is discarded
# after the build anyway - nothing is pushed back.

set -e

if [ -z "$CI_BUILD_NUMBER" ]; then
  echo "CI_BUILD_NUMBER is unset - not running under Xcode Cloud, leaving the build number alone."
  exit 0
fi

# Xcode Cloud exports the checkout root; fall back to it for local test runs. Two levels up, not
# one: this script lives in `iosApp/ci_scripts/`, so its grandparent is the repository root.
REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-$(cd "$(dirname "$0")/../.." && pwd)}"
PBXPROJ="$REPO_ROOT/iosApp/iosApp.xcodeproj/project.pbxproj"

if [ ! -f "$PBXPROJ" ]; then
  echo "error: project.pbxproj not found at $PBXPROJ"
  exit 1
fi

echo "--- Setting CURRENT_PROJECT_VERSION to $CI_BUILD_NUMBER"
sed -i '' "s/CURRENT_PROJECT_VERSION = [^;]*;/CURRENT_PROJECT_VERSION = $CI_BUILD_NUMBER;/g" "$PBXPROJ"

# Say so plainly if the substitution matched nothing: a silent no-op here surfaces much later as an
# "this build number has already been used" rejection, with no hint that this script is the cause.
PATCHED="$(grep -c "CURRENT_PROJECT_VERSION = $CI_BUILD_NUMBER;" "$PBXPROJ" || true)"
if [ "$PATCHED" -eq 0 ]; then
  echo "error: no CURRENT_PROJECT_VERSION entry was updated - has the build setting been renamed or removed?"
  exit 1
fi

echo "--- Build number set in $PATCHED configuration(s)"

# ---------------------------------------------------------------------------
# Memory budget for the Gradle build that produces the Kotlin framework.
#
# Build 33 (2026-08-25) died after 5m40s with `OutOfMemoryError: Java heap space` inside
# `DevirtualizationAnalysis` while running `:shared:linkReleaseFrameworkIosArm64` - the release
# device framework the archive cannot do without. The checked-in gradle.properties is tuned for a
# 16 GB developer Mac: 5 GB for the Gradle daemon, 3 GB for the Kotlin daemon and 6 GB for the
# Kotlin/Native compiler. Three JVMs that together promise more than a CI machine has, and the
# native compiler is the one that loses the race - it is the last to allocate and the only one
# whose peak is genuinely large (release LTO holds the whole program graph in memory).
#
# On CI the first two do almost nothing: one link task, no Android build, no test run. So their
# heaps are cut right down and the freed budget goes to the native compiler. The machine's own RAM
# decides how much that is, so it is measured rather than assumed - and printed, because a future
# OOM here is unreadable without knowing what the runner actually had.
#
# gradle.properties is edited in the discarded CI working copy, exactly like the pbxproj above;
# the committed file keeps the developer-machine values (4 GB there was already too little, see
# the 2026-08-25 note in IOS_MIGRATION_PLAN.md).

PROPS="$REPO_ROOT/gradle.properties"

if [ ! -f "$PROPS" ]; then
  echo "error: gradle.properties not found at $PROPS"
  exit 1
fi

TOTAL_BYTES="$(sysctl -n hw.memsize 2>/dev/null || echo 0)"
TOTAL_GB=$(( TOTAL_BYTES / 1073741824 ))
echo "--- Runner memory: ${TOTAL_GB} GB"

# A quarter of the machine, clamped to 6-16 GB. Not "everything that is free": build 34 ran on a
# 64 GB runner, was handed -Xmx61g by an earlier version of this script, and died with the *same*
# `OutOfMemoryError: Java heap space` a full 100 seconds sooner than the 6 GB attempt before it.
# A heap ceiling that close to physical RAM makes the JVM keep expanding until macOS refuses the
# commit, and the refusal surfaces as a heap OOM long before the ceiling is reached.
HEAP_GB=$(( TOTAL_GB / 4 ))
[ "$HEAP_GB" -lt 6 ] && HEAP_GB=6
[ "$HEAP_GB" -gt 16 ] && HEAP_GB=16

# Rewrite one key in place, or append it if the file has no such line, and echo what was set -
# build 36 failed in 0.4s because an edit to this script deleted this helper and `set -e` turned
# the first call into a hard exit, so the values are printed as proof that it ran.
set_prop() {
  key="$1"
  val="$2"
  if grep -q "^${key}=" "$PROPS"; then
    sed -i '' "s|^${key}=.*|${key}=${val}|" "$PROPS"
  else
    printf '%s=%s\n' "$key" "$val" >> "$PROPS"
  fi
  echo "--- ${key}=${val}"
}

# The Gradle daemon gets the same ceiling as the native compiler, because on CI it *is* the native
# compiler: the link runs inside the Gradle JVM here, while on the developer Mac it runs in a
# process of its own. Three builds pinned that down - the moment the OOM arrives tracks
# `org.gradle.jvmargs` and ignores `kotlin.native.jvmArgs` entirely:
#
#     build 33: gradle 5g, native 6g   -> died at 5m40
#     build 34: gradle 2g, native 61g  -> died at 3m59
#     build 37: gradle 4g, native 16g  -> died at 4m19
#
# Both are set regardless, so the values stay right whichever process ends up doing the work.
set_prop "org.gradle.jvmargs" "-Xmx${HEAP_GB}g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8"
set_prop "kotlin.daemon.jvmargs" "-Xmx4g"
set_prop "kotlin.native.jvmArgs" "-Xmx${HEAP_GB}g"
