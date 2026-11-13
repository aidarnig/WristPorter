# Windows SmartScreen и подпись издателя / Windows security

## Русский

На данный момент у WristPorter **нет доверенной цифровой подписи Authenticode**. Microsoft Defender SmartScreen может показать красное предупреждение **«Система Windows защитила ваш компьютер»** и издателя «Неизвестный издатель».

SmartScreen оценивает репутацию самого файла и его издателя. После изменения EXE может потребоваться накопление репутации заново, поэтому предыдущая версия могла не вызывать такого предупреждения. **Мы не отключаем и не обходим защиту Windows программно.**

Скачивайте только из [официального GitHub Releases](https://github.com/aidarnig/WristPorter/releases). Сравнивайте SHA-256 ZIP с файлом контрольной суммы. Это проверяет совпадение опубликованного файла, но не подтверждает добросовестность издателя и не является антивирусной экспертизой.

Для устранения проблемы в будущих релизах потребуется цифровая подпись доверенным сертификатом, хранение ключа вне репозитория, проверка подписи в CI и репутация издателя. **Самоподписанный сертификат не решает эту проблему.** Даже доверенная подпись не гарантирует немедленного исчезновения SmartScreen.

## English

WristPorter is currently **unsigned by a trusted Authenticode publisher**. Microsoft Defender SmartScreen may display an **Unknown publisher** warning on a new EXE, even if an older version did not.

We do not bypass Windows security controls. Download only from [official GitHub Releases](https://github.com/aidarnig/WristPorter/releases) and verify the ZIP SHA-256 against the published checksum. A checksum verifies byte integrity relative to the release, not publisher trust or absence of malware.

Future releases should use a trusted signing identity with protected credentials, verified signing in CI, and time to build publisher reputation. Self-signed certificates are not sufficient; a valid signature alone does not guarantee zero warnings.

Microsoft documentation: [SmartScreen reputation](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation).

## Planned local browser uploads / Безопасность будущего Web Upload

**NOT IMPLEMENTED IN v1.1.2.** A future single-Windows-app Web Upload mode will permit an iPhone browser to submit selected photos to Windows on the private LAN for transfer to the watch over existing ADB. It is *not* a cloud app or installed iOS companion.

- **HTTP over Wi-Fi is plaintext:** anybody who can intercept traffic on that LAN may read or change transmitted images, PINs or sessions. A pairing code restricts use but does **not** encrypt traffic. W13 must evaluate authenticated HTTPS/trust onboarding; without it, user consent/trusted-private-network restriction and explicit limitation documentation are mandatory release gates. Never claim public/hotel Wi-Fi is safe.
- Listener is **OFF by default**, user-initiated, bound to selected private interface only, time-limited and stopped on desktop shutdown. Do not expose to the public Internet or bypass Windows Firewall protections.
- Short-lived authorization, code attempt limits, strict Host/Origin/CSRF policy, revocable session, no permissive CORS or remotely available ADB/admin endpoints.
- Only JPEG/PNG after server-side type/signature/size/pixel validation; limits on memory/temporary storage/requests, server-generated names, no path traversal or executable uploads, private temporary staging outside web root, cleanup on success/cancel/crash recovery.
- Pin the selected ADB watch to the session; never permit remote watch changes or APK installs. Do not publish file content, phone IPs, tokens, serials or original image metadata in logs.
- Separate adversarial tests and actual private-LAN iPhone → Windows → watch confirmation precede any stable release.

Подробности: [LOCAL_WEB_PORTAL.md](LOCAL_WEB_PORTAL.md), [ADR-0004](adr/0004-one-desktop-optional-lan-portal.md), [W13–W16](ROADMAP.md). Текущий Desktop без включённого Web Upload не должен слушать локальную сеть.
