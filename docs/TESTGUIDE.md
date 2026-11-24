# Testguide – Gielinor Goals

Manuell testning i spelet. Knapp- och menynamn är skrivna exakt som i pluginet (engelska).
"⋮" betyder radens knapp **More options** (eller högerklick på raden).

Kryssa i medan du testar. Skriv en issue med mallarna längst ner för allt som inte stämmer.

---

## 0. Förberedelser

- [ ] **Backup av sparfilen.** Målen sparas i RuneLites config, grupp `gielinorgoals`, per RS-profil
      (nyckeln `goals`). Egna templates ligger globalt (`customTemplates`). Filerna finns i
      `%USERPROFILE%\.runelite\profiles2\` (en `.properties`-fil per RuneLite-profil). Kopiera hela mappen.
- [ ] **Backup i pluginet också:** panelen → **Import and export goals** → **Export all goals...** →
      **Save as file...**. Då har du en `GG1:`-sträng att importera om något går fel.
- [ ] Stäng alla RuneLite-klienter. Starta dev-klienten från repots rot:
      ```powershell
      ./gradlew run
      ```
- [ ] Logga in med Jagex-kontot enligt
      [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
      (dev-klienten behöver `--insecure-write-credentials` från den vanliga launchern en gång).
- [ ] Öppna banken en gång, så att föremål i banken räknas (panelen visar annars
      *"Open your bank once so banked items are counted."*).

---

## 1. Mål och kravtyper

- [ ] Skriv en titel i **Quick add, press Enter** och tryck Enter → ett manuellt mål läggs till som rot.
- [ ] **Add goal...** → **What kind of goal?** Testa varje typ:
  - [ ] **Skill level**: välj skill och nivå under din nivå → målet klaras direkt och tyst (redan uppfyllt när det lades till).
  - [ ] **Skill XP** med **Count XP gained from now** → progress startar på 0 och ökar när du tränar.
  - [ ] **Item**: sök t.ex. ett föremål du har i banken. Testa **Also count variants (e.g. charged/uncharged)**.
  - [ ] **Quest**, med och utan **Add the quest's requirements as sub-goals**.
  - [ ] **Achievement diary**, **Combat achievement** (listorna döljer det du redan gjort när du är inloggad).
  - [ ] **Total level**, **Combat level**, **Quest points**.
  - [ ] **Kill count**, **Activity points**, **Manual** (se §5–6).
- [ ] ⋮ → **Add sub-goal**, **Edit goal**, **Delete goal**, **Move up**/**Move down**,
      **Make sub-goal of goal above**, **Move out one level**.
- [ ] ⋮ → **Mark as done** / **Mark as not done** på ett manuellt mål.
- [ ] Föremål i rune pouchen och i equipment räknas till item-mål.
- [ ] Ett klart item-mål blir **inte** ogjort när du gör av med föremålet.
- [ ] **Collapse all** fäller ihop alla mål med delmål; **Hide completed** döljer klara.

**Kända begränsningar:** bank och seed vault räknas från senast de var öppna. Ett klart mål avbockas
aldrig automatiskt.

**Utforskande frågor:** Hittade du rätt kravtyp direkt? Var det tydligt vad "Skill XP" mäter? Saknades någon kravtyp?

---

## 2. Popup och ljud

- [ ] Klara ett **delmål** (t.ex. ett skill-mål en nivå över din) → popup högst upp i mitten och
      **Sub-goal sound** spelas.
- [ ] Klara ett **rotmål** → **Main goal sound**.
- [ ] Håll musen över popupen → den stannar kvar.
- [ ] Klara ett mål och dess förälder samtidigt (förälder med ett enda delmål) → bara **en** popup, förälderns.
- [ ] **Mark as done** för hand → ingen popup.
- [ ] Logga ut, klara inget, logga in → ingen popup för redan klara mål.
- [ ] Config **Popup**: **All goals** / **Only main goals** / **Off**. **Popup duration**, **Play sound**.
- [ ] **Chat message on completion** ger ett chattmeddelande.

**Kända begränsningar:** mål som klaras de första sekunderna efter inloggning räknas som offline (ingen popup, ≈-datum).

**Utforskande frågor:** Kändes popupen belönande eller störande? Var ljuden lagom? Rätt antal popups vid stora templates?

---

## 3. Completed

- [ ] Fliken **Completed** visar klara mål; klara mål ligger kvar nedtonade i **Active**.
- [ ] **Sort by completion date**: **Newest first** / **Oldest first**.
- [ ] **Show only one kind of goal**, **Only main goals**, **Hide sub-goals**.
- [ ] Mål som var klara när de lades till visas som ≈ / *"Completed before tracking / found at login"*.
- [ ] Hover visar "tog X dagar" när båda datumen finns. Config **Show time to complete** stänger av det.
- [ ] **Show in tree** hoppar till målet i Active. **Open on wiki** öppnar wikin.
- [ ] **Mark as not done** på ett mål som fortfarande är uppfyllt → texten *"(still met in game)"*.

**Utforskande frågor:** Är det tydligt att Completed är en logg och att målen finns kvar? Är ≈-datumen begripliga?

---

## 4. Delade mål och aggregat

- [ ] ⋮ → **Link to another goal...** → välj ett mål → det visas under båda föräldrarna.
- [ ] Hover visar **Also under:** med den andra föräldern.
- [ ] Klara det delade målet → klart under båda.
- [ ] ⋮ → **Remove from …** tar bort kopplingen bara under den föräldern; **Delete goal** tar bort målet överallt.
- [ ] ⋮ → **Show as counter** på en förälder → "X/Y", ihopfälld.
- [ ] Aggregat utan eget krav klaras när alla direkta delmål är klara.
- [ ] Lägg till två templates som delar en quest → *"You already have this goal; it is linked in instead of copied"*.

**Utforskande frågor:** Förstod du skillnaden mellan Remove och Delete? Blev det rörigt med samma mål på flera ställen?

---

## 5. Kill count och räknare

- [ ] **Add goal...** → **Kill count** → sök en boss → *"Fetched from the hiscores after you add the goal."* → värdet fylls i.
- [ ] Döda bossen → KC från chattmeddelandet uppdaterar räknaren (vinner över hiscores).
- [ ] **Other NPC (by name)...** (t.ex. en vanlig NPC) → räknas upp ett per loot.
- [ ] **Manual counter...** → radens **Count one more** / **Count one less** och ⋮ → **Set value...**.
- [ ] Två mål på samma boss läser samma räknare.
- [ ] Config **Fetch from hiscores** av → inget hämtas.

**Kända begränsningar:** NPC-KC räknas bara från att målet skapas. Hiscores kräver rank och hämtas
högst var tionde minut efter inloggning (30 s efter ett nytt KC-mål).

**Utforskande frågor:** Var startvärdet rätt? Märktes det var värdet kom ifrån?

---

## 6. Poäng

- [ ] **Add goal...** → **Activity points** → t.ex. Slayer points → nuvarande värde visas.
- [ ] Värdet uppdateras när poängen ändras i spelet.

**Utforskande frågor:** Saknas någon aktivitet med poäng?

---

## 7. Torrhetsmätare

- [ ] Item-mål med **Drops from (optional, shows how dry you are)** och **Drop rate: 1 in** → raden visar hur torr du är.
- [ ] Mål på flera föremål (t.ex. 4 shards) räknas om till antal drops.
- [ ] Templates med raids visar ungefärlig rate med förklaring.
- [ ] När målet klaras sparas KC:n då det klarades.

**Utforskande frågor:** Var torrhetstexten begriplig? Motiverande eller deprimerande?

---

## 8. Templates

- [ ] **Add from template...** → sök med **Search templates and tags**, bläddra i kategorier.
- [ ] **Genererade:** Quest cape (inga miniquests), Achievement diary cape, Max cape, Base level, skill-stegar, CA, quest-serier.
- [ ] **Show full quest chains** på/av ändrar förhandsvisningen.
- [ ] **Preview**: avbocka en gren (*"Untick to leave out the whole template"*) → den kommer inte med.
- [ ] **Handplockade:** t.ex. Fire cape, Barrows gloves, Graceful, en raid.
- [ ] **Paket:** early/mid/late ironman, All capes → delar kopplas ihop, inga dubbletter.
- [ ] **Add as goals** → målen läggs till under en ny rot.
- [ ] **Egna:** ⋮ → **Save as template...** → finns i dialogen; **Edit name, description and tags...**,
      **Duplicate**, **Delete...**. Logga in på ett annat konto → den egna templaten finns kvar.
- [ ] **Uppdatering:** ⋮ → **Update from template...** → *"This goal is up to date with its template."*
      eller listorna **New in the template** / **No longer in the template** / **Changed in the template** → **Update**.
- [ ] **Open on wiki** finns på mål från templates.

**Kända begränsningar:** handplockad data är inte verifierad än (se `template-verification.md`, issue #16).
Inga wield-krav.

**Utforskande frågor:** Hittade du det du letade efter? Blev träden för stora? Saknades någon template?

---

## 9. Export och import

- [ ] ⋮ → **Export goal** → **Copy share string** / **Save as file...**.
- [ ] **Import and export goals** → **Export all goals...**.
- [ ] **Import...** → klistra in strängen eller **Choose file...** → **Next** → **Import** → målen läggs till
      och befintliga mål ersätts aldrig.
- [ ] Exporterade mål har inga klar-status, datum eller räknarvärden.
- [ ] Klistra in skräptext → begripligt fel, inget importeras.
- [ ] I template-dialogen: **Share template** och import av en template-sträng.

**Utforskande frågor:** Var det tydligt vad som skulle hända vid import? Saknade du "ersätt allt"?

---

## 10. Karta

- [ ] **Map** (eller ⋮ → **Show on map**) öppnar fönstret *"Gielinor Goals - Map"*.
- [ ] **Which goal to map**: en rot åt gången som standard, **All goals** visar allt.
- [ ] Rötter överst; delade mål ritas en gång med flera kanter.
- [ ] Aggregat är ihopfällda; fäll ut på kartan → panelen ändras inte.
- [ ] **Hide completed**, **Fit to window**, **Search, press Enter** (Enter igen = nästa träff).
- [ ] Klicka på en nod → detaljer (**Progress**, **Part of**, **Note**, **Edit**, **Open on wiki**).
- [ ] Klara ett mål med kartan öppen → noden pulserar. Logga in på ett annat konto → ingen pulsering.
- [ ] Stäng och öppna → fönstret har samma storlek och plats.

**Utforskande frågor:** Gav kartan överblick? Var stora kartor läsbara? Används den, eller är panelen nog?

---

## 11. Dra och släpp

- [ ] Dra en rad i greppet (*"Drag to change the order"*) bland syskonen → en linje visar platsen, ordningen sparas.
- [ ] Det går inte att släppa under en annan förälder (ingen linje visas).
- [ ] **Esc** under dragning, eller släpp utanför → inget ändras.
- [ ] Dra nära kanten → listan scrollar.
- [ ] Nya ordningen ändrar **Next goals** i overlayn.

**Utforskande frågor:** Hittade du greppet? Saknade du att kunna dra till en annan förälder?

---

## 12. Overlays och config

- [ ] **Show next goals overlay** och **Goals in overlay** (antal).
- [ ] Högerklicka på popupen medan den visas → **Show on map** öppnar kartan på målet. Next goals-overlayn har bara **Configure**.
- [ ] Gå igenom alla config-poster: **Hide completed goals**, **Chat message on completion**,
      Notifications, Completed (**Show time to complete**), Kill count (**Fetch from hiscores**).
- [ ] Stäng av och slå på pluginet → inga popups, inga dubbletter, samma mål.

**Utforskande frågor:** Var standardvärdena rätt? Saknade du någon inställning?

---

## Feedback

Skriv en issue med rätt mall:
[Bugg](https://github.com/Sebsson/gielinor-goals/issues/new?template=1-bugg.yml) ·
[Friktion](https://github.com/Sebsson/gielinor-goals/issues/new?template=2-friktion.yml) ·
[Idé](https://github.com/Sebsson/gielinor-goals/issues/new?template=3-ide.yml) ·
[Template-önskemål](https://github.com/Sebsson/gielinor-goals/issues/new?template=4-template.yml) ·
[Datafel](https://github.com/Sebsson/gielinor-goals/issues/new?template=5-datafel.yml)

Snabbanteckning under testet (för över till issues efteråt):

```
Typ: Bugg / Friktion / Idé
Område: (t.ex. Karta)
Vad hände / vad var krångligt / idén:
Förväntat / förslag:
Steg:
Kontotyp och version:
```
