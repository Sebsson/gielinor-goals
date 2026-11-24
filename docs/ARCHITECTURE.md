# Arkitektur – Gielinor Goals

Referens för utvecklare och agenter. Skriv "se ARCHITECTURE.md" i prompts i stället för att
återberätta pluginet. Uppdatera filen i samma gren som ändrar det som beskrivs här.

Rotpaket: `com.gielinorgoals` (Java 11, inga beroenden utöver `runelite-client`).

## 1. Paket

| Paket | Ansvar |
|---|---|
| *(rot)* | `GielinorGoalsPlugin` (events, trådbyten, livscykel), `GoalManager` (laddad profil, ändringar, utvärdering, sparning), `GielinorGoalsConfig`, `PopupLevel`, `CompletionSound`, `ProfileGuard`. |
| `model` | Ren domänmodell utan Swing/klient: `Goal`, `GoalTree` (DAG), kravtyperna, `GoalEvaluator`, `GoalProgress`, `ActivityCounters`, `DropSource`/`DryMeter`, `NextGoals`, `CompletedLog`, `GameData`. |
| `tracking` | Läser spelet på klienttråden: `SkillTracker`, `ItemTracker`, `AchievementTracker` (quests, diaries, CA), `KillCountMessages` (chatt), `HiscoreFetcher`, `InitialSync`, `AccountStatus`. Bygger `GameSnapshot`. |
| `persistence` | `GoalSerializer` (Gson, `RequirementAdapter` för kravens typfält), `SchemaMigrator`, `LoadResult`, `ContainerCache` (cachad bank/seed vault). |
| `template` | Templates: genererade klasser, handplockade JSON-filer (`JsonTemplate`), `TemplateCatalog`, `TemplatePlanner`/`TemplateBuilder`, `TemplateInserter`, `TemplateUpdates`, `TemplateKeys`, `CustomTemplates`, `TemplateExport`, `ShareCodec`, `ImportValidator`, quest-databasen. |
| `ui` | Swing på EDT: `GielinorGoalsPanel`, `GoalRow`, dialoger (edit, template, import, dela, uppdatera, länka), `CompletedTab`, overlays (`NextGoalOverlay`, `GoalCompletedOverlay`), notiskedjan, dra och släpp (`RowDragController`, `DragHandle`, `DropPlacement`), kartfönstret (`GoalMapWindow`, `GoalMapCanvas`, `GoalMapDetails`). |
| `map` | Kartans modell utan Swing: `MapGraph`, `MapView` (omfång och utfällning), `LayeredLayout` (förenklad Sugiyama), `MapLayout`, `MapLayoutCache`. |

## 2. Datamodell

### Målgrafen
- `GoalTree` är en **DAG**: varje `Goal` lagras en gång (id → mål) med en ordnad `childIds`-lista.
  Rötterna är en egen ordnad lista `rootIds`. Ett delmål kan alltså ha flera föräldrar.
- Flyttoperationer (upp/ner, indrag/utdrag, dra) gäller kopplingen under **en** förälder
  (`parentId == null` = rotlistan). Inte trådsäker, bara EDT.
- Ordningen är prioritet: `NextGoals` går igenom rötterna och delmålen djupet först och föreslår
  ej klara mål utan ej klara delmål. Ett delat mål nås via sin första förälder.

### `Goal`-fält (urval)
`id`, `title`, `note`, `completed`, `completedAt`, `completedAtApprox` (≈-datum), `createdAt`,
`expanded`, `aggregate`, `requirement`, `childIds`, `icon` (för mål utan krav), `wikiPage`,
`doneWhen` + `doneWhenAll`, `dropSource` + `obtainedAtKc`, samt template-fälten `templateId`,
`templateParams`, `templateChoices`, `templateVersion`, `templateIgnoredKeys`, `templateNodeKey`.

### Kravtyper (`RequirementType`, namnen sparas i JSON och får aldrig bytas utan migrering)
| Typ | Klass | Källa |
|---|---|---|
| `SKILL` | `SkillRequirement` | nivå eller XP; läget `XP_GAIN` mäter vinst från `startXp` |
| `ITEM` | `ItemRequirement` | inventory, equipment, bank, seed vault, rune pouch; `includeVariants`, `countsAlso` (t.ex. crystal armour räknas mot seeds) |
| `QUEST` | `QuestRequirement` | quest-status |
| `DIARY` | `DiaryRequirement` | diary-varbits (område + nivå) |
| `COMBAT_ACHIEVEMENT` | `CombatAchievementRequirement` | CA-flaggor |
| `TOTAL_LEVEL`, `COMBAT_LEVEL`, `QUEST_POINTS` | respektive klass | kontonivåer |
| `KILL_COUNT` | `KillCountRequirement` | räknare, aktivitetstyp `HISCORE`, `NPC` eller manuell |
| `POINTS` | `PointsRequirement` | varbit/varp via `PointsActivity` (Slayer, Tithe Farm, Giants' Foundry, Tempoross, Soul Wars, NMZ …) |

Mål utan krav är manuella (bockas av för hand).

### Utvärdering (`GoalEvaluator`)
- Räknar progress för alla automatiska mål och bockar av de som nått målet. **Spärrat**: ett
  klart mål avmarkeras aldrig automatiskt.
- **Aggregat** (`aggregate = true`) visar "X av Y" direkta delmål; ett aggregat utan eget krav
  klaras när alla direkta delmål är klara. Barn räknas före föräldrar.
- **`doneWhen`**: lista med template-nycklar; målet klaras när något (eller alla, om
  `doneWhenAll`) av målen med de nycklarna är klart, t.ex. lägre defenders när dragon defendern finns.
- Aggregat och `doneWhen` itereras tills inget ändras, så kedjor klaras i samma utvärdering.

### Räknare (`ActivityCounters`)
Per profil, nycklade på `KillCountRequirement.counterKey()`, med källa och tid. Flera mål läser
samma räknare. Regler:
- **chatt** (spelets KC-meddelanden): absolut, vinner alltid;
- **hiscores**: används bara om inget värde finns eller om de är högre;
- **loot** (`NpcLootReceived`): +1; räknas bara för NPC:er som något mål följer (alltså från att målet finns);
- **manuellt**: "Set value…" och justeringar.

### Drop-källor och torrhetsmätare
`DropSource` på item-mål: NPC eller hiscore-aktivitet, `rate`, ev. `approximate` + `approximateNote`.
`DryMeter` räknar om till antal drops och visar hur torr man är. `obtainedAtKc` sparas när målet klaras.

## 3. Dataflöde och trådar

```
RuneLite-events (klienttråd)
  StatChanged / ItemContainerChanged / VarbitChanged / WidgetLoaded
      → SkillTracker, ItemTracker, AchievementTracker (markerar ändrat)
  GameTick → trackers läser → GameSnapshot (xp, items, achievements)
      → SwingUtilities.invokeLater → GoalManager.updateGameData(profil, snapshot)   [EDT]
  ChatMessage → KillCountMessages → invokeLater → GoalManager.recordChatCount       [EDT]
  NpcLootReceived → invokeLater → GoalManager.recordNpcKill                         [EDT]
  HiscoreFetcher.request (klienttråd) → HiscoreClient async på OkHttp
      → resultat till GoalManager.recordHiscores på EDT

GoalManager (EDT): GoalEvaluator → GoalProgress per mål → spara → listener
  → onGoalsChanged: panel.rebuild(), kartfönstret, NextGoalOverlay-poster
  → completionListener: GoalCompletionNotifier → NotificationQueue → GoalCompletedOverlay
      (ritas på klienttråden; ljud via clientThread.invokeLater(client::playSoundEffect))
```

- **Klienttråden** läser all speldata. `AchievementTracker` läser bara bevakade mål, på nästa tick;
  quests (script per quest) högst var femte tick.
- **EDT** äger `GoalManager`, `GoalTree`, `ActivityCounters`, `CustomTemplates` och all UI.
- **OkHttp** bara för hiscores (cooldown 10 min efter login, 30 s efter nytt KC-mål; config
  `fetchHiscores`).
- Profilen fångas på klienttråden med datan; `GoalManager` ignorerar data från en annan profil än den
  laddade. Dialoger fångar profilen med `ProfileGuard` och kastar resultatet vid profilbyte.
- `InitialSync`: ett tidsfönster efter inloggning, världsbyte, profilbyte eller påslag. Mål som klaras
  då får ingen popup och ≈-datum. `CompletionWatcher` gör att kartan inte pulserar vid första
  uppdateringen eller profilbyte.
- Template-bygget (quest-kedjor m.m.) kan köras asynkront; resultatet sätts in på EDT.

## 4. Persistens

All lagring går via `ConfigManager`, grupp `gielinorgoals` (`GielinorGoalsConfig.GROUP`).

| Nyckel | Nivå | Innehåll |
|---|---|---|
| `goals` | RS-profil | mål, `rootIds`, `counters`, `schemaVersion` |
| `goalsCorruptBackup` | RS-profil | rådata som inte gick att läsa (sparas innan något skrivs över) |
| `goalsBackupBeforeV<N>` | RS-profil | rådata före migrering till version N |
| `bankCache`, `seedVaultCache` | RS-profil | `ContainerCache` (skrivs av `ItemTracker` på klienttråden) |
| `customTemplates` (+ `customTemplatesCorrupt`) | global | egna templates, samma format som handplockade |
| `mapWindowBounds` m.fl. | global | dolda config-poster för kartfönstret |

En fil med **högre** schemaversion än pluginet känner till laddas skrivskyddad i stället för att
sparas över (därför höjs versionen även för rena fälttillägg).

### Schemaversioner (`SchemaMigrator.CURRENT_VERSION = 16`)
| V | Tillägg |
|---|---|
| 1 | manuella mål |
| 2 | fältet `requirement` |
| 3 | kravtypen `ITEM` |
| 4 | `QUEST`, `DIARY`, `COMBAT_ACHIEVEMENT` |
| 5 | skill-läget `XP_GAIN` + `startXp` |
| 6 | `createdAt`, `completedAtApprox` |
| 7 | **strukturell**: nästlade `children` → platt lista med `childIds` + `rootIds` (`v6ToV7`) |
| 8 | `TOTAL_LEVEL`, `COMBAT_LEVEL`, `QUEST_POINTS` |
| 9 | `KILL_COUNT` + sektionen `counters` |
| 10 | `templateId`, `templateParams`, `templateIgnoredKeys`, `templateNodeKey`, `wikiPage` |
| 11 | `dropSource`, `obtainedAtKc`, `icon`, `doneWhen` |
| 12 | `templateChoices` |
| 13 | `POINTS` + `templateVersion` |
| 14 | `countsAlso` på item-krav |
| 15 | `doneWhenAll` |
| 16 | `approximate`, `approximateNote` på drop-källor |

### Lägga till en migrering
1. Höj `CURRENT_VERSION` och lägg en rad i versionshistoriken i `SchemaMigrator`.
2. Registrera steget från föregående version: `Map.entry(N-1, SchemaMigrator::noStructuralChange)`
   eller en egen `vXToVY` om datan ska skrivas om.
3. Lägg till en fixtur och test i `MigrationFixturesTest`/`SchemaMigratorTest` (gammal JSON → ny).
4. Uppdatera tabellen ovan.

## 5. Template-systemet

### Typer
- **Genererade** (Java-klasser, `GoalTemplate`): Quest cape (`QuestCapeExclusions` tar bort
  miniquests), Achievement diary cape, Max cape, Base level, Skill ladder, Total level, Combat level,
  Combat achievements, Quest series, Diary tier, Diary rewards, Barrows. Bygger på
  `quest-requirements.json` (`QuestRequirementsDb`, `QuestChains`) och `diary-rewards.json`.
- **Handplockade** (`resources/com/gielinorgoals/template/handpicked/*.json`, listade i `index.json`):
  unlocks, utrustning, outfits, capes, raids och **paket** (`package-*.json`) som bara består av
  referenser. Katalogen sorterar genererade före handplockade.
- **Egna** (`CustomTemplates`, id-prefix `custom:`): sparade från ett mål via `TemplateExport`, lagras globalt.

### JSON-format (`JsonTemplate`)
Filnivå: `formatVersion` (formatets version, saknas = 1), `id` (annars filnamnet), `version`
(innehållsversion, höjs vid ändring), `name`, `description`, `category` (se `TemplateCategories`),
`tags`, `expandQuestChains`, `verifiedBy`, `verifiedAt`, parametrar och rotnoden.

Nod: valfri `title`, `note`, `wikiPage`, `aggregate`, `icon`, `doneWhen`/`doneWhenAll`,
`replaces`, högst **ett** krav och `children`. Krav:
```
{"quest": "RECIPE_FOR_DISASTER"}               (+ "expandChain": false)
{"skill": "COOKING", "level": 70}              (egna templates även "xp")
{"questPoints": 175} {"totalLevel": 1500} {"combatLevel": 100}
{"diary": {"area": "VARROCK", "tier": "HARD"}}
{"item": {"id": 7462, "name": "Barrows gloves", "quantity": 1, "includeVariants": true}}
         (+ "dropSource": {"npc"|"hiscore": …, "rate": 500, "approximate": true, "approximateNote": "…"})
{"killCount": {"hiscore": "ZULRAH" | "npc": "Lizardman shaman" | "manual": "…", "count": 100}}
{"points": {"activity": "SLAYER", "count": 750}}
{"combatAchievement": {"id": 12, "name": "Noxious Foe", "tier": "Easy"}}
```
Ikon för noder utan krav: `{"item": id}`, `{"activity": "THE_HUEYCOATL"}` eller `{"sideIcon": "QUEST"}`.

Delade noder och referenser:
- `"id": "n1"` + en annan nod `{"ref": "n1"}` = samma delmål under flera föräldrar (egna templates och export).
- `{"template": "fire-cape"}` eller `{"template": "base-level", "params": {"level": 70}}` = den
  refererade templatens hela träd insatt där. Bara inbyggda templates. Samma template med samma
  värden blir en delad nod. Katalogen kontrollerar att id:n finns och att det inte finns cykler.

En trasig fil ger ingen template alls, aldrig en halv.

### Nycklar, dubblettkoppling och uppdateringar
- `TemplateKeys` härleder en stabil nyckel ur kravet (`quest:117`, `skill:ATTACK:70`, `item:7462`,
  `kc:manual:slayer points:750`); noder utan krav får en nyckel av titeln. Sparas som
  `templateNodeKey`; formatet får inte ändras utan migrering.
- `TemplateInserter`: roten blir ett nytt rotmål; för varje delmål med krav letas ett befintligt
  mål med exakt samma krav och kopplas in i stället för en dubblett (det behåller sina egna delmål).
- `TemplateUpdates` ("Update from template"): bygger om med sparade parametrar, jämför nycklar och
  visar tillägg, borttagna och (via `replaces`) ersatta noder. `templateIgnoredKeys` minns det
  användaren valt bort. `templateVersion` visar om templaten ändrats.

### Delning
`ShareCodec`: `GG1:` + base64(gzip(JSON i template-formatet)). Används för en template, ett mål med
delgraf och backup av alla mål. Endast urklipp eller fil. Import är opålitlig: storlek begränsas
före och efter uppackning, `ImportValidator` kontrollerar fält, typer, längder, djup och antal.
Import slår ihop (dubblettkoppling som ovan) och ersätter aldrig befintliga mål. Export tar bara med
målen, aldrig klar-status, datum eller räknarvärden.

## 6. Recept

### Ny kravtyp
1. Klass i `model` som implementerar `Requirement` (progress från `GameData`/räknare).
2. Ny konstant i `RequirementType` (namnet blir permanent).
3. Läsning i rätt tracker och fält i `GameSnapshot`/`GameData` om ny data behövs; bevaka bara de mål som finns.
4. Nyckel i `TemplateKeys`, nodformat i `JsonTemplate` (läsa) och `TemplateExport` (skriva), `ImportValidator`.
5. UI: `GoalEditDialog`, ikon/text i `GoalRow`/`UiText`.
6. Ny schemaversion (se §4) och tester (modell, serializer, migrering, template-format).

### Ny handplockad template
1. JSON-fil i `template/handpicked/`, lägg till i `index.json`, välj `category`.
2. Hämta siffror från OSRS Wiki; lägg en sektion i `docs/template-verification.md` med länkar och
   **(uncertain)** där något inte kunde bekräftas. Lämna `verifiedBy`/`verifiedAt` tomma.
3. Bara grenarna "Lås upp" och "Skaffa", inga wield-krav. Återanvänd andra templates med `{"template": …}`.
4. Test (t.ex. i `HandpickedTemplatesTest` eller ett tematest). Höj `version` vid ändring av befintlig template.

### Ny config-post
1. `@ConfigItem` i `GielinorGoalsConfig` (grupp `gielinorgoals`), i rätt `section`, med `position`.
2. Nyckeln är permanent – byt aldrig namn utan migrering.
3. Tredjepartsserver: av som standard och med RuneLites `warning`-text (se AGENTS.md).
4. Behöver UI:t byggas om: lägg nyckeln i `onConfigChanged` i pluginet.
