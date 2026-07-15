# cloud-itonami-isco-8331

Open Occupation Blueprint for **ISCO-08 8331**: Bus and Tram Drivers.

This repository designs a forkable OSS business for an independent passenger transport practice: a vehicle-inspection and boarding-log robot manages pre-trip inspections and passenger counts under a governor-gated actor, so the practice keeps its own trip records instead of renting a closed fleet-management SaaS.

**Maturity: `:implemented`.** `src/transport/` implements the
`PassengerTransportActor` as a `langgraph.graph/state-graph`
(`transport.actor`) wired to a `Transport Advisor` (`transport.advisor`)
and an independent `PassengerTransportGovernor` (`transport.governor`),
following the itonami actor pattern (ADR-2607011000): `:intake -> :advise
-> :govern -> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. 14 tests / 29 assertions
green (`clojure -M:test`). HARD invariants (always hold, never
overridable): client provenance, no-actuation (`:effect` must be
`:propose`), a registered vehicle basis for any route-dispatch
proposal, the proposed passenger count not exceeding the vehicle's
registered passenger-capacity ceiling (dispatching beyond it is an
overcrowded dispatch, not efficient scheduling), and a passed pre-trip
inspection before any route can be dispatched (dispatching without one
is an unsafe departure, not efficient service). Always-escalate ops
(human sign-off regardless of confidence, mapping this repo's Trust
Controls in [`docs/business-model.md`](docs/business-model.md)):
`:approve-over-capacity-dispatch` and
`:approve-emergency-route-deviation`.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a vehicle-inspection and boarding-log robot performs pre-trip inspection checklist logging and passenger-count tracking under an actor that proposes
actions and an independent **Passenger Transport Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
route dispatch above the vehicle's registered passenger-capacity ceiling) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
route request + vehicle inspection + passenger manifest
        |
        v
Transport Advisor -> Passenger Transport Governor -> dispatch route/approve, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8331`). Required capabilities:

- :robotics
- :telemetry
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
