#!/usr/bin/env bash
# Rend les services locaux joignables en http://localhost:<port> depuis l'émulateur ou le téléphone branché :
# API-CORE (3000), pawrise-auth (8200), pawrise-assistant (8100) et le mock Prism des tests de contrat (4010).
set -euo pipefail
for port in 3000 8200 8100 4010; do adb reverse "tcp:$port" "tcp:$port"; done
echo "OK : ports 3000, 8200, 8100 et 4010 joignables depuis l'appareil"
