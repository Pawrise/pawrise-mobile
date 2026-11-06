#!/usr/bin/env bash
# Rend le mock d'API (port 4010) joignable en http://localhost:4010 depuis l'émulateur ou le téléphone branché.
set -euo pipefail
adb reverse tcp:4010 tcp:4010
echo "OK : l'app joint le mock sur http://localhost:4010"
