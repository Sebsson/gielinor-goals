# Gielinor Goals

Plan your Old School RuneScape account in the RuneLite sidebar. Goals tick themselves off from game data, templates
lay out long grinds for you, and a goal map shows how everything connects. Made with ironmen in mind, useful for any
account.

<!-- SCREENSHOT: the sidebar panel with a few templates added, one goal expanded and a progress bar visible -->

## Features

- **Goals of every kind, ticked off automatically**
  - Skills: a level (virtual levels up to 126), an amount of XP, or XP gained from now
  - Items, counting your inventory, equipment, bank, seed vault and rune pouch, optionally with variants such as
    charged and uncharged versions
  - Quests, achievement diaries (with progress such as `7/11 tasks`) and combat achievement tasks
  - Total level, combat level and quest points
  - Kill counts: bosses, clues and minigames from the game's own chat messages and the official hiscores, other NPCs
    counted from their loot, and manual counters you count yourself
  - Manual goals for anything else
- **Goals inside goals.** Nest goals as deep as you like. A goal can sit under several parents, for example a quest
  needed for both Barrows gloves and the Quest cape, and is stored once. Show a goal as a counter (`142/170`) to see
  how many of its sub-goals are done.
- **Templates.** Add a whole plan in one go:
  - Quest cape, Achievement diary cape and Base N (every skill to a level you choose)
  - Ironman templates: Barrows gloves, Bow of Faerdhinen with crystal armour, Dragon warhammer, Doom of Mokhaiotl,
    The Hueycoatl, Fire cape, Infernal cape, Dragon defender, Fighter torso, Ava's assembler, Graceful, Rune pouch,
    Imbued god cape, Zenyte jewellery, and Ardougne cloak with fairy rings
  - Optional full quest chains: every prerequisite quest, skill level and quest point requirement is added too
  - Goals you already have are linked in instead of copied, and parts you have already done are ticked off quietly
  - Search by name or nickname (`bowfa`, `dwh`, `huey`, `torso` …), grouped by early, mid and late game
  - **Update from template** offers what a newer version of a template adds
- **Dry-streak meter.** Item goals can have a drop source. While you hunt, the goal shows your kill count against the
  drop rate (`1,240 kc · 1.6× rate`) and the chance you would have had it by now. When it drops, the kill count is
  saved.
- **Completion popup and sound.** A popup and an optional game sound tell you when a goal is done, with a separate
  sound for main goals.
- **Completed tab.** Everything you have finished, grouped by date, with how long each goal took.
- **Goal map.** A separate window that draws your goals as a graph: shared goals once with several arrows, collapsible
  groups, pan and zoom, search, and highlighting of everything a goal needs and unlocks. Click a goal for its
  details, double-click to edit, right-click for the same menu as in the panel.
- **Next goals overlay.** An optional overlay shows your next few goals and how close you are.
- **Open on wiki.** Most goals link to their OSRS Wiki page.
- **One list per account.** Every RuneScape account and game mode has its own goals, stored in your RuneLite profile.

<!-- SCREENSHOT: the template dialog with the ironman templates and the "Show full quest chains" box -->
<!-- SCREENSHOT: the goal map for Bow of Faerdhinen with a shared quest highlighted and the details panel open -->
<!-- SCREENSHOT: a completion popup in game -->

## Getting started

1. Open the **Gielinor Goals** panel from the sidebar and log in.
2. Click **Add from template...** and pick a plan, for example *Barrows gloves* or *Quest cape*, or click
   **Add goal...** for a single goal. Type in the field below to add a quick manual goal.
3. Open your bank once so banked items are counted. The bank is remembered between sessions.
4. Right-click a goal (or click `...`) to add sub-goals, edit, link it under another goal or show it on the map.
5. Click **Map** at the top of the panel to see how your goals connect.

For diary goals, open that diary's journal in game and each task is added as a sub-goal.

### Settings

| Setting | Default | Description |
|---|---|---|
| Hide completed goals | Off | Hide goals that are ticked off, including their sub-goals |
| Chat message on completion | On | Show a chat message when a goal is ticked off automatically |
| Show next goals overlay | On | Show your next goals in the game view |
| Goals in overlay | 3 | How many upcoming goals the overlay shows (1–10) |
| Popup | All goals | Which automatically completed goals show a popup: all, only main goals, or none |
| Popup duration | 4 s | How long each popup stays on screen (2–10 s) |
| Play sound | On | Play a game sound with each popup, at the in-game sound effect volume |
| Sub-goal sound / Main goal sound | Trophy ding / Fanfare | The sounds for sub-goals and top-level goals |
| Show time to complete | On | Show how many days each goal took, when both dates are known |
| Fetch from hiscores | On | Look up kill counts, clues and activities on the official OSRS hiscores |

## How it works

Almost all data comes from the game client itself. The only network request is the kill count lookup on the official
OSRS hiscores, made through RuneLite's own hiscore client straight to Jagex's servers. It runs after login and when
you add a kill count goal, and can be turned off with *Fetch from hiscores*.

- Skills and items come from RuneLite events. The bank and seed vault can only be read while they are open, so their
  contents are cached per account. The rune pouch is read from its game variables.
- Quests, diaries and combat achievements are read from game variables. The list of combat achievement tasks is read
  from the game cache, so it stays up to date with the game.
- Kill counts come from the game's chat messages, the hiscores and NPC loot. The chat message always wins; the
  hiscores lag behind and are only used when they are higher.
- Diary tasks are read from the journal text when you open it, because the task texts are not part of the game cache.

## Data sources

Template contents and quest requirements were compiled by hand from the
[Old School RuneScape Wiki](https://oldschool.runescape.wiki): requirements, item amounts, drop rates and quest
prerequisites. Each template file names the wiki pages it is based on and the date they were read. The plugin ships
this data as static files (`quest-requirements.json` and the template files) and downloads nothing while it runs.

The OSRS Wiki's content is available under the
[Creative Commons Attribution-NonCommercial-ShareAlike 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/)
license. Item, NPC and quest names are those of the game, © Jagex.

## Known limitations

- Items deposited through a deposit box or bank chest are counted the next time you open a normal bank.
- Diary task sub-goals update when you open the journal, while the diary progress counter updates right away.
- NPC kill counts go up when an NPC drops loot, so kills of NPCs that sometimes drop nothing are not all counted.
- The quest requirement data does not cover every quest yet; quests without data are added without their chain.

## Development

Requires a JDK (11 or newer).

```
./gradlew run
```

This starts a RuneLite development client with the plugin enabled. See
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to log in.

Run the unit tests with:

```
./gradlew test
```

## License

BSD 2-Clause, see [LICENSE](LICENSE).
