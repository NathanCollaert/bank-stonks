# Stockpile

*"I bought that ages ago… I wonder what it's worth now?"*

Stockpile is a RuneLite plugin that remembers what you paid for the items you buy on the
Grand Exchange and shows you, at a glance, how much **profit or loss** you're sitting on for
the ones you're still holding in your bank.

## The vision

This isn't a flipping tool and it isn't here to tell you when to sell. It exists for one
simple, satisfying reason: **curiosity**. You accumulate stuff over time — supplies you
stockpiled, gear you invested in, that pile of an item you were convinced would moon — and it
just sits in your bank. Stockpile quietly keeps score, so whenever you're curious you can see
how those old purchases have actually panned out.

It's deliberately **passive and honest**:

- It values your holdings at the current **wiki price** (what the item is worth *now*), not
  what you'd net after fees — because you're asking "how did my buy do?", not "what if I dump
  it today?".
- It never nags, never suggests, never sells. It just shows the number.

## How it works (in plain terms)

- Every time you **buy** something on the GE, Stockpile records the quantity and the *actual*
  average price you paid (partial fills included).
- Your **held quantity comes from your bank** — not from guessing. So selling, dropping,
  alching or trading is reflected automatically the next time your bank updates.
- Profit/loss for an item = **held quantity × (current wiki price − your average buy price)**.
- Held quantity is capped at what you actually *bought* (`min(bought, in bank)`), so free
  drops of an item you also bought never get counted as "profit".
- Charged/uncharged and dose variants are matched together, so buying an item uncharged,
  charging it, and banking the charged version still tracks correctly.

## Features

**Sidebar panel**
- A running **total profit/loss** across everything you hold, coloured green/red.
- One row per held item: icon, quantity, your average buy price, the P/L (and optional %),
  and how long you've **held** it ("held 2y 3mo").
- A **search box** to filter the list by name.
- A collapsible **"Add item manually"** form.

**Add items manually**
- For things you bought *before* installing (Stockpile can't know their price otherwise):
  enter the item, quantity, and the price you paid.
- Optional **"Held since"** field so old purchases show their real age. Accepts dates
  (`2023-03-15`, `Jan 2024`, `2022`) or relative ages (`2y`, `6mo`, `2y 3mo`, `30d ago`).
  Leave it blank to use now.

**Right-click a row**
- **Untrack** — deletes that item's data. It disappears from the list, and reappears fresh
  (with up-to-date buy price) if you buy it again. A one-time clean-up.
- **Block** — hides the item permanently. It keeps tracking in the background but is never
  shown. Managed via the block list in settings.

**In the bank**
- Hover any tracked item to see a tooltip: `quantity @ avg price`, how long held, and P/L.
- An optional draggable overlay shows your **total portfolio P/L** while the bank is open.

**Settings**
- **Block list** — comma-separated item names never shown, with `*` wildcards
  (e.g. `prayer potion*` blocks every dose).
- Toggle the **%**, hide items you no longer hold, choose the **sort order**, pick your
  profit/loss **colours**, toggle the bank overlays, and optionally value holdings **after
  GE tax** if you prefer.

## Building & running

```
./gradlew.bat run          # launch RuneLite in dev mode with the plugin loaded
./gradlew.bat shadowJar    # build a runnable fat jar
```

## Good to know

- Stockpile can only record buys **from the moment it's installed**; the game doesn't expose
  your old purchase history. Use **manual add** (with a Held-since date) to backfill older
  holdings.
- Category matching for charged/dose variants relies on RuneLite's item variation data, which
  covers the common cases but not every single item.

## Architecture (for the curious)

| Piece | Role |
|---|---|
| `StockpilePlugin` | Wiring, events, refresh loop |
| `StockpileConfig` | Settings (block list, colours, sort, toggles) |
| `PortfolioManager` | Per-account state, persistence, P/L computation |
| `GrandExchangeBuyTracker` | Records buy cost basis from GE offers (dedup-safe) |
| `BankTracker` | Snapshots bank quantities |
| `BankOverlay` / `BankTotalOverlay` | Hover tooltip + total on the bank |
| `BlockLists` | Wildcard block-list matching (cached) |
| `Format` | Shared number/age formatting |
| `ui/StockpilePanel` | The sidebar |
| `model/*` | `TrackedItem`, `SlotState`, `PortfolioData`, `PortfolioRow` |
