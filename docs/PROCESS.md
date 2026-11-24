# Arbetsprocess

Hur Gielinor Goals utvecklas. Gäller alla sessioner, människor som agenter.
Tekniska detaljer finns i [ARCHITECTURE.md](ARCHITECTURE.md), beslut i [DECISIONS.md](DECISIONS.md).

## Roller

| Roll | Vem | Gör | Gör inte |
|---|---|---|---|
| Produktägare och testare | Sebas | prioriterar, fattar beslut, testar i spelet, verifierar wiki-data, godkänner merge och release | – |
| Design-chatt | Claude, egen chatt | diskuterar idéer, formulerar beslut till DECISIONS.md, skriver prompts | implementerar inget |
| Utvecklarsession | Claude Code, en per milstolpe/gren | implementerar enligt prompt, skriver tester, uppdaterar docs | pushar, skapar PR eller GitHub-objekt utan OK; påstår att något är klart före test i spelet |
| Granskning | `/code-review` eller egen session | granskar diffen mot AGENTS.md, DECISIONS.md och korrekthet | ändrar koden utan att fråga |
| Data och innehåll | Claude Code (`data/…`-gren) | templates och wiki-data, poster i `template-verification.md` | markerar data som verifierad |

## Flöde

1. **Idé** – från test, design-chatt eller användare.
2. **Issue** – med rätt mall och etiketter (status `behöver-beslut`).
3. **Diskussion** – i design-chatten eller i issuen.
4. **Beslut** – ny post i DECISIONS.md; issuen får status `redo`.
5. **Prompt** – skrivs av design-chatten med promptmallen nedan.
6. **Implementation** – i egen gren, en session (status `pågår`).
7. **Granskning** – `/code-review` eller granskningssession; fynd åtgärdas.
8. **Test i spelet** – produktägaren följer testlistan.
9. **Merge** – efter OK.
10. **Efterarbete** – ROADMAP.md och CHANGELOG.md uppdateras; issuen stängs.

## Grenar

- Namn: `feature/…`, `fix/…`, `data/…`, `docs/…` (kort, med bindestreck, t.ex. `feature/mini-tracker`).
- En session per gren, en gren per uppgift.
- Parallella sessioner får aldrig röra samma filer. Behöver de det används separata worktrees, och
  den andra grenen rebasas på den första efter merge.
- Lokala grenar tas bort när de har mergats och pushats.
- Pusha, skapa PR eller GitHub-objekt bara med produktägarens uttryckliga OK.
- Formatering blandas aldrig med funktionsändringar i samma commit.

## Definition of Done

- [ ] `./gradlew build` och alla tester gröna.
- [ ] Granskning gjord och fynden åtgärdade eller medvetet avfärdade.
- [ ] Testad av produktägaren i spelet enligt testlistan.
- [ ] ARCHITECTURE.md, DECISIONS.md och ROADMAP.md uppdaterade där det behövs.
- [ ] Ny eller ändrad template-data har en sektion i `template-verification.md`.
- [ ] Ändrat sparformat har ny schemaversion, migrering och test (D13).
- [ ] AGENTS.md-reglerna följda (trådar, gamevals, config-regler, inga förbjudna funktioner).
- [ ] CHANGELOG.md har en rad under "Ej släppt".

## Promptmall

```markdown
## Uppgift: <titel> (issue #<nr>)

### Bakgrund
Se docs/ARCHITECTURE.md och docs/DECISIONS.md (särskilt D<n>). <Bara det som inte står där.>

### Uppgift
<Vad som ska göras och varför. Vad som inte ska göras.>
Arbeta i grenen `<typ>/<namn>`. Commita, men pusha inte utan mitt OK.

### Delar (stanna för mitt OK efter varje del)
1. <del>
2. <del>

### Tester
<Vilka enhetstester som ska finnas. Migreringstest om formatet ändras.>

### När du är klar
- Redovisa ändrade filer och en sammanfattning.
- Lista oklarheter du hittat.
- Ge en testlista för spelet: golden path, kantfall, vad som inte ska hända.
- Påstå inte att något är klart förrän jag har testat i spelet.
```

## Release

### Versionsnumrering
`MAJOR.MINOR.PATCH`:
- **PATCH** – buggfixar och datarättningar.
- **MINOR** – nya funktioner och templates.
- **MAJOR** – ändringar som bryter användarens data eller arbetssätt (bör inte behövas tack vare migreringar).

Versionen skrivs i CHANGELOG.md och som git-tagg (`v1.2.0`) på release-commiten. Plugin Hub läser
inte versionsnumret – den bygger den commit-hash som anges.

### Steg
1. Allt som ska med är mergat till `main`; build och tester gröna.
2. Flytta "Ej släppt" i CHANGELOG.md till en ny versionsrubrik med datum. Commit `Release X.Y.Z`.
3. Tagga: `git tag vX.Y.Z` (push av `main` och taggen med OK).
4. Hämta commit-hashen: `git rev-parse HEAD`.
5. I forken av `runelite/plugin-hub`: ny gren, ändra `commit=` i `plugins/gielinor-goals` till hashen.
6. PR till `runelite/plugin-hub` med en kort lista över ändringar och attributionsraden:
   ```
   Generated-by: Claude Code
   ```
   PR:er till plugin-hub skapas i en separat chatt, med produktägarens OK.
7. När den är mergad: stäng issues som ingick, uppdatera ROADMAP.md.

## Dataverifiering

- Varje handplockad template har fälten `verifiedBy` och `verifiedAt` i sin JSON-fil. Tomma = inte verifierad.
- Utvecklaren skriver en sektion per template i `docs/template-verification.md`: wiki-länkar,
  siffror som används, och **(uncertain)** för allt som inte gick att bekräfta.
- Produktägaren kontrollerar mot OSRS Wiki, kryssar i "Verified" och fyller i
  `verifiedBy` (namn) och `verifiedAt` (`ÅÅÅÅ-MM-DD`) i filen, i en `data/…`-gren.
- Fel rapporteras med issue-mallen "Datafel". Rättningen höjer templatens `version`.
- När en template ändras efter verifiering töms `verifiedAt` och kryssrutan avmarkeras.
