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
