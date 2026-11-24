# Beslut

Korta beslutsposter. Nya beslut läggs **sist** med nästa nummer. Ett ändrat beslut får en ny post
som hänvisar till den gamla; gamla poster skrivs inte om. Datum = när beslutet togs eller
infördes (från git-historiken där inget annat är känt).

Format: **Beslut** · **Datum** · **Varför** · **Konsekvens**

---

### D1. Popup: eget overlay TOP_CENTER
- **Beslut:** Klarade mål visas i ett eget overlay (`GoalCompletedOverlay`, `OverlayPosition.TOP_CENTER`), inte i spelets popup-interface. Ljudet är spelets egna ljudeffekter (`CompletionSound`: Trophy ding, Fanfare, Relic unlock, Twinkle, Crystal chime), separat för delmål och huvudmål.
- **Datum:** 2026-11-20
- **Varför:** Spelets popup kan inte styras utan att röra interface-widgets, och vi behöver kö, hover-paus och egen text. Spelets ljud följer spelarens volyminställning.
- **Konsekvens:** Config `popupLevel` (All goals / Only main goals / Off), `popupDuration`, `soundEnabled`, `goalSound`, `rootGoalSound`. Popupen stannar kvar medan musen är över den.

### D2. Popup bara för nya, automatiska avbockningar
- **Beslut:** Ett delmål som klaras i samma batch som en förfader ger ingen egen popup. Ingen popup under första synken (`InitialSync`), vid profilbyte eller vid manuell avbockning.
- **Datum:** 2026-11-20
- **Varför:** Undvika popup-storm vid inloggning och dubbla notiser för samma framsteg.
- **Konsekvens:** Mål som klaras under första synken får ≈-datum (`completedAtApprox`).

### D3. Completed är en logg, inte en flytt
- **Beslut:** Klara mål ligger kvar nedtonade i trädet. Fliken Completed är en logg över avbockningar. Mål som upptäcks redan klara får ≈-datum. `createdAt` och "tog X dagar" visas och kan stängas av (`showTimeToComplete`).
- **Datum:** 2026-11-21
- **Varför:** Strukturen ska vara stabil; att flytta mål skulle bryta föräldrar och delade mål.
- **Konsekvens:** `hideCompleted` döljer klara mål i panelen. Inga datum hittas på för gamla mål (null).

### D4. Målen är en DAG
- **Beslut:** Delmål kan ha flera föräldrar. Lagras platt med `childIds` och `rootIds` (schema 7).
- **Datum:** 2026-11-21
- **Varför:** Samma quest eller nivå krävs av många mål och ska bara göras/klaras en gång.
- **Konsekvens:** Flytt gäller en koppling under en förälder. Templates kopplar in befintliga mål med samma krav i stället för dubbletter.

### D5. Aggregerade mål
- **Beslut:** Aggregat räknar **direkta** delmål ("X av Y") och är ihopfällda som standard (panel och karta).
- **Datum:** 2026-11-21
- **Varför:** Stora listor (t.ex. alla CA:s) skulle annars dränka panelen.
- **Konsekvens:** Ett aggregat utan krav klaras när alla direkta delmål är klara.

### D6. Källor för kill count
- **Beslut:** Chattmeddelanden vinner alltid. `NpcLootReceived` räknar vanliga NPC:er. Hiscores används som startvärde (på som standard; Jagex förstapartstjänst via RuneLites `HiscoreClient`) och bara om de är högre. Manuella räknare för resten.
- **Datum:** 2026-11-21
- **Varför:** Chatten är exakt; hiscores släpar och kräver rank; loot är det enda för NPC:er utan KC-meddelande.
- **Konsekvens:** NPC-loot räknas bara från att ett mål följer NPC:n. Hiscores hämtas med cooldown (10 min efter login, 30 s efter nytt mål). Ingen tredjepartsvarning behövs eftersom tjänsten är Jagex egen.

### D7. Genererade templates före handplockade
- **Beslut:** Templates som kan genereras ur speldata (quests, diaries, skills, CA:s) byggs i kod; handplockade JSON-filer bara där det behövs. Katalogen listar genererade först.
- **Datum:** 2026-11-21
- **Varför:** Genererade är alltid kompletta och kräver ingen manuell verifiering.
- **Konsekvens:** Quest-kedjor kommer från `quest-requirements.json`.

### D8. Handplockad data verifieras av produktägaren
- **Beslut:** Data från OSRS Wiki skrivs in av utvecklaren, men verifieras av produktägaren (`verifiedBy`/`verifiedAt` i filen, checklista i `template-verification.md`). Osäkra uppgifter märks **(uncertain)**.
- **Datum:** 2026-11-23
- **Varför:** En agent kan läsa fel på wikin; spelaren ska kunna lita på siffrorna.
- **Konsekvens:** Ny handplockad data är inte "klar" förrän den har en sektion i verifieringslistan.

### D9. Templates: bara "Lås upp" och "Skaffa"
- **Beslut:** Handplockade templates har bara grenarna för att låsa upp och skaffa, inga krav för att bära/använda (wield). Quest cape räknar inte miniquests. Paket består bara av `template`-referenser.
- **Datum:** 2026-11-23
- **Varför:** Wield-krav dubblerar skill-mål och gör träden stora. Paket ska uppdateras automatiskt när deras delar ändras.
- **Konsekvens:** `QuestCapeExclusions` listar miniquests. Referenser löses i katalogen med cykelkontroll.

### D10. Egna templates och delning
- **Beslut:** Egna templates lagras globalt (alla konton). Delning bara via urklipp eller fil (`GG1:`-strängar), aldrig via en server. Import slår ihop och ersätter aldrig.
- **Datum:** 2026-11-23
- **Varför:** Inga tredjepartsservrar, ingen moderering, inget IP-läckage. Import får aldrig förstöra befintliga mål.
- **Konsekvens:** Import valideras strikt (`ImportValidator`, storleksgränser mot gzip-bomber). Export innehåller aldrig status, datum eller räknarvärden.

### D11. Kartan
- **Beslut:** Kartan är ett eget fönster med egen lagerlayout (förenklad Sugiyama, inga nya beroenden). Rötter överst, en rot åt gången som standard, egen utfällning skild från panelens.
- **Datum:** 2026-11-23
- **Varför:** Plugin Hub tillåter inte godtyckliga beroenden; sidopanelen är för smal för en graf.
- **Konsekvens:** Layouten är deterministisk och cachas (`MapLayoutCache`). Fönstrets position sparas i den dolda config-posten `mapWindowBounds`.

### D12. Dra och släpp bara bland syskon
- **Beslut:** En rad dras i sitt grepp och kan bara flyttas bland syskonen under samma förälderrad. Ordningen är prioritet i "Nästa mål". Esc eller släpp utanför avbryter.
- **Datum:** 2026-11-24
- **Varför:** Att byta förälder genom att dra är lätt att göra av misstag i en DAG; indrag/utdrag finns redan.
- **Konsekvens:** Egna muslyssnare, inte Swings DnD. Esc via key binding, inte `KeyboardFocusManager`.

### D13. Varje formatändring får en schemaversion
- **Beslut:** Varje ändring av sparformatet, även ett nytt valfritt fält eller en ny kravtyp, höjer `schemaVersion` och får ett migreringssteg med test.
- **Datum:** 2026-11-20
- **Varför:** En äldre pluginversion laddar då en nyare fil skrivskyddat i stället för att tyst kasta bort okända fält när den sparar.
- **Konsekvens:** Rådata sparas som `goalsBackupBeforeV<N>` före migrering och som `goalsCorruptBackup` om den inte går att läsa.

---

## Beslut hittade i koden och historiken

### D14. Automatisk avbockning är spärrad
- **Beslut:** Ett klart mål avmarkeras aldrig automatiskt.
- **Datum:** 2026-11-20
- **Varför:** Föremål förbrukas och XP-mål ska inte "ångras" (t.ex. barrows gloves som används).
- **Konsekvens:** Bara användaren kan avbocka. `countsAlso` och `doneWhen` finns för förbrukade resurser.

### D15. Profilbyten skyddas
- **Beslut:** All speldata bär sin RS-profil och ignoreras om en annan profil är laddad. Dialoger fångar profilen (`ProfileGuard`).
- **Datum:** 2026-11-23
- **Varför:** Modala dialoger låter EDT köra vidare; mål kunde annars hamna på fel konto.
- **Konsekvens:** Dialogresultat kastas tyst efter ett kontobyte.

### D16. Panelen byggs inte om vid varje spellägesändring
- **Beslut:** Bara de rader vars progress ändrats uppdateras; full ombyggnad bara när grafen ändras.
- **Datum:** 2026-11-23
- **Varför:** Prestanda; GameTick ger uppdateringar ofta.
- **Konsekvens:** `GoalManager` har en `revision` som UI:t jämför med.

### D17. Bank och seed vault cachas per profil
- **Beslut:** Bank och seed vault räknas från senast de var öppna och cachas i config per RS-profil. Runor i rune pouchen räknas via varbits.
- **Datum:** 2026-11-20 / 2026-11-21
- **Varför:** Containrarna går bara att läsa när de är öppna.
- **Konsekvens:** Item-mål kan visa gammal bankdata tills banken öppnas.

### D18. Ungefärliga drop rates
- **Beslut:** Drop-källor kan märkas `approximate` med en förklarande text (t.ex. raids där chansen beror på poäng).
- **Datum:** 2026-11-24
- **Varför:** Torrhetsmätaren ska inte ge falsk precision.
- **Konsekvens:** Schema 16.

### D19. Templates har innehållsversioner
- **Beslut:** Varje handplockad template har `version` som höjs vid ändring, och `replaces` för att visa bytta noder som ändringar.
- **Datum:** 2026-11-23
- **Varför:** "Update from template" ska kunna säga att något har ändrats och vad.
- **Konsekvens:** `templateVersion` sparas på template-roten (schema 13).
