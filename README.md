# House Keeping

Your bank fills up with things you no longer need. House Keeping looks at your bank and suggests what can go, and explains why:

- **Outgrown skilling items.** Low-tier supplies and tools you've moved past, like raw shrimps when you have 99 Fishing.
- **POH storage.** Items that fit in storage furniture you've already built in your player-owned house.
- **Obsolete quest items.** Quest items whose quests you've finished and that nothing else needs.
- **Junk or retrievable items.** Items with no remaining use, or items you can get back for free, such as books from your POH bookcase.

House Keeping only makes suggestions. It never drops, moves, or sells anything for you.

## Updating the item data

The bookcase and quest item lists are generated from the [OSRS Wiki](https://oldschool.runescape.wiki). To refresh them after a game update, run:

```
python scripts/generate_data.py --review
```

This rewrites `BookcaseBooks.java` and `QuestItems.java`. The `--review` flag prints the wiki sentence behind each quest item so you can check it. To hide an item, add it to `QUEST_DENY` in the script. To add an item reported by players, add it to `QUEST_EXTRA`. The script needs a JDK, for `javap`, and a Gradle build run beforehand. The plugin itself never makes network requests.
