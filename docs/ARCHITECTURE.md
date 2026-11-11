# Software architecture

This file defines **technical decisions and module boundaries**, not feature requirements or QA checklists.

## Dependency direction

```text
watch-domain    (Java only: validation and filenames)
      ^
watch-adb       (subprocess transport, parsing, pairing, scanning)
      ^
watch-core      (Spring service orchestration)
      ^
watch-desktop   (JavaFX user interface and Spring Boot startup)
```

Direct dependency edges may also skip an intermediate module where a consumer needs a domain type. Spring Boot starts with `WebApplicationType.NONE`; no local HTTP port, database or web server is needed.

## Data flow

Select a destination ADB serial. Ensure that exact serial is connected and authorized. Create the watch folder `/sdcard/DCIM/Camera`, generate a collision-resistant ASCII filename, push via `adb -s SERIAL`, then request `android.intent.action.MEDIA_SCANNER_SCAN_FILE` for the pushed file URI. Emit a separate result for each file; a scan failure must not be confused with a copy failure. A `TransferUpdate` carries the generated remote filename only once upload is confirmed. A retry of `FAILED` performs a new upload; a retry of `COPIED_SCAN_FAILED` reuses that remote filename for **scan-only** retry. A retry must target the same watch serial as the original batch. Uncertain ADB timeouts cannot prove whether a partially copied remote file exists.

The broadcast is a legacy Android API known to work on the reference Watch 4 Classic. Keep it encapsulated in the ADB adapter for future replacement.

## Security decisions

- Use `ProcessBuilder(List<String>)`, never interpolate user input into a command shell.
- Send pairing codes through stdin; never store them or include them in a process command line.
- Validate local files; accept only generated remote filenames.
- Never copy to an implicitly selected device or overwrite a known existing remote name.
- No analytics, cloud access or destructive operations.
- Run blocking ADB calls off JavaFX Application Thread.

## Packaging

Plan for `jpackage` with bundled Java runtime. Keep ADB external until platform-tools redistribution licensing is reviewed.
