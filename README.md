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
  average price you paid (partial fills included).
- Your **held quantity comes from your bank**, not from guessing. So selling, dropping,
  alching or trading is reflected automatically the next time your bank updates.
- Profit/loss for an item = **held quantity × (current wiki price − your average buy price)**.
- Held quantity is capped at what you actually *bought* (`min(bought, in bank)`), so free
  drops of an item you also bought never get counted as "profit".
- Charged/uncharged and dose variants are matched together, so buying an item uncharged,
  charging it, and banking the charged version still tracks correctly.

## Good to know

- Bank Stonks can only record buys **from the moment it's installed**; the game doesn't expose
  your old purchase history. Use **manual add** (with a Held-since date) to backfill older
  holdings.
- Category matching for charged/dose variants relies on RuneLite's item variation data, which
  covers the common cases but not every single item.

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