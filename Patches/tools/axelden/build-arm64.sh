#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
EXPECTED_NDK="29.0.14206865"
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "ERROR: Set ANDROID_HOME or ANDROID_SDK_ROOT to an installed Android SDK." >&2
  exit 2
fi
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME}}"
NDK="${ANDROID_NDK_HOME:-${SDK}/ndk/${EXPECTED_NDK}}"
if [[ ! -x "$NDK/ndk-build" && ! -x "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++" ]]; then
  echo "ERROR: Android NDK $EXPECTED_NDK not found at: $NDK" >&2
  echo "Install NDK $EXPECTED_NDK in Android Studio > SDK Manager > SDK Tools." >&2
  exit 3
fi
export ANDROID_NDK_HOME="$NDK"
./gradlew --no-daemon :manager:assembleDebug -Pandroid.injected.build.abi=arm64-v8a
