#!/usr/bin/env bash
# Kritik asılılıqların iOS üçün HƏLL OLUNMUŞ versiyalarını göstərir.
# Catalog-dakı yazı deyil — Gradle-ın konflikt həllindən sonrakı real versiya.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$ROOT"

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
CONF="iosSimulatorArm64CompileKlibraries"
DEPS=("${@:-}")
[ -z "${DEPS[0]:-}" ] && DEPS=(ktor-client-core ktor-client-darwin kotlinx-datetime kotlinx-serialization-json supabase-kt)

echo "═══ Konfiqurasiya: $CONF"
echo "═══ Catalog-dakı yazılar:"
grep -nE '^(ktor|kotlinxDatetime|kotlinx-datetime|serialization|supabase|kotlinxSerialization)' gradle/libs.versions.toml || true
echo

for d in "${DEPS[@]}"; do
  echo "───────── $d ─────────"
  ./gradlew :shared:dependencyInsight --configuration "$CONF" --dependency "$d" -q --console=plain 2>&1 \
    | grep -vE '^\s*$|^Task |^> Task' \
    | head -25
  echo
done

echo "═══ 'X -> Y (conflict resolution)' sətirləri = sürüşmə var."
echo "═══ Versiya dəyişdirsən: /verify + /ios-check (yaşıl hədəf bu tələyə sübut deyil)."
./gradlew --stop >/dev/null 2>&1
