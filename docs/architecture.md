# Architecture

## Product shape

NodeStatus is intended to be a panel-monitoring platform with a thin Android client.
The app is primarily a configuration center. The operational surfaces are:

- home-screen widgets
- notifications
- lock-screen-visible summaries

## Core principles

1. Snapshots are the system boundary.
   Network collection writes normalized snapshots first. UI surfaces only read cached snapshots.
2. Providers are replaceable.
   A provider family such as `VirtFusion` is not the same thing as a site profile such as `vmvm`.
3. Capability support is explicit.
   A panel may expose traffic data but not CPU or memory usage. Unsupported must stay distinct from zero.
4. Community extension points are declarative first.
   Rule packs, fixtures, and mappings are preferred over runtime-loaded executable code.

## Module map

- `core:model`
  Shared monitoring primitives such as snapshots, metrics, and provider families.
- `core:storage`
  Contracts for persisting and retrieving normalized snapshots.
- `core:widget`
  Contracts for projecting snapshots into compact widget-friendly summaries.
- `adapter:engine`
  Rule-pack and adapter contracts.
- `adapter:virtfusion`
  First provider-family blueprint.
- `adapter:testkit`
  Fixture descriptors and future adapter test utilities.

## Planned flow

1. Acquire or restore auth state.
2. Resolve provider capabilities.
3. Discover resources.
4. Collect metrics.
5. Normalize into `ResourceSnapshot`.
6. Persist snapshots.
7. Render widgets and notifications from cached projections.

## Deliberate non-goals for the current phase

- Dynamic execution of third-party code on Android devices.
- Real-time widget polling.
- Provider-specific logic mixed into UI screens.
