#!/usr/bin/env bash
set -euo pipefail

# Exercise module selection with a fake Maven, never compiling Java.
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
sandbox="$(mktemp -d)"
trap 'rm -rf "$sandbox"' EXIT
export WGS_CI_MVN_LOG="$sandbox/mvn-calls"
cat > "$sandbox/mvn" <<'FAKE_MAVEN'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$WGS_CI_MVN_LOG"
FAKE_MAVEN
chmod +x "$sandbox/mvn"
export PATH="$sandbox:$PATH"

run_case() {
    : > "$WGS_CI_MVN_LOG"
    env FULL_REACTOR=false DOMAIN_CHANGED=false ADB_CHANGED=false CORE_CHANGED=false DESKTOP_CHANGED=false "$@" \
        bash "$here/verify-changed.sh"
}

assert_selected() {
    local modules="$1"
    [[ "$(wc -l < "$WGS_CI_MVN_LOG")" -eq 2 ]]
    grep -Fx -- "-B -ntp -pl $modules -am -amd -DskipTests install" "$WGS_CI_MVN_LOG"
    grep -Fx -- "-B -ntp -pl $modules test" "$WGS_CI_MVN_LOG"
}

run_case DOMAIN_CHANGED=true
assert_selected watch-domain
run_case ADB_CHANGED=true DESKTOP_CHANGED=true
assert_selected watch-adb,watch-desktop
run_case CORE_CHANGED=true
assert_selected watch-core
run_case DESKTOP_CHANGED=true
assert_selected watch-desktop
run_case FULL_REACTOR=true
[[ "$(wc -l < "$WGS_CI_MVN_LOG")" -eq 1 ]]
grep -Fx -- '-B -ntp verify' "$WGS_CI_MVN_LOG"
run_case
[[ ! -s "$WGS_CI_MVN_LOG" ]]
echo "Selective CI module-selection checks passed"
