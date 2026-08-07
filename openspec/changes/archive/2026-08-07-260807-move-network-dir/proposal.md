## Why

The network package is located directly beneath the application package. Moving it to the production module structure clarifies ownership and aligns it with the project's feature layout.

## What Changes

- Move the `com.creepersan.memorialday.network` production package to `com.creepersan.memorialday.module.network`.
- Keep network tests under test sources and update their package path.
- Update package declarations and affected imports to use the new production package path.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a source-layout refactor with no behavioral changes.

## Impact

- Affects network source files and their references.
- Does not change network APIs, dependencies, or runtime behavior.
