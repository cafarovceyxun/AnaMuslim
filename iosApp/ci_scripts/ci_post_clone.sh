#!/bin/sh
# Xcode Cloud runs this automatically after cloning, before the build starts.
#
# The path is fixed by Apple: the `ci_scripts` directory must sit "in the same directory as your
# Xcode project or workspace" - so for this repo that is `iosApp/ci_scripts/`, NOT the repository
# root. It lived at the root until 2026-08-14 and Xcode Cloud simply never ran it: builds 3, 4, 5
# and 10 all died ~35s in, first with a bare `PhaseScriptExecution failed` and later with this
# project's own "no JDK found" message. The script must also stay executable (chmod +x) - Xcode
# Cloud only respects the shebang for executable files.
#
# Why it exists: `iosApp`'s "Compile Kotlin Framework" phase shells out to `./gradlew`, and the
# Xcode Cloud image ships no JDK at all - so without this the phase dies immediately with a bare
# "PhaseScriptExecution failed with a nonzero exit code" and no explanation. That is exactly how
# builds 3, 4 and 5 failed, each after ~34 seconds.
#
# JDK 21, not 17: the project compiles to JVM 17 bytecode (`jvmTarget = JVM_17`) but pins no
# toolchain, so the JDK that launches Gradle only has to satisfy Gradle 9 / AGP 9, which want 17 as
# a floor. 21 is the LTS comfortably above that floor and can still target 17. The developer
# machine runs Android Studio's bundled JDK 25, so exact parity is not achievable anyway.
#
# Only a JDK is installed, not the Android SDK: building the iOS framework
# (`:shared:embedAndSignAppleFrameworkForXcode`) configures the Android target but never runs an
# Android task, and AGP tolerates a missing `sdk.dir` until one actually does - verified by
# configuring `:shared` with `sdk.dir` removed.

set -e

echo "--- Installing a JDK for the Gradle build"
brew install --quiet openjdk@21

# No `sudo` here, deliberately. This used to symlink the keg into
# /Library/Java/JavaVirtualMachines so `/usr/libexec/java_home` could see it - but Xcode Cloud has
# no interactive terminal, so sudo dies with "a terminal is required to read the password" and the
# whole script exits 1. That is how build 12 failed, *after* the JDK had already installed fine.
#
# Instead the keg path is left where Homebrew put it and the Xcode "Compile Kotlin Framework"
# phase looks for it directly. Env vars exported here would not survive into xcodebuild, so the
# path has to be discoverable, not inherited - keep the two in sync if this moves.
JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"

if [ ! -x "$JDK_HOME/bin/java" ]; then
  echo "error: openjdk@21 installed but no java at $JDK_HOME/bin/java"
  exit 1
fi

echo "--- JDK in place at $JDK_HOME"
"$JDK_HOME/bin/java" -version
