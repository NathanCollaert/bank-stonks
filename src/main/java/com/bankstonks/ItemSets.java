package com.bankstonks;

import java.util.HashMap;
import java.util.Map;

/**
 * Sets are usually unpacked after buying, so their pieces must still count as the set. RuneLite has
 * no set data; this table follows the OSRS Wiki's "Item set" page.
 */
final class ItemSets
{
	private ItemSets()
	{
	}

	private static final Map<Integer, int[]> COMPONENTS = new HashMap<>();

	static
	{
		COMPONENTS.put(13020, new int[]{2613, 2607, 2609, 2611}); // Adamant gold-trimmed set (lg)
		COMPONENTS.put(13022, new int[]{2613, 2607, 3475, 2611}); // Adamant gold-trimmed set (sk)
		COMPONENTS.put(13012, new int[]{1161, 1123, 1073, 1199}); // Adamant set (lg)
		COMPONENTS.put(13014, new int[]{1161, 1123, 1091, 1199}); // Adamant set (sk)
		COMPONENTS.put(13016, new int[]{2605, 2599, 2601, 2603}); // Adamant trimmed set (lg)
		COMPONENTS.put(13018, new int[]{2605, 2599, 3474, 2603}); // Adamant trimmed set (sk)
		COMPONENTS.put(12881, new int[]{4708, 4712, 4714, 4710}); // Ahrim's armour set
		COMPONENTS.put(21049, new int[]{21018, 21021, 21024}); // Ancestral robes set
		COMPONENTS.put(13171, new int[]{12496, 12492, 12494, 12490}); // Ancient dragonhide set
		COMPONENTS.put(13060, new int[]{12466, 12460, 12462, 12468}); // Ancient rune armour set (lg)
		COMPONENTS.put(13062, new int[]{12466, 12460, 12464, 12468}); // Ancient rune armour set (sk)
		COMPONENTS.put(13169, new int[]{12512, 12508, 12510, 12506}); // Armadyl dragonhide set
		COMPONENTS.put(13052, new int[]{12476, 12470, 12472, 12478}); // Armadyl rune armour set (lg)
		COMPONENTS.put(13054, new int[]{12476, 12470, 12474, 12478}); // Armadyl rune armour set (sk)
		COMPONENTS.put(13167, new int[]{12504, 12500, 12502, 12498}); // Bandos dragonhide set
		COMPONENTS.put(13056, new int[]{12486, 12480, 12482, 12488}); // Bandos rune armour set (lg)
		COMPONENTS.put(13058, new int[]{12486, 12480, 12484, 12488}); // Bandos rune armour set (sk)
		COMPONENTS.put(12871, new int[]{2503, 2497, 2491}); // Black dragonhide set
		COMPONENTS.put(12996, new int[]{2595, 2591, 2593, 2597}); // Black gold-trimmed set (lg)
		COMPONENTS.put(12998, new int[]{2595, 2591, 3473, 2597}); // Black gold-trimmed set (sk)
		COMPONENTS.put(12988, new int[]{1165, 1125, 1077, 1195}); // Black set (lg)
		COMPONENTS.put(12990, new int[]{1165, 1125, 1089, 1195}); // Black set (sk)
		COMPONENTS.put(12992, new int[]{2587, 2583, 2585, 2589}); // Black trimmed set (lg)
		COMPONENTS.put(12994, new int[]{2587, 2583, 3472, 2589}); // Black trimmed set (sk)
		COMPONENTS.put(31136, new int[]{29028, 29022, 29025, 28997}); // Blood moon armour set
		COMPONENTS.put(31163, new int[]{25413, 25404, 25416, 25407, 25410}); // Bloodbark armour set
		COMPONENTS.put(12867, new int[]{2499, 2493, 2487}); // Blue dragonhide set
		COMPONENTS.put(31139, new int[]{29019, 29013, 29016, 28988}); // Blue moon armour set
		COMPONENTS.put(13153, new int[]{3835, 3836, 3837, 3838}); // Book of balance page set
		COMPONENTS.put(13159, new int[]{12621, 12622, 12623, 12624}); // Book of darkness page set
		COMPONENTS.put(13157, new int[]{12617, 12618, 12619, 12620}); // Book of law page set
		COMPONENTS.put(13155, new int[]{12613, 12614, 12615, 12616}); // Book of war page set
		COMPONENTS.put(12968, new int[]{12211, 12205, 12207, 12213}); // Bronze gold-trimmed set (lg)
		COMPONENTS.put(12970, new int[]{12211, 12205, 12209, 12213}); // Bronze gold-trimmed set (sk)
		COMPONENTS.put(12960, new int[]{1155, 1117, 1075, 1189}); // Bronze set (lg)
		COMPONENTS.put(12962, new int[]{1155, 1117, 1087, 1189}); // Bronze set (sk)
		COMPONENTS.put(12964, new int[]{12221, 12215, 12217, 12223}); // Bronze trimmed set (lg)
		COMPONENTS.put(12966, new int[]{12221, 12215, 12219, 12223}); // Bronze trimmed set (sk)
		COMPONENTS.put(13064, new int[]{2428, 113, 2432}); // Combat potion set
		COMPONENTS.put(24333, new int[]{24288, 24291, 24294}); // Dagon'hai robes set
		COMPONENTS.put(12877, new int[]{4716, 4720, 4722, 4718}); // Dharok's armour set
		COMPONENTS.put(21882, new int[]{11335, 21892, 4087, 21895}); // Dragon armour set (lg)
		COMPONENTS.put(21885, new int[]{11335, 21892, 4585, 21895}); // Dragon armour set (sk)
		COMPONENTS.put(23667, new int[]{24034, 24037, 24040, 24046, 24043}); // Dragonstone armour set
		COMPONENTS.put(12863, new int[]{10, 6, 12, 8}); // Dwarf cannon set
		COMPONENTS.put(31142, new int[]{29010, 29004, 29007, 29000}); // Eclipse moon armour set
		COMPONENTS.put(13036, new int[]{3486, 3481, 3483, 3488}); // Gilded armour set (lg)
		COMPONENTS.put(13038, new int[]{3486, 3481, 3485, 3488}); // Gilded armour set (sk)
		COMPONENTS.put(23124, new int[]{23264, 23267, 23261}); // Gilded dragonhide set
		COMPONENTS.put(12865, new int[]{1135, 1099, 1065}); // Green dragonhide set
		COMPONENTS.put(12873, new int[]{4724, 4728, 4730, 4726}); // Guthan's armour set
		COMPONENTS.put(13048, new int[]{2673, 2669, 2671, 2675}); // Guthix armour set (lg)
		COMPONENTS.put(13050, new int[]{2673, 2669, 3480, 2675}); // Guthix armour set (sk)
		COMPONENTS.put(13165, new int[]{10382, 10378, 10380, 10376}); // Guthix dragonhide set
		COMPONENTS.put(13175, new int[]{1057, 1053, 1055}); // Halloween mask set
		COMPONENTS.put(13149, new int[]{3827, 3828, 3829, 3830}); // Holy book page set
		COMPONENTS.put(31169, new int[]{30073, 30076, 30079, 30082}); // Hueycoatl hide armour set
		COMPONENTS.put(9668, new int[]{5574, 5575, 5576}); // Initiate harness m
		COMPONENTS.put(24488, new int[]{24419, 24420, 24421}); // Inquisitor's armour set
		COMPONENTS.put(12980, new int[]{12241, 12235, 12237, 12243}); // Iron gold-trimmed set (lg)
		COMPONENTS.put(12982, new int[]{12241, 12235, 12239, 12243}); // Iron gold-trimmed set (sk)
		COMPONENTS.put(12972, new int[]{1153, 1115, 1067, 1191}); // Iron set (lg)
		COMPONENTS.put(12974, new int[]{1153, 1115, 1081, 1191}); // Iron set (sk)
		COMPONENTS.put(12976, new int[]{12231, 12225, 12227, 12233}); // Iron trimmed set (lg)
		COMPONENTS.put(12978, new int[]{12231, 12225, 12229, 12233}); // Iron trimmed set (sk)
		COMPONENTS.put(22438, new int[]{22326, 22327, 22328}); // Justiciar armour set
		COMPONENTS.put(12883, new int[]{4732, 4736, 4738, 4734}); // Karil's armour set
		COMPONENTS.put(27355, new int[]{27235, 27238, 27241}); // Masori armour set (f)
		COMPONENTS.put(13008, new int[]{12283, 12277, 12279, 12281}); // Mithril gold-trimmed set (lg)
		COMPONENTS.put(13010, new int[]{12283, 12277, 12285, 12281}); // Mithril gold-trimmed set (sk)
		COMPONENTS.put(13000, new int[]{1159, 1121, 1071, 1197}); // Mithril set (lg)
		COMPONENTS.put(13002, new int[]{1159, 1121, 1085, 1197}); // Mithril set (sk)
		COMPONENTS.put(13004, new int[]{12293, 12287, 12289, 12291}); // Mithril trimmed set (lg)
		COMPONENTS.put(13006, new int[]{12293, 12287, 12295, 12291}); // Mithril trimmed set (sk)
		COMPONENTS.put(31166, new int[]{29280, 29283, 29286, 29289}); // Mixed hide armour set
		COMPONENTS.put(23113, new int[]{4089, 4091, 4093, 4095, 4097}); // Mystic set (blue)
		COMPONENTS.put(23116, new int[]{4099, 4101, 4103, 4105, 4107}); // Mystic set (dark)
		COMPONENTS.put(23119, new int[]{23047, 23050, 23053, 23056, 23059}); // Mystic set (dusk)
		COMPONENTS.put(23110, new int[]{4109, 4111, 4113, 4115, 4117}); // Mystic set (light)
		COMPONENTS.put(30744, new int[]{30750, 30753, 30756}); // Oathplate armour set
		COMPONENTS.put(21279, new int[]{21298, 21301, 21304}); // Obsidian armour set
		COMPONENTS.put(13173, new int[]{1038, 1040, 1044, 1042, 1046, 1048}); // Partyhat set
		COMPONENTS.put(9670, new int[]{9672, 9674, 9678}); // Proselyte harness f
		COMPONENTS.put(9666, new int[]{9672, 9674, 9676}); // Proselyte harness m
		COMPONENTS.put(30331, new int[]{30404, 30406, 30408, 30410}); // Raging echoes relic hunter (t1) armour set
		COMPONENTS.put(30334, new int[]{30412, 30414, 30416, 30418}); // Raging echoes relic hunter (t2) armour set
		COMPONENTS.put(30337, new int[]{30420, 30422, 30424, 30426}); // Raging echoes relic hunter (t3) armour set
		COMPONENTS.put(12869, new int[]{2501, 2495, 2489}); // Red dragonhide set
		COMPONENTS.put(31151, new int[]{6128, 6129, 6130, 6151, 6145}); // Rock-shell armour set
		COMPONENTS.put(13024, new int[]{1163, 1127, 1079, 1201}); // Rune armour set (lg)
		COMPONENTS.put(13026, new int[]{1163, 1127, 1093, 1201}); // Rune armour set (sk)
		COMPONENTS.put(13032, new int[]{2619, 2615, 2617, 2621}); // Rune gold-trimmed set (lg)
		COMPONENTS.put(13034, new int[]{2619, 2615, 3476, 2621}); // Rune gold-trimmed set (sk)
		COMPONENTS.put(13028, new int[]{2627, 2623, 2625, 2629}); // Rune trimmed set (lg)
		COMPONENTS.put(13030, new int[]{2627, 2623, 3477, 2629}); // Rune trimmed set (sk)
		COMPONENTS.put(13040, new int[]{2665, 2661, 2663, 2667}); // Saradomin armour set (lg)
		COMPONENTS.put(13042, new int[]{2665, 2661, 3479, 2667}); // Saradomin armour set (sk)
		COMPONENTS.put(13163, new int[]{10390, 10386, 10388, 10384}); // Saradomin dragonhide set
		COMPONENTS.put(26554, new int[]{26427, 26430, 26433, 26436}); // Shattered relic hunter (t1) armour set
		COMPONENTS.put(26557, new int[]{26439, 26442, 26445, 26448}); // Shattered relic hunter (t2) armour set
		COMPONENTS.put(26560, new int[]{26451, 26454, 26457, 26460}); // Shattered relic hunter (t3) armour set
		COMPONENTS.put(31154, new int[]{6137, 6139, 6141, 6153, 6147}); // Skeletal armour set
		COMPONENTS.put(31157, new int[]{6131, 6133, 6135, 6149, 6143}); // Spined armour set
		COMPONENTS.put(20382, new int[]{20178, 20169, 20172, 20181}); // Steel gold-trimmed set (lg)
		COMPONENTS.put(20385, new int[]{20178, 20169, 20175, 20181}); // Steel gold-trimmed set (sk)
		COMPONENTS.put(12984, new int[]{1157, 1119, 1069, 1193}); // Steel set (lg)
		COMPONENTS.put(12986, new int[]{1157, 1119, 1083, 1193}); // Steel set (sk)
		COMPONENTS.put(20376, new int[]{20193, 20184, 20187, 20196}); // Steel trimmed set (lg)
		COMPONENTS.put(20379, new int[]{20193, 20184, 20190, 20196}); // Steel trimmed set (sk)
		COMPONENTS.put(29424, new int[]{28933, 28936, 28939}); // Sunfire fanatic armour set
		COMPONENTS.put(13066, new int[]{2436, 2440, 2442}); // Super potion set
		COMPONENTS.put(31160, new int[]{25398, 25389, 25401, 25392, 25395}); // Swampbark armour set
		COMPONENTS.put(12879, new int[]{4745, 4749, 4751, 4747}); // Torag's armour set
		COMPONENTS.put(31145, new int[]{26382, 26384, 26386}); // Torva armour set
		COMPONENTS.put(25380, new int[]{25028, 25031, 25034, 25037}); // Trailblazer relic hunter (t1) armour set
		COMPONENTS.put(25383, new int[]{25016, 25019, 25022, 25025}); // Trailblazer relic hunter (t2) armour set
		COMPONENTS.put(25386, new int[]{25001, 25004, 25007, 25010}); // Trailblazer relic hunter (t3) armour set
		COMPONENTS.put(28777, new int[]{28712, 28715, 28718, 28721}); // Trailblazer reloaded relic hunter (t1) armour set
		COMPONENTS.put(28780, new int[]{28724, 28727, 28730, 28733}); // Trailblazer reloaded relic hunter (t2) armour set
		COMPONENTS.put(28783, new int[]{28736, 28739, 28742, 28745}); // Trailblazer reloaded relic hunter (t3) armour set
		COMPONENTS.put(24469, new int[]{24405, 24407, 24409, 24411}); // Twisted relic hunter (t1) armour set
		COMPONENTS.put(24472, new int[]{24397, 24399, 24401, 24403}); // Twisted relic hunter (t2) armour set
		COMPONENTS.put(24475, new int[]{24387, 24389, 24391, 24393}); // Twisted relic hunter (t3) armour set
		COMPONENTS.put(13151, new int[]{3831, 3832, 3833, 3834}); // Unholy book page set
		COMPONENTS.put(12875, new int[]{4753, 4757, 4759, 4755}); // Verac's armour set
		COMPONENTS.put(31148, new int[]{26241, 26243, 26245}); // Virtus armour set
		COMPONENTS.put(13044, new int[]{2657, 2653, 2655, 2659}); // Zamorak armour set (lg)
		COMPONENTS.put(13046, new int[]{2657, 2653, 3478, 2659}); // Zamorak armour set (sk)
		COMPONENTS.put(13161, new int[]{10374, 10370, 10372, 10368}); // Zamorak dragonhide set
	}

	static boolean isSet(int itemId)
	{
		return COMPONENTS.containsKey(itemId);
	}

	/** Null when the id is not a known set. */
	static int[] piecesFor(int setItemId)
	{
		return COMPONENTS.get(setItemId);
	}
}
