# Economy

HeadDB can charge players for heads through Vault and a Vault-compatible economy plugin.

Economy is configured in:

```text
plugins/HeadDB/economy.yml
```

## Requirements

| Requirement | Notes |
| --- | --- |
| Vault | Required when economy support is enabled. |
| Economy provider | Any compatible economy plugin supported by Vault. |
| Economy enabled | `enabled: true` in `economy.yml`. |

If economy is disabled, HeadDB does not charge players for heads.

## Basic configuration

```yaml
enabled: false
provider: vault

prices:
  any-head: 0.0
  player-heads: 0.0
  categories: {}
  custom-categories: {}
  heads: {}
```

## Pricing priority

HeadDB resolves prices from most specific to least specific.

| Priority | Source | Example |
| --- | --- | --- |
| 1 | Per-head price | `heads.123: 50.0` |
| 2 | Custom category price | `custom-categories.event: 25.0` |
| 3 | Remote category price | `categories.blocks: 10.0` |
| 4 | Player-head price | `player-heads: 15.0` |
| 5 | Fallback price | `any-head: 5.0` |

## Examples

Flat price:

```yaml
prices:
  any-head: 10.0
  player-heads: 10.0
```

Category prices:

```yaml
prices:
  any-head: 5.0
  categories:
    blocks: 2.0
    decoration: 8.0
```

Per-head prices:

```yaml
prices:
  heads:
    '123': 100.0
    'custom:dragon': 250.0
    'player:f16df3ef-06b8-443e-9166-fba6689585b4': 25.0
```

Quote keys that contain special characters.

## Troubleshooting

| Problem | Likely cause | Fix |
| --- | --- | --- |
| Heads are free | Economy disabled or price is `0.0`. | Enable economy and configure prices. |
| Economy unavailable | Vault or provider missing. | Install Vault and a compatible economy plugin. |
| Player cannot buy | Insufficient balance or permission issue. | Check balance, permissions, and console. |
| Wrong price | A more specific price overrides fallback. | Check per-head, custom-category, and category entries. |
