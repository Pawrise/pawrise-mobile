#!/usr/bin/env bash
# Recopie le contrat d'API qui fait foi (dépôt pawrise-doc cloné à côté de ce repo).
set -euo pipefail
cd "$(dirname "$0")/.."
DOC="${PAWRISE_DOC:-../pawrise-doc}"
git -C "$DOC" pull -q --ff-only || echo "pawrise-doc : pull impossible, copie de la version locale"
SHA=$(git -C "$DOC" rev-parse --short HEAD)
{
  echo "# Copie de pawrise-doc/contrat-interface/openapi.yaml (commit $SHA), contrat qui fait foi pour l'app."
  echo "# Ne pas modifier ici : ./scripts/sync-contract.sh recopie la dernière version."
  echo "#"
  echo "# Mock (compose.yaml) : forcer une réponse avec l'en-tête « Prefer: code=409 » ou « Prefer: example=<nom> »."
  echo "# Les routes Bearer répondent 401 sans en-tête Authorization (n'importe quel jeton passe)."
  cat "$DOC/contrat-interface/openapi.yaml"
} > api/openapi.yaml
echo "api/openapi.yaml ← pawrise-doc@$SHA"
