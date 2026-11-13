#!/usr/bin/env bash
set -euo pipefail

if [[ "${FULL_REACTOR:-false}" == "true" ]]; then
  echo "Manual full reactor verification"
  exec mvn -B -ntp verify
fi

modules=()
[[ "${DOMAIN_CHANGED:-false}" == "true" ]] && modules+=(watch-domain)
[[ "${ADB_CHANGED:-false}" == "true" ]] && modules+=(watch-adb)
[[ "${CORE_CHANGED:-false}" == "true" ]] && modules+=(watch-core)
[[ "${DESKTOP_CHANGED:-false}" == "true" ]] && modules+=(watch-desktop)

if (( ${#modules[@]} == 0 )); then
  echo "No modified Maven modules"
  exit 0
fi

selected=$(IFS=,; echo "${modules[*]}")
echo "Compiling changed modules, required upstream dependencies and downstream consumers: $selected"
# -am includes prerequisites; -amd adds dependents. Compile downstream test
# sources, but run unit tests only for the modules that actually changed.
mvn -B -ntp -pl "$selected" -am -amd -DskipTests install
echo "Testing only changed modules: $selected"
exec mvn -B -ntp -pl "$selected" test
