# GE Portfolio Tracker

A RuneLite plugin that tracks the items you buy on the Grand Exchange and shows live
**profit / loss** for the amount you still hold, comparing your average buy price against
the current wiki (GE) price.

## How it works

- **Only the average buy price is tracked.** Every GE buy fill adds to a per-item running
  total (`totalSpent / totalBought`). Held quantity is **not** stored — it is read live from
  your bank, so selling, dropping, alching or trading is reflected automatically.
- **Valued quantity = `min(totalBought, bankQuantity)`.** This caps out items obtained
  outside the GE (drops, etc.) so they are never counted as profit, and drops sold-off stock
  out of the total.
- **Profit / loss** per item = `heldQty × (currentWikiPrice − averageBuyPrice)`, using the
  raw wiki price from RuneLite's `ItemManager`.
- All state is persisted per account in the RuneLite config.

## Architecture

| Class | Responsibility |
|---|---|
| `GePortfolioPlugin` | Wiring, event subscriptions, 30s refresh timer |
| `GePortfolioConfig` | Toggles (hide un-held items, show %) |
| `GrandExchangeBuyTracker` | Diffs `GrandExchangeOfferChanged` → records buy cost basis (dedup-safe across logins) |
| `BankTracker` | Snapshots the bank on `ItemContainerChanged` → cached quantities |
| `PortfolioManager` | Per-account state, load/save, row computation |
| `ui/GePortfolioPanel` | Sidebar: total P/L header + one row per held item |
| `model/*` | `TrackedItem`, `SlotState`, `PortfolioData`, `PortfolioRow` |

## Build

```
./gradlew.bat compileJava        # compile
./gradlew.bat shadowJar          # runnable fat jar (launches the client with the plugin)
```

Run `GePortfolioPluginTest.main` to start RuneLite with the plugin loaded for local testing.

## Roadmap

- **Phase 1 (done):** GE buy capture, bank-derived holdings, per-account persistence, sidebar panel.
- **Phase 2:** Bank overlay (per-item + total P/L drawn on the bank interface), manual
  add/edit entries, more config (colors, sort order).
- **Phase 3 (optional):** sell tracking toggle, GE-tax-aware value, per-lot history.
