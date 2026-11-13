# ADR-0002 — Direct BLE ANCS/AMS with explicit safety and privacy boundaries

**Status:** Accepted as proposed architecture; implementation details provisional pending W0, 2026-11-13.

## Context

A watch without Galaxy Wearable support on iPhone should be able to receive **Apple-provided** notifications and media state. ANCS and AMS are documented but their availability can vary by iOS, app and firmware. Long-lived BLE connections affect battery and sensitive information.

## Decision

- Use only native documented ANCS, AMS and **physically observed** optional Bluetooth GATT services, not private iOS APIs or unverified protocol hacks.
- Pair/bond only after user initiative/confirmation. Accept only explicitly selected known device, never scan-first or guessed hardware identity.
- One serialized GATT operation queue per connection, timeouts/cancellation, generation-scoped callbacks and UIDs, bounded memory/packet sizes. Handle Service Changed.
- Source text and app metadata are **RAM-only by default** and excluded from logs; Android notification/lockscreen controls honor user privacy settings.
- Show ANCS positive/negative actions by **provided flag/label**; never treat them as arbitrary replies. AMS remote commands only when current player advertises them.
- Use OS-sanctioned lifecycle (evaluate connectedDevice FGS versus companion-presence strategy); no infinite scans, keepalives or aggressive retry loops without evidence.
- Instrument non-sensitive diagnostics and perform comparative battery measurements; no invented battery-life promises.
- Do not introduce Internet permission or cloud account into first Bridge implementation.

## Consequences and open choices

- Stronger privacy and reliability at cost of more state-machine tests and conservative optional features.
- Actual minimum API, background service approach, bond flow and GATT role constraints remain **open** until W0/W7 and should receive follow-up ADR where substantive.
- Later opt-in persistent notification history or separate app data transfer requires a new privacy decision.

## Primary protocols

[Apple ANCS specification](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleNotificationCenterServiceSpecification/Specification/Specification.html)  
[Apple AMS specification](https://developer.apple.com/library/archive/documentation/CoreBluetooth/Reference/AppleMediaService_Reference/Specification/Specification.html)  
[Android BLE background guidance](https://developer.android.com/develop/connectivity/bluetooth/ble/background)

[Capability matrix](../BRIDGE_CAPABILITIES.md) · [Architecture](../BRIDGE_ARCHITECTURE.md).
