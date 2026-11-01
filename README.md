# House Keeping

Your bank fills up with things you no longer need. House Keeping looks through your bank, suggests what can go, and tells you why.

House Keeping only makes suggestions. It never drops, moves or sells anything for you.

## What it suggests

- **Outgrown skilling items.** Low-tier supplies and tools you've moved past. For example, raw shrimps once you have 70+ Fishing and 70+ Cooking, or a bronze axe once you can use a rune one.
- **Obsolete quest items.** Leftover quest items that the OSRS Wiki says have no further use. An item is only suggested once *every* quest that needs it is complete.
- **Store in your POH.** Items that fit in your costume room: the armour case, cape rack, magic wardrobe, fancy dress box, toy box or treasure chest. Tell House Keeping which ones you've built in the *Player-owned house* section of its settings. For the treasure chest, also pick its tier, because the tier decides which clue rewards fit.
- **Junk / retrievable items.** Items with no remaining use, such as burnt food, and books your POH bookcase gives back for free.

Some suggestions are only a **maybe**: usually unneeded, but with a niche use, such as steel tools or the Collection log. They're hidden unless you turn on *Include 'maybe' suggestions*, and are marked with a **?** when shown.

## How it shows them

- **Dots.** Suggested items get a coloured dot in your bank and inventory, one colour per category. Hover over an item to see why it was suggested.
- **Side panel.** Click the broom icon to see every suggestion, grouped by category. Each item shows its quantity and value, using whichever is higher of its GE price and high alch value. The header shows how many bank slots you could free and what the suggestions are worth.
- **Search.** Type in the panel's search bar to find a suggestion by name or reason. It forgives typos, ignores spaces and capitals, accepts words in any order, and understands initials ("bdm" finds *Blue dragon mask*).
- **Bank tab.** With your bank open, press **Show in bank** in the panel to see only the suggested items in your bank. Use the **show** link on a group to see just that category. Press it again, or pick another bank tab, to go back. This uses RuneLite's built-in Bank Tags plugin, which must be enabled.
- **Ignoring.** Not interested in a suggestion? Click **✕** next to it in the panel, or right-click the item in your bank or inventory and choose *Ignore suggestion*. Ignored items move to the panel's *Ignored* group, where **↺** brings them back.

Open your bank once after installing so House Keeping can read it. After that it remembers your bank, even between sessions.

## Settings

- **Suggestions:** turn each category on or off, and choose whether to include 'maybe' suggestions.
- **Display:** turn the bank and inventory dots on or off, the right-click option and the slots counter. You can also change each category's colour.
- **Player-owned house:** your treasure chest tier, and which other costume room furniture you've built.

## Updating the item data

This section is for developers. The bookcase, quest item and POH storage lists are generated from the [OSRS Wiki](https://oldschool.runescape.wiki). To refresh them after a game update, run:

```
python scripts/generate_data.py --review
```

This rewrites `BookcaseBooks.java`, `QuestItems.java` and `PohStorage.java`. The `--review` flag prints the wiki sentence behind each quest item so you can check it. To hide a quest item, add it to `QUEST_DENY` in the script. To add one reported by players, add it to `QUEST_EXTRA`. The script needs a JDK, for `javap`, and a Gradle build run beforehand. The plugin itself never makes network requests.
