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
