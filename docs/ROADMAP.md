# Roadmap

Uppdateras när en gren mergas (se PROCESS.md). Idéer blir GitHub Issues; den här listan är
översikten, issues är sanningen för detaljer och status.

## Klart

Sammanfattat per milstolpe ur git-historiken (2026-11-20 – 2026-11-24).

| Milstolpe | Innehåll |
|---|---|
| M1–M4 Grund | Sidopanel med mål och delmål, kravtyper för skill, item (inkl. bank, seed vault, rune pouch), quest, diary och combat achievement, automatisk avbockning. |
| M5 Popup och ljud | Eget popup-overlay, spelets ljudeffekter, popup-nivå, hover-paus, XP-vinstmål, hover-detaljer. |
| M6 Completed | Completed-flik med historik, ≈-datum, statistik och "tog X dagar". |
| M7 Graf | Målen lagras som DAG, delade mål i panelen, räknarmål, total level, combat level och quest points. |
| M8 Templates | Template-motor och dialog (quest, diary, base level, Barrows gloves), "Collapse all", quest-databas med hela kedjor, uppdaterbara templates, kategorier, sök, wiki-länkar. |
| M9 Kill count | KC-mål med räknare från chatt, loot och hiscores, KC-dialog, radräknare, bossikoner. |
| M10 Drops | Drop-källor med torrhetsmätare, multi-drop, `doneWhen`-länkar, första ironman-templates, ikoner för mål utan krav. |
| M11 Karta | Lagerlayout, kartfönster, detaljer, "Show on map", pulsering vid klart mål, delad målmeny. Kvalitetsrunda (panelen byggs inte om varje ändring, ProfileGuard, migreringsfixturer). Plugin Hub-metadata. |
| M12 Templates våg 1 | Valparametrar, async-bygge, quest-serier, genererade templates för CA, diaries, skills; egna templates; delningssträngar för export/import. |
| M13 Templates våg 2 | Poängmål (varbits), template-versioner, 17 unlock-templates, verifieringschecklista, skilling outfits och capes. |
| M14 Templates våg 3 | Template-referenser, paket, förbrukade resurser (`countsAlso`, crystal armour), utrustnings-templates, ungefärliga drop rates, CoX/ToB/ToA, våg 3 i mid/late ironman-paketen, större template-dialog. **Klart.** |
| M15 Dra och släpp | Dra mål i ett grepp bland syskon, Esc avbryter (key binding). **Klart.** |

## Pågår

- Testperiod: produktägaren testar i spelet och rapporterar via issues.
- Processdokumentation (den här grenen `docs/process`).
- Verifiering av handplockad template-data (`template-verification.md`).

## Idéer

Blir issues i DEL 2. Ingen ordning än; prioritet sätts med etiketter.

1. **"Behövs för"-tooltips** – hover på föremål/quest i spelet visar vilka mål som behöver det.
2. **Inköps- och farmlista** – samlad lista över föremål och mängder som saknas för valda mål.
3. **Bankflik** – bankflik/markering för föremål som mål behöver.
4. **Markering av föremål på marken** – markera drops som något mål behöver.
5. **Mini-tracker** – litet overlay för ett fäst mål med progress.
6. **Collection log-integration** – läsa collection log som källa för item- och KC-mål.
7. **Tidsuppskattning (ETA)** – uppskattad tid kvar utifrån XP/h eller kills/h.
8. **"Vad kan jag göra nu?"** – mål vars alla krav redan är uppfyllda.
9. **Guide vid första start** – kort introduktion och förslag på templates.
10. **"Nyheter" efter uppdatering** – visa vad som är nytt efter en pluginuppdatering.
11. **Statistikdiagram** – klarade mål över tid.
12. **Skärmdump när ett mål klaras** – lokal skärmdump; Discord som opt-in med tredjepartsvarning.
13. **Äventyrsdagbok** – berättande logg över klarade mål.
14. **Verktyg för wiki-verifiering** – bara vid utveckling, jämför template-data mot wikin.
15. **Community-templates via PR** – `CONTRIBUTING.md` för att bidra med templates.
