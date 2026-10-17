# Elements: Utilities changelog

## Unreleased
- Known loot bag mods are detected at start and their items count as loot bags (`loot_bag_detection` switch in `features.toml`). The server log lists the ones found.
- Item cleanup is manual by default: staff run `/utilities cleanup` (node `utilities.cleanup`).
- New `mode` setting under `[item_cleanup]`: `"manual"` or `"timer"` for the old automatic sweep.

## 1.0.0
- First release.
