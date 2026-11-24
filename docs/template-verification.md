# Template verification

One section per handpicked template. Check the numbers against the OSRS Wiki and tick the box. Then fill in
`verifiedBy` and `verifiedAt` in the template's JSON file.

Items marked **(uncertain)** were not confirmed on the wiki when the template was written. Check those first.

Data was read from the wiki on 2026-11-24.

---

## Wave 2 – unlocks (milestone 13, part 2)

### Prifddinas access (`prifddinas.json`)
- [ ] Verified
- Wiki: [Prifddinas](https://oldschool.runescape.wiki/w/Prifddinas), [Song of the Elves](https://oldschool.runescape.wiki/w/Song_of_the_Elves)
- Every way into the city requires Song of the Elves.
- Song of the Elves: 70 Agility, Construction, Farming, Herblore, Hunter, Mining, Smithing and Woodcutting (not boostable).
- Quests: Mourning's End Part II, Making History and Druidic Ritual. The full chain comes from `quest-requirements.json`.
- The Song of the Elves node is shared with *Bow of Faerdhinen* and *Spirit trees*.

### Lunar spellbook (`lunar-spellbook.json`)
- [ ] Verified
- Wiki: [Lunar spellbook](https://oldschool.runescape.wiki/w/Lunar_spellbook), [Lunar Diplomacy](https://oldschool.runescape.wiki/w/Lunar_Diplomacy), [Seal of Passage](https://oldschool.runescape.wiki/w/Seal_of_Passage)
- The spellbook is unlocked by completing Lunar Diplomacy.
- Quests: The Fremennik Trials, Lost City, Rune Mysteries and Shilo Village.
- Skills: Herblore 5, Crafting 61, Defence 40, Firemaking 49, Magic 65, Mining 60 and Woodcutting 55.
- Casting any Lunar spell needs 65 Magic and 40 Defence without boosts. This is a note, not a goal.
- Seal of Passage: item 9083 (`LUNAR_SEAL_OF_PASSAGE`). Used as the icon only.

### Ancient Magicks (`ancient-magicks.json`)
- [ ] Verified
- Wiki: [Ancient Magicks](https://oldschool.runescape.wiki/w/Ancient_Magicks), [Desert Treasure I](https://oldschool.runescape.wiki/w/Desert_Treasure_I)
- The spellbook is unlocked by completing Desert Treasure I. You need 50 Magic to use it at all.
- Quests: The Dig Site, Temple of Ikov, The Tourist Trap, Troll Stronghold, Priest in Peril and Waterfall Quest.
- Skills: Magic 50 (not boostable). Thieving 53, Firemaking 50 and Slayer 10 are not needed to start.
- **(uncertain)** The Ancient Magicks page also lists Druidic Ritual. The quest page does not.

### Arceuus spellbook (`arceuus-spellbook.json`)
- [ ] Verified
- Wiki: [Arceuus spellbook](https://oldschool.runescape.wiki/w/Arceuus_spellbook)
- To switch, speak to Tyss. There are no requirements since Kourend favour was removed on 10 January 2024. This is a manual goal.
- A Kingdom Divided is needed for most combat and utility spells. It is in its own group and is not an access requirement.

### Fossil Island (`fossil-island.json`)
- [ ] Verified
- Wiki: [Fossil Island](https://oldschool.runescape.wiki/w/Fossil_Island), [Bone Voyage](https://oldschool.runescape.wiki/w/Bone_Voyage), [Museum Camp](https://oldschool.runescape.wiki/w/Museum_Camp), [Mycelium Transportation System](https://oldschool.runescape.wiki/w/Mycelium_Transportation_System), [Volcanic Mine](https://oldschool.runescape.wiki/w/Volcanic_Mine)
- Bone Voyage needs 100 Kudos (a manual goal) and The Dig Site.
- Museum Camp buildings:

  | Building | Construction | Materials |
  |---|---|---|
  | Cleaning station | 5 | |
  | Bank chest | 21 | 2 oak planks, 1 iron bar, 5 nails |
  | Well | 22 | |
  | Cooking pot | 24 | |
  | Spinning wheel | 28 | |
  | Loom | 29 | |

  Everything together needs 10 oak planks, 5 planks, 2 iron bars, 25 nails, 2 ropes, 2 logs, 3 soft clay, a bucket and a tinderbox.
- Magic Mushtrees: House on the Hill, Verdant Valley, Sticky Swamp (needs an axe and a rake) and Mushroom Meadow.
- Volcanic Mine: 50 Mining. It costs 30 numulite per visit, or **(uncertain)** 3,000 numulite for permanent access. Numulite is item 21555 (`FOSSIL_NUMULITE`).
- Digsite pendant teleport to the House on the Hill: the cost is not stated on the wiki.
- Not included: birdhouses, the Slayer requirement for wyverns and the underwater area.

### Kingdom of Miscellania (`miscellania.json`)
- [ ] Verified
- Wiki: [Throne of Miscellania](https://oldschool.runescape.wiki/w/Throne_of_Miscellania), [Royal Trouble](https://oldschool.runescape.wiki/w/Royal_Trouble), [Managing Miscellania](https://oldschool.runescape.wiki/w/Managing_Miscellania)
- Throne of Miscellania: Heroes' Quest and The Fremennik Trials. Support must go from 25% to 75%.
- Royal Trouble: Agility 40 and Slayer 40 (boostable).
- Workers: 10, or 15 after Royal Trouble.
- Coffer: up to 5m, or 7.5m after Royal Trouble. Wages are 10% a day, at most 50k (75k after Royal Trouble).
- The wiki recommends keeping at least 500k in the coffer, or 750k after Royal Trouble.
- Approval falls 2.5% a day, or 1% a day after Royal Trouble.

### Ectophial (`ectophial.json`)
- [ ] Verified
- Wiki: [Ectophial](https://oldschool.runescape.wiki/w/Ectophial), [Ghosts Ahoy](https://oldschool.runescape.wiki/w/Ghosts_Ahoy)
- Item 4251 (`ECTOPHIAL`). The empty one is 4252 and is counted as a variant.
- Reward from Ghosts Ahoy. A lost one comes free from Velorina with a ghostspeak amulet, or costs 4,600 coins from Perdu.
- Ghosts Ahoy: Priest in Peril and The Restless Ghost. Agility 25 and Cooking 20 (boostable).

### Dragon scimitar (`dragon-scimitar.json`)
- [ ] Verified
- Wiki: [Dragon scimitar](https://oldschool.runescape.wiki/w/Dragon_scimitar), [Monkey Madness I](https://oldschool.runescape.wiki/w/Monkey_Madness_I)
- Item 4587 (`DRAGON_SCIMITAR`). Daga sells it for 100,000 coins after Monkey Madness I. Coins are item 995.
- 60 Attack is a wield requirement, so it is not included.

### Spirit trees (`spirit-trees.json`)
- [ ] Verified
- Wiki: [Spirit tree](https://oldschool.runescape.wiki/w/Spirit_tree)
- Tree Gnome Village opens the network. The Grand Tree is needed only to travel from the Gnome Stronghold tree.
- Poison Waste: partial The Path of Glouphrie. This is a manual goal that is done by the full quest.
- Prifddinas: Song of the Elves.
- Laguna Aurorae: Pandemonium, 58 Sailing and mooring at the island once.
- Patches: Etceteria, Port Sarim, Brimhaven and Hosidius need 83 Farming.
- Farming Guild: **(uncertain)** 85 Farming. The wiki only says you must be boosted to 85 to teleport there.
- Trees outside your house: 1 at 83 Farming, 2 at 88, 3 at 93, 4 at 96 and any number at 99.
- In your house: 75 Construction and 83 Farming. The spiritual fairy tree needs 95 Construction.

### Gnome gliders (`gnome-gliders.json`)
- [ ] Verified
- Wiki: [Gnome glider](https://oldschool.runescape.wiki/w/Gnome_glider)
- The Grand Tree opens Ta Quir Priw, Sindarpos, Kar-Hewo, Gandius and the one-way flight to Lemanto Andra.
- Feldip Hills: partial One Small Favour. This is a manual goal that is done by the full quest.
- Ape Atoll: Monkey Madness II.

### Bonecrusher (`bonecrusher.json`)
- [ ] Verified
- Wiki: [Bonecrusher](https://oldschool.runescape.wiki/w/Bonecrusher)
- Item 13116 (`BONECRUSHER`). Reward for the hard Morytania Diary, from a ghost disciple in the Ectofuntus.
- Charged with ecto-tokens: 25 charges per token.

### Ash sanctifier (`ash-sanctifier.json`)
- [ ] Verified
- Wiki: [Ash sanctifier](https://oldschool.runescape.wiki/w/Ash_sanctifier)
- Item 25781 (`ASH_SANCTIFIER`). Reward for the hard Kourend & Kebos Diary, from Tyss.
- Charged with death runes: 10 charges per rune.

### Herb sack (`herb-sack.json`)
- [ ] Verified
- Wiki: [Herb sack](https://oldschool.runescape.wiki/w/Herb_sack)
- Item 13226 (`SLAYER_HERB_SACK`). The open one is 24478.
- Costs 750 slayer points (a POINTS goal), or 250 Tithe Farm points (in the note).
- **(uncertain)** 58 Herblore: the wiki gives it as a requirement to buy on one page and to use on another.
- The "750 slayer points" goal links with the same goal in *Rune pouch*. Points are spent, so you need 1,500 for both. This is in the note.

### Seed box (`seed-box.json`)
- [ ] Verified
- Wiki: [Seed box](https://oldschool.runescape.wiki/w/Seed_box)
- Item 13639 (`SEED_BOX`). The open one is 24482.
- Costs 250 Tithe Farm points (a POINTS goal). One per account.

### Slayer helmet (`slayer-helm.json`)
- [ ] Verified
- Wiki: [Slayer helmet](https://oldschool.runescape.wiki/w/Slayer_helmet), [Slayer Rewards](https://oldschool.runescape.wiki/w/Slayer_Rewards), [Black mask](https://oldschool.runescape.wiki/w/Black_mask)
- The Malevolent masquerade unlock costs 400 slayer points (POINTS). Assembling the helmet needs 55 Crafting.
- Slayer helmet: item 11864 (`SLAYER_HELM`).
- Parts:

  | Part | Item ID | Gameval constant | Source |
  |---|---|---|---|
  | Black mask | 8921 | `HARMLESS_BLACK_MASK` | Cave horror drop, 1/512. Needs 58 Slayer and Cabin Fever. |
  | Earmuffs | 4166 | `SLAYER_EARMUFFS` | 200 coins |
  | Facemask | **(uncertain)** 4164 | `SLAYER_FACEMASK` | Shop |
  | Nose peg | **(uncertain)** 4168 | `SLAYER_NOSEPEG` | Shop |
  | Spiny helmet | **(uncertain)** 4551 | `WALLBEAST_SPIKE_HELMET` | Shop |
  | Enchanted gem | **(uncertain)** 4155 | `SLAYER_GEM` | Shop |

  The IDs marked uncertain come from the gameval names, not from the wiki.
- Reinforced goggles are needed only after A Porcine of Interest, so they are not included.

### Void Knight equipment (`void-knight.json`)
- [ ] Verified
- Wiki: [Void Knight equipment](https://oldschool.runescape.wiki/w/Void_Knight_equipment), [Void knight top](https://oldschool.runescape.wiki/w/Void_knight_top)
- Buying needs 42 Attack, Strength, Defence, Hitpoints, Ranged and Magic, and 22 Prayer.
- Points: top 250, robe 250, gloves 150 and 200 per helm. All pieces with three helms come to 1,250 (a manual counter).
- Items:

  | Piece | Item ID | Gameval constant |
  |---|---|---|
  | Top | 8839 | `PEST_VOID_KNIGHT_TOP` |
  | Robe | **(uncertain)** 8840 | `PEST_VOID_KNIGHT_ROBES` |
  | Gloves | **(uncertain)** 8842 | `PEST_VOID_KNIGHT_GLOVES` |
  | Mage helm | **(uncertain)** 11663 | `GAME_PEST_MAGE_HELM` |
  | Ranger helm | **(uncertain)** 11664 | `GAME_PEST_ARCHER_HELM` |
  | Melee helm | **(uncertain)** 11665 | `GAME_PEST_MELEE_HELM` |

### Elite void (`elite-void.json`)
- [ ] Verified
- Wiki: [Elite Void Knight equipment](https://oldschool.runescape.wiki/w/Elite_Void_Knight_equipment), [Elite void top](https://oldschool.runescape.wiki/w/Elite_void_top)
- Needs the hard Western Provinces Diary. Each upgrade costs 200 points, 400 for both (a manual counter).
- Elite void top: 13072 (`ELITE_VOID_KNIGHT_TOP`). Elite void robe: **(uncertain)** 13073 (`ELITE_VOID_KNIGHT_ROBES`).
- The void knight top and robe are shared nodes with *Void Knight equipment*.

---

## Wave 2 – skilling outfits and capes (milestone 13, part 3)

The piece item IDs in this section come from the gameval names, not from the wiki infoboxes, because the outfit pages don't list IDs. All of them are **(uncertain)** until checked.

### Angler's outfit (`angler.json`)
- [ ] Verified
- Wiki: [Angler's outfit](https://oldschool.runescape.wiki/w/Angler%27s_outfit), [Molch pearl](https://oldschool.runescape.wiki/w/Molch_pearl)
- Fishing Trawler: 1/12 per game for a piece you don't own, if you have at least 50 contribution. This is a drop source with a manual counter called "Fishing Trawler games".
- Or 50 molch pearls per piece from Alry, 200 for the set. Molch pearl is item 22820 (`AERIAL_FISHING_PEARL`), confirmed on the wiki.
- Aerial fishing needs 43 Fishing. This is a note.
- Pieces: hat 13258, top 13259, waders 13260, boots 13261 (`TRAWLER_REWARD_*`).
- A piece also counts as done when its spirit angler piece is done (doneWhen).

### Prospector kit (`prospector.json`)
- [ ] Verified
- Wiki: [Prospector kit](https://oldschool.runescape.wiki/w/Prospector_kit), [Golden nugget](https://oldschool.runescape.wiki/w/Golden_nugget)
- Golden nuggets: helmet 40, jacket 60, legs 50, boots 30, so 180 for the set. Golden nugget is item 12012 (`MOTHERLODE_NUGGET`).
- The Volcanic Mine alternative costs 120,000 points for the set. It is a note; the price per piece is not on the wiki.
- Pieces: 12013 to 12016 (`MOTHERLODE_REWARD_HAT/TOP/LEGS/BOOTS`).
- No Mining level is included, because the wiki page doesn't give one.

### Lumberjack outfit (`lumberjack.json`)
- [ ] Verified
- Wiki: [Lumberjack outfit](https://oldschool.runescape.wiki/w/Lumberjack_outfit)
- Forestry Shop prices:

  | Piece | Bark | Maple | Yew | Magic | Redwood |
  |---|---|---|---|---|---|
  | Hat | 1,200 | | 200 | 100 | |
  | Top | 1,500 | | 60 | 120 | 120 |
  | Legs | 1,300 | | 160 | 140 | |
  | Boots | 1,000 | 100 | 200 | | |
  | **Set** | **5,000** | **100** | **620** | **360** | **120** |

- Bark is 28134 (`FORESTRY_CURRENCY`). Logs: maple 1517, yew 1515, magic 1513, redwood 19669.
- Temple Trekking alternative: 25% per undead lumberjack kill.
- Pieces: hat 10941, top 10939, legs 10940, boots 10933 (`RAMBLE_LUMBERJACK_*`).
- **(uncertain)** Gameval also has `FORESTRY_LUMBERJACK_*` (28169 to 28175). It's unclear which one the Forestry Shop gives.

### Pyromancer outfit (`pyromancer.json`)
- [ ] Verified
- Wiki: [Pyromancer outfit](https://oldschool.runescape.wiki/w/Pyromancer_outfit)
- From the Wintertodt reward cart. **(uncertain)** The drop rate per piece is not on the outfit page, so there is no drop source.
- Pieces: hood 20708, garb 20704, robe 20706, boots 20710.
- Wintertodt's own requirements were not checked.

### Rogue equipment (`rogue.json`)
- [ ] Verified
- Wiki: [Rogue equipment](https://oldschool.runescape.wiki/w/Rogue_equipment)
- From rogue's equipment crates in Rogues' Den, and you choose the piece.
- The wiki says the set "effectively requires" 50 Agility and 50 Thieving. Both are goals.
- Pieces: mask 5554, top 5553, gloves 5556, trousers 5555, boots 5557 (`ROGUESDEN_*`).

### Farmer's outfit (`farmers-outfit.json`)
- [ ] Verified
- Wiki: [Farmer's outfit](https://oldschool.runescape.wiki/w/Farmer%27s_outfit)
- Tithe Farm points: strawhat 75, jacket 150, trousers 125, boots 50, so 400 for the set. This is a POINTS goal.
- Pieces, male variant: 13646, 13642, 13640, 13644.
- **(uncertain)** The female variants are 13647, 13643, 13641 and 13645. They only count if RuneLite's variation table links the two.

### Carpenter's outfit (`carpenters-outfit.json`)
- [ ] Verified
- Wiki: [Carpenter's outfit](https://oldschool.runescape.wiki/w/Carpenter%27s_outfit)
- Carpenter points: helmet 400, shirt 800, trousers 600, boots 200, so 2,000 for the set. This is a manual counter, because the points can't be read.
- Pieces: 24872, 24874, 24876, 24878 (`CONSTRUCTION_HAT/SHIRT/TROUSERS/BOOTS`).

### Smiths' uniform (`smiths-uniform.json`)
- [ ] Verified
- Wiki: [Smiths' Uniform](https://oldschool.runescape.wiki/w/Smiths%27_Uniform)
- Foundry reputation: tunic 4,000, trousers 4,000, gloves 3,500, boots 3,500, so 15,000 for the set. This is a POINTS goal.
- Pieces: tunic 27023, trousers 27025, gloves 27029, boots 27027.
- **(uncertain)** The iced gloves (27031) only count through variants.

### Spirit angler's outfit (`spirit-angler.json`)
- [ ] Verified
- Wiki: [Spirit angler's outfit](https://oldschool.runescape.wiki/w/Spirit_angler%27s_outfit)
- Each piece costs 1,200 spirit flakes plus the matching angler piece, so 4,800 flakes for the set. Spirit flakes are 25588 (`SPIRIT_FLAKES`).
- Pieces: headband 25592, top 25594, waders 25596, boots 25598.
- The angler pieces are shared nodes with *Angler's outfit*.

### Dizana's quiver (`dizanas-quiver.json`)
- [ ] Verified
- Wiki: [Dizana's quiver](https://oldschool.runescape.wiki/w/Dizana%27s_quiver), [Fortis Colosseum](https://oldschool.runescape.wiki/w/Fortis_Colosseum)
- The Colosseum needs Children of the Sun. The quiver is guaranteed for beating Sol Heredit in wave 12, or Minimus sells it for 4,000 sunfire splinters.
- Uncharged quiver is 28947 and charged is 28951, both confirmed on the wiki.
- Blessed: 150,000 sunfire splinters at a Shrine of Ralos.
- **(uncertain)** The blessed quiver is assumed to be 28955 (`DIZANAS_QUIVER_INFINITE`). Sunfire splinters are assumed to be 28924 (`SUNFIRESPLINTER`).
- The 75 Ranged needed to wear it is not included.

### Mythical cape (`mythical-cape.json`)
- [ ] Verified
- Wiki: [Mythical cape](https://oldschool.runescape.wiki/w/Mythical_cape), [Myths' Guild](https://oldschool.runescape.wiki/w/Myths%27_Guild), [Dragon Slayer II](https://oldschool.runescape.wiki/w/Dragon_Slayer_II)
- Jack sells it for 10,000 coins. The cape is 22114, confirmed on the wiki.
- Needs Dragon Slayer II. The quest chain comes from the quest database.

### Champion's cape (`champions-cape.json`)
- [ ] Verified
- Wiki: [Champion's cape](https://oldschool.runescape.wiki/w/Champion%27s_cape), [Champions' Challenge](https://oldschool.runescape.wiki/w/Champions%27_Challenge), [Champion's scroll](https://oldschool.runescape.wiki/w/Champion%27s_scroll)
- You need 32 quest points before scrolls can drop. Each scroll is 1/5,000.
- There are 10 champions plus Leon d'Cour. The cape is 21439, confirmed on the wiki.
- Drop sources used, one main monster per scroll: imp, goblin, skeleton, zombie, hill giant, hobgoblin, ghoul, earth warrior, jogre and lesser demon. Other monsters also drop each scroll.
- **(uncertain)** The scroll IDs come from the gameval names: 6798 to 6807 (`CHAMPIONS_CHALLENGE_*`).

---

## Packages (milestone 14)

Packages contain only references to other templates and notes, so only the order needs checking. The guide ([Ironman guide](https://oldschool.runescape.wiki/w/Ironman_guide)) groups items by stage but gives no order within a stage. The order inside each package is therefore a suggestion. Each part is verified in its own section above.

### Early ironman (`package-early-iron.json`)
- [ ] Order verified
- Order: Ectophial, Dragon scimitar, Ardougne cloak and fairy rings, Graceful, Fighter torso, Dragon defender, Rune pouch, Barrows gloves, Ava's assembler, Bonecrusher, Herb sack, Seed box, Base 70.
- **Differs from the guide:** the guide places Ava's assembler in late game.

### Mid ironman (`package-mid-iron.json`)
- [ ] Order verified
- Order: Lunar spellbook, Ancient Magicks, Fire cape, Slayer helmet, Elite void, Ash sanctifier, Zenyte jewellery, Prifddinas access, Dragon warhammer, Bow of Faerdhinen and crystal armour.
- **Differs from the guide:** the guide places Fire cape and Slayer helmet in early game, and zenyte in late game.

### Late ironman (`package-late-iron.json`)
- [ ] Order verified
- Order: Infernal cape, Dizana's quiver, Doom of Mokhaiotl, The Hueycoatl, Base 90.

### All capes (`package-all-capes.json`)
- [ ] Order verified
- Order: Quest cape, Achievement diary cape, Max cape, Fire cape, Infernal cape, Dizana's quiver, Mythical cape, Champion's cape, Imbued god cape, Ardougne cloak 4 (from diary rewards, Ardougne).

---

## Wave 3 – equipment (milestone 15, part 1)

### Toxic blowpipe + Serpentine helm (`blowpipe-serp.json`)
- [ ] Verified
- Wiki: [Zulrah](https://oldschool.runescape.wiki/w/Zulrah), [Toxic blowpipe](https://oldschool.runescape.wiki/w/Toxic_blowpipe), [Serpentine helm](https://oldschool.runescape.wiki/w/Serpentine_helm)
- Zulrah needs Regicide.
- Tanzanite fang and Serpentine visage drop as 2 × 1/1,024 each, about 1/512 per kill. The dry meter uses the Zulrah KC from the hiscores.
- Fletching 78 to make the blowpipe. It was 53 before May 2024. Crafting 52 to make the serpentine helm.
- Zulrah's scales: 11,000 as a target (charging). Each kill gives 100–299 scales. The quantity is a suggestion, not a requirement.
- Item ids: fang 12922, blowpipe 12924, visage 12927, helm 12929, scales 12934.

### Trident of the seas (`trident-seas.json`)
- [ ] Verified
- Wiki: [Kraken](https://oldschool.runescape.wiki/w/Kraken), [Trident of the seas](https://oldschool.runescape.wiki/w/Trident_of_the_seas)
- Slayer 87 for the Kraken boss.
- The uncharged trident drops at 1/512 from the boss and 1/200 from cave kraken. The goal uses the boss rate and the Kraken KC.
- The goal counts the charged variants too. The uncharged trident is 11908.
- **(uncertain)** The 87 Slayer is also required when the boss is fought outside a task. The wiki says so, but check it.

### Trident of the swamp (`trident-swamp.json`)
- [ ] Verified
- Wiki: [Trident of the swamp](https://oldschool.runescape.wiki/w/Trident_of_the_swamp), [Magic fang](https://oldschool.runescape.wiki/w/Magic_fang)
- References *Trident of the seas*, so the seas node is shared and not copied.
- Regicide for Zulrah, Crafting 59 to combine.
- Magic fang 12932 drops as 2 × 1/1,024 from Zulrah, about 1/512. Trident of the swamp is 12900.

### Dragon hunter crossbow (`dhcb.json`)
- [ ] Verified
- Wiki: [Dragon hunter crossbow](https://oldschool.runescape.wiki/w/Dragon_hunter_crossbow), [Chambers of Xeric](https://oldschool.runescape.wiki/w/Chambers_of_Xeric)
- Only from Chambers of Xeric. It is 4/60 of the unique rolls in normal mode and 4/56 in Challenge Mode.
- Crossbow 21012. "Raid the Chambers of Xeric" is a manual goal that is done when you own the crossbow.
- **(uncertain)** No drop rate yet. The raid unique chance depends on points, so it gets an approximate rate in part 2.

### Barrows set (`BarrowsTemplate.java`, generated)
- [ ] Verified
- Wiki: [Barrows](https://oldschool.runescape.wiki/w/Barrows), [Chest (Barrows)](https://oldschool.runescape.wiki/w/Chest_(Barrows)), [Barrows equipment](https://oldschool.runescape.wiki/w/Barrows_equipment)
- The brother is a choice: Ahrim, Dharok, Guthan, Karil, Torag or Verac. It is generated in Java because JSON templates have no choice parameters.
- Unlock: Priest in Peril, Nature Spirit started (Mort Myre) and His Faithful Servants started (digging). The two started quests are manual goals that become done when the quest is completed.
- With all six brothers killed, each piece is 7 × 1/2,448, about 1/350 per chest. Any piece per roll is 1/(450 − 58N). The dry meter uses Barrows chests from the hiscores.
- The goals count degraded pieces too.
- **(uncertain)** The item ids come from the gameval names `BARROWS_<BROTHER>_HEAD/BODY/LEGS/WEAPON`, because the wiki pages show no ids.

### Moons of Peril (`moons-of-peril.json`)
- [ ] Verified
- Wiki: [Moons of Peril](https://oldschool.runescape.wiki/w/Moons_of_Peril), [Lunar Chest](https://oldschool.runescape.wiki/w/Lunar_Chest), [Perilous Moons](https://oldschool.runescape.wiki/w/Perilous_Moons)
- Needs the quest Perilous Moons.
- Three groups: Blood Moon, Blue Moon and Eclipse Moon, each with helm, chestplate, tassets and weapon.
- Each piece is 1/224 per chest (1/56 per moon). The dry meter uses Lunar Chests from the hiscores.
- **(uncertain)** Ids from gameval: Blood 29028/29022/29025 + Dual macuahuitl 28997, Blue 29019/29013/29016 + Blue moon spear 28988, Eclipse 29010/29004/29007 + Eclipse atlatl 29000.

### Sunfire fanatic armour (`sunfire-fanatic.json`)
- [ ] Verified
- Wiki: [Sunfire fanatic armour](https://oldschool.runescape.wiki/w/Sunfire_fanatic_armour), [Fortis Colosseum](https://oldschool.runescape.wiki/w/Fortis_Colosseum)
- Needs Children of the Sun.
- The helm is about 1/24.47 per full run (wave rates from 70/43,400 up to 75/4,800). The dry meter uses Sol Heredit KC.
- Helm 28933, cuirass 28936, chausses 28939.
- **(uncertain)** The cuirass and chausses have no rate. Their per-wave rates and the wave they start at (3 or 4) were not confirmed.

### Tormented bracelet + Amulet of torture (`torture-tormented.json`)
- [ ] Verified
- Wiki: [Amulet of torture](https://oldschool.runescape.wiki/w/Amulet_of_torture), [Tormented bracelet](https://oldschool.runescape.wiki/w/Tormented_bracelet), [Lvl-6 Enchant](https://oldschool.runescape.wiki/w/Lvl-6_Enchant)
- References *Zenyte jewellery*. The shards, onyx and levels are shared and not copied.
- Crafting 95 (bracelet) and 98 (amulet). Magic 93 for Lvl-6 Enchant, which costs 20 soul, 20 blood and 1 cosmic rune.
- Tormented bracelet 19544, Amulet of torture 19553.

### Dragonfire shield (`dragonfire-shield.json`)
- [ ] Verified
- Wiki: [Dragonfire shield](https://oldschool.runescape.wiki/w/Dragonfire_shield), [Draconic visage](https://oldschool.runescape.wiki/w/Draconic_visage)
- Smithing 90 to make it at an anvil. Oziach makes it for 1.25m after Dragon Slayer I (note).
- Draconic visage 11286: 1/5,000 from Vorkath, King Black Dragon and the black dragons in the Wilderness Slayer Cave. Rune dragons drop it at 1/8,000, adamant at 1/9,000 and other dragons at 1/10,000. The dry meter uses Vorkath KC.
- Anti-dragon shield 1540, DFS 11284. The shield and the visage are done once you own the DFS.

### Desert Treasure II rings (`dt2-rings.json`)
- [ ] Verified
- Wiki: [Desert Treasure II - The Fallen Empire](https://oldschool.runescape.wiki/w/Desert_Treasure_II_-_The_Fallen_Empire), [Chromium ingot](https://oldschool.runescape.wiki/w/Chromium_ingot), [Ultor ring](https://oldschool.runescape.wiki/w/Ultor_ring)
- Needs the quest. Magic 90 and Crafting 80 to make the rings, with 3 chromium ingots each.
- Chromium ingots 28276 × 12. Owned rings count as 3 ingots each.
- Vestiges and boss rates:

  | Ring | Vestige | Boss | Rate |
  |---|---|---|---|
  | Ultor 28307 | 28285 | Vardorvis | 1/1,088 |
  | Bellator 28316 | 28279 | The Whisperer | 1/512 |
  | Magus 28313 | 28281 | Duke Sucellus | 1/720 |
  | Venator 28310 | 28283 | The Leviathan | 1/768 |

- Each vestige is done once you own its ring.
- **(uncertain)** The wiki pages disagreed on whether the magus and venator rings need blood runes or similar to make. It is not a goal.
