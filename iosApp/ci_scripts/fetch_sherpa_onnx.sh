#!/bin/sh
# SherpaOnnxC.xcframework-u (sherpa-onnx 1.13.8, hədisləri cihazda səsləndirən mühərrik) gətirir.
#
# Niyə skript: framework 81 MB-dır (simulyator dilimi 56 MB) — repoya qoyulmur, `third_party/`
# git-ə düşmür. Xcode Cloud bunu `ci_post_clone.sh`-dan çağırır; yerli maşında bir dəfə əl ilə:
#
#     sh iosApp/ci_scripts/fetch_sherpa_onnx.sh
#
# Mənbə: sherpa-onnx-un GitHub buraxılışında iOS paketi YOXDUR; rəsmi iOS binarı yalnız layihənin öz
# Flutter plaginində (`sherpa_onnx_ios`, pub.dev) gəlir. Arxiv sha256 ilə sabitlənib.
#
# ⚠️ 1.13.8-dən yuxarı qalxma: 2.0 espeak-ng-ni kitabxanadan çıxarıb, bizim Piper modeli isə fonemləri
# ondan alır. Android AAR-ı da eyni versiyadır (`app/build.gradle.kts`) — ikisini birlikdə yenilə.

set -e

VERSION="1.13.8"
SHA256="044e3a614d1847c0ff9b6475201c6e5d023cd6ca6cd17f14c16332081c01e1d2"
URL="https://pub.dev/api/archives/sherpa_onnx_ios-$VERSION.tar.gz"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEST="$ROOT/third_party/sherpa-onnx"
FRAMEWORK="$DEST/SherpaOnnxC.xcframework"
STAMP="$DEST/SherpaOnnxC.version"

if [ -d "$FRAMEWORK" ] && [ "$(cat "$STAMP" 2>/dev/null)" = "$VERSION" ]; then
  echo "--- SherpaOnnxC.xcframework $VERSION artıq var"
  exit 0
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "--- sherpa_onnx_ios $VERSION endirilir"
curl -fsSL -o "$WORK/pkg.tar.gz" "$URL"

ACTUAL="$(shasum -a 256 "$WORK/pkg.tar.gz" | awk '{print $1}')"
if [ "$ACTUAL" != "$SHA256" ]; then
  echo "error: sherpa_onnx_ios arxivinin sha256-sı uyğun gəlmir: $ACTUAL"
  exit 1
fi

# Yalnız framework qovluğu çıxarılır — paketdəki Dart/Swift plagin kodu lazım deyil.
mkdir -p "$WORK/x"
tar -xzf "$WORK/pkg.tar.gz" -C "$WORK/x" "ios/sherpa_onnx_ios/SherpaOnnxC.xcframework"

mkdir -p "$DEST"
rm -rf "$FRAMEWORK"
mv "$WORK/x/ios/sherpa_onnx_ios/SherpaOnnxC.xcframework" "$FRAMEWORK"
echo "$VERSION" > "$STAMP"
echo "--- SherpaOnnxC.xcframework $VERSION hazırdır: $FRAMEWORK"
