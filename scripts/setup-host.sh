#!/usr/bin/env bash
# Installe l'outillage Android sur un poste Ubuntu : JDK 21, Android Studio (archive Google), accès KVM.
# À lancer une fois : sudo ./scripts/setup-host.sh
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "Lance ce script avec sudo : sudo $0" >&2
  exit 1
fi
TARGET_USER="${SUDO_USER:?Lance via sudo depuis ton compte utilisateur}"

echo "→ JDK 21 (pour ./gradlew en terminal ; Android Studio embarque le sien)"
apt-get update -qq
apt-get install -y -qq openjdk-21-jdk

# Archive officielle depuis dl.google.com (le Snap Store plafonne parfois à ~100 ko/s).
# Version : https://developer.android.com/studio (ou jb.gg/android-studio-releases-list.json)
STUDIO_VERSION="2026.2.2.8"
STUDIO_URL="https://dl.google.com/android/studio/ide-zips/${STUDIO_VERSION}/android-studio-rabbit2-linux.tar.gz"
echo "→ Android Studio ${STUDIO_VERSION} (/opt/android-studio)"
if [[ ! -x /opt/android-studio/bin/studio.sh ]]; then
  tmp="$(mktemp -d)"
  curl -fL --progress-bar -o "$tmp/studio.tar.gz" "$STUDIO_URL"
  tar -xzf "$tmp/studio.tar.gz" -C /opt
  rm -rf "$tmp"
fi
ln -sf /opt/android-studio/bin/studio.sh /usr/local/bin/android-studio
cat > /usr/share/applications/android-studio.desktop <<'DESKTOP'
[Desktop Entry]
Type=Application
Name=Android Studio
Icon=/opt/android-studio/bin/studio.svg
Exec=/opt/android-studio/bin/studio.sh %f
Categories=Development;IDE;
Terminal=false
StartupWMClass=jetbrains-studio
DESKTOP

echo "→ Accès KVM pour l'émulateur"
usermod -aG kvm "$TARGET_USER"

PROFILE="/home/$TARGET_USER/.zshrc"
[[ -f "$PROFILE" ]] || PROFILE="/home/$TARGET_USER/.bashrc"
if ! grep -q "ANDROID_HOME" "$PROFILE"; then
  cat >> "$PROFILE" <<'RC'

# Android SDK (installé par Android Studio au premier lancement)
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
RC
  echo "→ ANDROID_HOME ajouté à $PROFILE"
fi

cat <<'MSG'

Terminé. Étapes suivantes (sans sudo) :
  1. Déconnecte-toi / reconnecte-toi (groupe kvm), ou lance : newgrp kvm
  2. Lance Android Studio (android-studio) : assistant → Standard → il installe le SDK dans ~/Android/Sdk
  3. Ouvre le dossier android/ du repo, laisse Gradle synchroniser
  4. Device Manager → crée un émulateur (Pixel, image Android 16/17 x86_64) ou branche ton téléphone (débogage USB)
MSG
