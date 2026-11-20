# Gielinor Goals

A goal and todo list for Old School RuneScape in the RuneLite sidebar. Goals tick themselves off from game data, so you
can plan your account without checking things off by hand. Great for ironmen, who have to get everything themselves.

## Features

- **Goals of every kind**
  - Manual todos for anything you like
  - Skill goals, by level (virtual levels up to 126) or by XP
  - Item goals, counting your inventory, equipment, bank and seed vault
  - Quests
  - Achievement diaries, with progress such as `7/11 tasks`
  - Combat achievement tasks
- **Automatic completion.** Goals are ticked off the moment you reach them, and a chat message tells you about it.
  Once a goal is done it stays done, even if you later use up the items.
- **Sub-goals and ordering.** Nest goals as deep as you want (for example *Fire cape* → *70 Ranged*, *43 Prayer*),
  reorder them and collapse groups.
- **Diary tasks as sub-goals.** Add a diary goal, open that diary's journal in game, and each task is added as a
  sub-goal. Tasks you have already done are ticked off.
- **Only what is left.** When you add a goal, the quests, diary tiers and combat achievements you have already completed
  are hidden, and you cannot add the same goal twice.
- **Next goals overlay.** An optional overlay shows your next few goals and how close you are.
- **One list per account.** Every RuneScape account (and game mode) has its own list, stored in your RuneLite profile.

## Usage

1. Open the **Gielinor Goals** panel from the sidebar and log in.
2. Click **Add goal...** and pick a type: skill level, skill XP, item, quest, achievement diary or combat achievement.
   You can also type a quick note-style goal in the field below it and press Enter.
3. Right-click a goal (or click `...`) to add sub-goals, edit, reorder, or delete it.
4. Open your bank once so banked items are counted. The bank is remembered between sessions.
5. For diary goals, open that diary's journal in game to add its tasks as sub-goals.

### Settings

| Setting | Default | Description |
|---|---|---|
| Hide completed goals | Off | Hide goals that are ticked off, including their sub-goals |
| Chat message on completion | On | Show a chat message when a goal is completed automatically |
| Show next goals overlay | On | Show your next goals in the game view |
| Goals in overlay | 3 | How many upcoming goals the overlay shows |

## How it works

All data comes from the game client itself. The plugin makes no network requests.

- Skills and items come from RuneLite events (`StatChanged`, `ItemContainerChanged`). The bank and seed vault can only be
  read while they are open, so their contents are cached for each account.
- Quests, diaries and combat achievements are read from game variables. The list of combat achievement tasks is read
  from the game cache, so it stays up to date with the game.
- Diary tasks are read from the journal text when you open it, because the task texts are not part of the game cache.

## Known limitations

- Items deposited through a deposit box or bank chest are counted the next time you open a normal bank.
- Diary task sub-goals update when you open the journal, while the diary progress counter updates right away.

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
