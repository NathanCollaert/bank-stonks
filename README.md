# Bank Stonks

*"I bought that ages ago… I wonder what it's worth now?"*

Bank Stonks is a RuneLite plugin that remembers what you paid for the items you buy on the
Grand Exchange and shows you, at a glance, how much **profit or loss** you're sitting on for
the ones you're still holding in your bank.

## The vision

This isn't a flipping tool and it isn't here to tell you when to sell. It exists for one
simple, satisfying reason: **curiosity**. You accumulate stuff over time, supplies you
stockpiled, gear you invested in, that pile of an item you were convinced would moon. And it
just sits in your bank. Bank Stonks quietly keeps score, so whenever you're curious you can see
how those old purchases have actually panned out.

## How it works

- Every time you **buy** something on the GE, Bank Stonks records the quantity and the *actual*
  price you paid (partial fills included).
- Your **held quantity is whatever you currently have** in your bank, inventory and worn gear.
  Selling, dropping, alching or using items is reflected as your holdings change.
- Profit/loss for an item = **held quantity × (current wiki price − what you paid)**.
- Held quantity is capped at what you actually *bought* (`min(bought, held)`), so free drops of
  an item you also bought never get counted as "profit".
- Charged/uncharged and dose variants are matched together, so buying an item uncharged,
  charging it, and banking the charged version still tracks correctly.

## Limitations

A few things to expect, so nothing looks like a bug:

- **Items appear and disappear as your holdings change.** The list only shows what you're
  currently holding. Sell an item, use it up, drop it, keep it in your POH, store it at a boss
  reclaim, or lose it on death, and it drops off the list, then reappears the moment it's back
  in your bank or inventory.
- **Your buy history is never deleted.** Because an item can leave and come back (a withdrawal,
  a death, boss/POH storage), nothing is removed when it disappears, so a returning item keeps
  its original cost and "held since".
- **Holdings are valued at your most recent buys.** If you've bought an item at different times,
  what you hold is priced from the latest purchases first. Sell everything and rebuy later and
  it shows the *new* price with a fresh "held since", not a blend with the old ones.
- **Only buys are tracked, from install onward.** The game doesn't expose your past purchases,
  so use **manual add** (with a Held-since date) to backfill items you bought before installing.
- **Consumed supplies linger hidden.** If you buy food/potions/runes and use them up without
  selling, the record stays but is hidden while you hold none; buying more shows the new price.
- **Variant matching isn't exhaustive.** Charged/uncharged and dose variants are grouped via
  RuneLite's item-variation data, which covers common cases but not every item.
- **Only the 40 most recent buys are kept as separate history rows.** Beyond that, the oldest
  buys are merged into a single rolled-up entry to keep stored data small. No quantity or spend
  is ever lost, so totals and valuation stay exact; only the per-buy history is condensed.

## Presets

Paste any of these into `Settings > Bank Stonks > Block list` to hide a whole category. A
wildcard entry (one with `*`) matches any item whose name contains that text; an entry with no
`*` matches the exact name.

Ores, bars, gems and mining materials ([Mining](https://oldschool.runescape.wiki/w/Mining#Mineable_items),
[Bar](https://oldschool.runescape.wiki/w/Bar), [Gem](https://oldschool.runescape.wiki/w/Gem)):

```
* ore, * bar, coal, clay, soft clay, limestone, sandstone, granite, amethyst, rune essence, pure essence, volcanic ash, daeyalt shard, uncut*, sapphire, emerald, ruby, diamond, dragonstone, opal, jade, red topaz, onyx, zenyte
```

Crafting materials ([wiki](https://oldschool.runescape.wiki/w/Crafting)):

```
*dragonhide*, *dragon leather*, leather, hard leather, cowhide, snakeskin, snake hide, yak-hide, cured yak-hide, wool, ball of wool, flax, bow string, sinew, thread, needle, molten glass, bucket of sand, soda ash, seaweed, giant seaweed, * nails, bark
```

Herbs, seeds and compost ([Herb](https://oldschool.runescape.wiki/w/Herb),
[Seeds](https://oldschool.runescape.wiki/w/Seeds),
[Compost](https://oldschool.runescape.wiki/w/Compost_%28disambiguation%29)):

```
grimy *, guam leaf, marrentill, tarromin, harralander, ranarr weed, toadflax, irit leaf, avantoe, kwuarm, huasca, snapdragon, cadantine, lantadyme, dwarf weed, torstol, * seed, acorn, * spore, *compost*, saltpetre, sulphurous fertiliser
```

Logs and planks ([wiki](https://oldschool.runescape.wiki/w/Logs)):

```
* logs, logs, * plank, plank
```

Remains ([wiki](https://oldschool.runescape.wiki/w/Remains)):

```
* bone, bones, * ashes, ashes, * remains
```

Potions ([wiki](https://oldschool.runescape.wiki/w/Potion)):

```
*potion*, *brew*, *antipoison*, *antidote*, *anti-venom*, *antifire*, *stamina*, *serum*, *mix*, super attack*, super strength*, super defence*, super energy*, super restore*, overload*, guthix rest*, guthix balance*, relicym*, menaphite remedy*, prayer enhance*, prayer regeneration*, weapon poison*
```

Runes and teleports ([Runes](https://oldschool.runescape.wiki/w/Runes),
[Transportation](https://oldschool.runescape.wiki/w/Transportation)):

```
* rune, *teleport*
```

Food ([wiki](https://oldschool.runescape.wiki/w/Food/All_food)):

```
raw *, uncooked *, cooked *, roast *, premade *, * pie, * pizza, * crunchies, * batta, * potato, * kebab, * sandwich, * sq'irkjuice, * banana, monkey *, shrimps, anchovies, sardine, salmon, trout, giant carp, cod, herring, pike, mackerel, tuna, bass, swordfish, lobster, shark, manta ray, sea turtle, monkfish, fresh monkfish, anglerfish, dark crab, lava eel, cave eel, rainbow fish, loach, eel sushi, roe, caviar, paddlefish, crystal paddlefish, corrupted paddlefish, haddock, halibut, yellowfin, bluefin, giant krill, banana, toad's legs, tangled toad's legs, king worm, worm hole, veg ball, chocolate bomb, stuffed snake, field ration, bread, cake, chocolate cake, stew, spicy stew, banana stew, curry, choc-ice, baguette, roll, jug of wine, bottle of wine, peach, dragonfruit, watermelon, watermelon slice, papaya fruit, purple sweets, edible seaweed, blighted anglerfish, blighted manta ray, blighted karambwan
```

Ammunition ([wiki](https://oldschool.runescape.wiki/w/Ammunition)):

```
* arrow, * bolts, * dart, * javelin, * knife, * throwing axe, *cannonball*, *chinchompa*
```

### All of them at once:

```
* ore, coal, clay, soft clay, limestone, sandstone, granite, amethyst, rune essence, pure essence, volcanic ash, daeyalt shard, uncut*, sapphire, emerald, ruby, diamond, dragonstone, opal, jade, red topaz, onyx, zenyte, * bar, *dragonhide*, *dragon leather*, leather, hard leather, cowhide, snakeskin, snake hide, yak-hide, cured yak-hide, wool, ball of wool, flax, bow string, sinew, thread, needle, molten glass, bucket of sand, soda ash, seaweed, giant seaweed, * nails, bark, grimy *, guam leaf, marrentill, tarromin, harralander, ranarr weed, toadflax, irit leaf, avantoe, kwuarm, huasca, snapdragon, cadantine, lantadyme, dwarf weed, torstol, * seed, acorn, * spore, *compost*, saltpetre, sulphurous fertiliser, * logs, logs, * plank, plank, * bone, bones, * ashes, ashes, * remains, *potion*, *brew*, *antipoison*, *antidote*, *anti-venom*, *antifire*, *stamina*, *serum*, *mix*, super attack*, super strength*, super defence*, super energy*, super restore*, overload*, guthix rest*, guthix balance*, relicym*, menaphite remedy*, prayer enhance*, prayer regeneration*, weapon poison*, * rune, *teleport*, raw *, uncooked *, cooked *, roast *, premade *, * pie, * pizza, * crunchies, * batta, * potato, * kebab, * sandwich, * sq'irkjuice, * banana, monkey *, shrimps, anchovies, sardine, salmon, trout, giant carp, cod, herring, pike, mackerel, tuna, bass, swordfish, lobster, shark, manta ray, sea turtle, monkfish, fresh monkfish, anglerfish, dark crab, lava eel, cave eel, rainbow fish, loach, eel sushi, roe, caviar, paddlefish, crystal paddlefish, corrupted paddlefish, haddock, halibut, yellowfin, bluefin, giant krill, banana, toad's legs, tangled toad's legs, king worm, worm hole, veg ball, chocolate bomb, stuffed snake, field ration, bread, cake, chocolate cake, stew, spicy stew, banana stew, curry, choc-ice, baguette, roll, jug of wine, bottle of wine, peach, dragonfruit, watermelon, watermelon slice, papaya fruit, purple sweets, edible seaweed, blighted anglerfish, blighted manta ray, blighted karambwan, * arrow, * bolts, * dart, * javelin, * knife, * throwing axe, *cannonball*, *chinchompa*
```