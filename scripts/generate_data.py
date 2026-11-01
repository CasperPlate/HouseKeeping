#!/usr/bin/env python3
"""
Regenerates House Keeping's wiki-derived data classes:

  src/main/java/com/housekeeping/data/BookcaseBooks.java
  src/main/java/com/housekeeping/data/QuestItems.java
  src/main/java/com/housekeeping/data/PohStorage.java

Run by hand when new content is released; it is not part of the plugin build and the plugin
itself never touches the network. Needs Python 3.8+ and a JDK (for javap) on PATH or JAVA_HOME,
plus a runelite-api jar in the Gradle cache (run ./gradlew build once first).

    python scripts/generate_data.py            # regenerate both files
    python scripts/generate_data.py --review   # also print the wiki sentence behind every quest item

Quest items come from two sources: the wiki's "Quest items" category, where the page must say the
item has no further use, and the Wise Old Man's Recycling Centre list, which is the game's own list of
quest leftovers. Item ids are matched to net.runelite.api.gameval.ItemID constants by number, and quest
names to net.runelite.api.Quest constants, so the generated Java never contains magic numbers or guesses.
"""

import argparse
import glob
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(ROOT, "src", "main", "java", "com", "housekeeping", "data")
API = "https://oldschool.runescape.wiki/api.php"
USER_AGENT = "HouseKeeping RuneLite plugin data generator (github.com/CasperPlate)"

# ---------------------------------------------------------------------------------------------
# Hand-maintained lists. Edit these, then rerun the script.
# ---------------------------------------------------------------------------------------------

# Bookcase books that still do something besides being read; suggested only as a 'maybe'.
BOOK_MAYBE = {
	"COLLECTION_LOG",
	"BRAIN_INV_SARADOMIN_BOOK",
	"ELEMENTAL_WORKSHOP_SHIELD_BOOK",
	"ELEMENTAL_WORKSHOP_HELM_BOOK",
}

# Quest items never to suggest, by wiki page title, even when the wiki says they have no use.
QUEST_DENY = {
	"Arder-musca poison",  # wording matched a changelog entry; used against Duke Sucellus
	"Bone key (Shilo Village)",  # still opens Rashiliyia's Tomb
	"Coin purse",  # wording matched NPC dialogue, not the item's usefulness
	"Eagle feather",  # makes eagle capes
	"Enchanted symbol",  # wiki wording is ambiguous
	"Energy disk",  # wording matched a game message
	"Frozen bucket",  # thaws back into a bucket
	"Frozen jug",  # thaws back into a jug
	"Red banana",  # edible
	"Sole",  # wording is about the statuette, not the sole
	# The plain, drinkable rum. Its page calls the banana-stuffed rum useless, but that's a separate
	# item ("Karamjan rum (banana)") with its own page, which this entry doesn't affect
	"Karamjan rum",
	"Karambwanji paste",  # wording is about one useless version of the paste
	"Sithik portrait",  # wording is about the bad portrait, a different item
}

# Quest items to suggest even though the wiki does not say they are useless, by wiki page title.
# Use this for items reported by players; the quests are still read from the wiki infobox.
QUEST_EXTRA = {
}

# Wiki wording that marks an item as having no use left once its quests are done. "Used only in" is
# the wiki's standard opening for single-quest items; "only be used in <place>" deliberately doesn't match.
NO_USE = re.compile(
	r"(no (other |further |more |real |practical )?(use|purpose)"
	r"|(has|serves) no (other |further )?(use|purpose)"
	r"|useless after"
	r"|(is|are|becomes?) (essentially |virtually )?useless"
	r"|a useless item"
	r"|no longer (has a |serves a |of any )?(use|purpose|needed)"
	r"|not needed after"
	r"|(quest item|item that is|an item) (only used|used only) (in|during)"
	r"|is only used in the [^.]* quest)",
	re.I)


# ---------------------------------------------------------------------------------------------
# RuneLite constants
# ---------------------------------------------------------------------------------------------

def find_api_jar():
	cache = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1/net.runelite/runelite-api")
	jars = [j for j in glob.glob(os.path.join(cache, "*", "*", "runelite-api-*.jar")) if "sources" not in j]
	if not jars:
		sys.exit("runelite-api jar not found in the Gradle cache; run ./gradlew build first")

	def version(path):
		return [int(p) for p in re.findall(r"\d+", os.path.basename(path))]

	return max(jars, key=version)


def javap(jar, cls):
	java_home = os.environ.get("JAVA_HOME")
	exe = os.path.join(java_home, "bin", "javap") if java_home else "javap"
	return subprocess.run([exe, "-cp", jar, "-constants", cls], check=True, capture_output=True, text=True,
		timeout=120).stdout


def load_constants():
	jar = find_api_jar()
	print("Using", jar, file=sys.stderr)
	items = {}
	for name, value in re.findall(r"int ([A-Z0-9_]+) = (\d+);", javap(jar, "net.runelite.api.gameval.ItemID")):
		items.setdefault(int(value), name)
	quests = re.findall(r"Quest ([A-Z0-9_]+);", javap(jar, "net.runelite.api.Quest"))
	return items, quests


def quest_matcher(quests):
	def norm(s):
		return re.sub(r"[^A-Z0-9]", "", s.upper().replace("&", ""))

	by_norm = {norm(q): q for q in quests}

	def match(wiki_name, loose=False):
		n = norm(wiki_name)
		for candidate in (n, "THE" + n, re.sub(r"QUEST$", "", n)):
			if candidate in by_norm:
				return by_norm[candidate]
		if not loose:
			return None
		# e.g. "Desert Treasure II" or "Freeing Skrach Uglogwee" against a longer enum name. Only safe
		# where the wiki text is known to link quests, never for free text that also links places.
		partial = [q for k, q in by_norm.items() if n and (k.startswith(n) or k.endswith(n.replace("FREEING", "")))]
		return partial[0] if len(partial) == 1 else None

	return match


# ---------------------------------------------------------------------------------------------
# Wiki access
# ---------------------------------------------------------------------------------------------

def api(**params):
	params.update(format="json", formatversion=2)
	request = urllib.request.Request(API + "?" + urllib.parse.urlencode(params), headers={"User-Agent": USER_AGENT})
	with urllib.request.urlopen(request, timeout=60) as response:
		return json.load(response)


def page_texts(titles):
	texts = {}
	titles = list(titles)
	for i in range(0, len(titles), 50):
		data = api(action="query", prop="revisions", rvprop="content", rvslots="main", redirects=1,
			titles="|".join(titles[i:i + 50]))
		back = {r["to"]: r["from"] for r in data["query"].get("redirects", [])}
		back.update({n["to"]: n["from"] for n in data["query"].get("normalized", [])})
		for page in data["query"]["pages"]:
			if "revisions" in page:
				title = back.get(page["title"], page["title"])
				texts[back.get(title, title)] = page["revisions"][0]["slots"]["main"]["content"]
		time.sleep(0.5)
	return texts


def category_texts(category):
	texts = {}
	cont = {}
	while True:
		data = api(action="query", generator="categorymembers", gcmtitle=category, gcmlimit=50, gcmnamespace=0,
			prop="revisions", rvprop="content", rvslots="main", **cont)
		for page in data.get("query", {}).get("pages", []):
			if "revisions" in page:
				texts[page["title"]] = page["revisions"][0]["slots"]["main"]["content"]
		if "continue" not in data:
			return texts
		cont = data["continue"]
		time.sleep(0.5)


def infobox(text):
	start = text.find("{{Infobox Item")
	if start < 0:
		return ""
	box = text[start:start + 8000]
	end = box.find("\n}}")
	return box[:end] if end > 0 else box


def fields(box, name):
	return [v.strip() for v in re.findall(r"\|\s*" + name + r"\d*\s*=\s*([^\n]*)", box)]


def item_ids(box):
	return sorted({int(x) for f in fields(box, "id") for x in re.split(r"[ ,]+", f) if x.isdigit()})


def resolve_pages(names):
	"""Fetch item pages, keeping track of which version a redirect such as 'Agility cape(t)' points at."""
	resolved = {}
	texts = {}
	names = list(names)
	for i in range(0, len(names), 50):
		batch = names[i:i + 50]
		data = api(action="query", prop="revisions", rvprop="content", rvslots="main", redirects=1,
			titles="|".join(batch))
		normalized = {n["from"]: n["to"] for n in data["query"].get("normalized", [])}
		redirects = {r["from"]: (r["to"], r.get("tofragment")) for r in data["query"].get("redirects", [])}
		for name in batch:
			title = normalized.get(name, name)
			resolved[name] = redirects.get(title, (title, None))
		for page in data["query"]["pages"]:
			if "revisions" in page:
				texts[page["title"]] = page["revisions"][0]["slots"]["main"]["content"]
		time.sleep(0.5)
	return resolved, texts


def versioned_ids(text, name, version):
	"""Item ids for one item page, narrowed to a single version of a multi-version page when possible."""
	versions = {}
	for key, num, value in re.findall(r"\|\s*(version|name|id)(\d*)\s*=\s*([^\n]*)", infobox(text)):
		versions.setdefault(num, {})[key] = value.strip()
	for v in versions.values():
		v["ids"] = [int(x) for x in re.split(r"[ ,]+", v.get("id", "")) if x.isdigit()]

	numbered = [v for num, v in versions.items() if num]
	if not numbered:
		return versions.get("", {}).get("ids", [])
	for wanted in (version, name):
		if wanted:
			hits = [v for v in numbered if wanted.lower() in ((v.get("version") or "").lower(), (v.get("name") or "").lower())]
			if hits:
				return [i for v in hits for i in v["ids"]]
	# No single version named: every version is the same item in another colour, gender or worn state
	return sorted({i for v in numbered for i in v["ids"]} | set(versions.get("", {}).get("ids", [])))


def plain(wikitext):
	return re.sub(r"\[\[(?:[^\]|]*\|)?([^\]]*)\]\]", r"\1", wikitext)


# ---------------------------------------------------------------------------------------------
# Generators
# ---------------------------------------------------------------------------------------------

def write(name, body):
	path = os.path.join(DATA_DIR, name)
	with open(path, "w", encoding="utf-8", newline="\n") as f:
		f.write(body)
	print("Wrote", os.path.relpath(path, ROOT), file=sys.stderr)


def quest_ref(quest):
	return "Quest." + quest if quest else "null"


def generate_books(items, match_quest):
	wikitext = api(action="parse", page="Book", prop="wikitext")["parse"]["wikitext"]
	table = wikitext[wikitext.find("==Books in bookcase=="):]
	table = table[:table.find("|}")]

	rows = []
	for row in table.split("|-")[1:]:
		cells = [c.lstrip("|").strip() for c in row.strip().split("\n|") if c.strip()]
		page = re.search(r"\{\{plinkt\|([^|}]+)", cells[0])
		if page:
			rows.append((page.group(1), cells[2] if len(cells) > 2 else ""))

	texts = page_texts(sorted({page for page, _ in rows}))
	lines = []
	book_consts = set()
	for page, source in rows:
		ids = item_ids(infobox(texts.get(page, "")))
		if not ids:
			print("  skipped book without item id:", page, file=sys.stderr)
			continue
		quest = None
		for link in re.findall(r"\[\[([^\]|#]+)", source):
			quest = match_quest(link)
			if quest:
				break
		for item_id in ids:
			const = items.get(item_id)
			if not const:
				print("  skipped book id without ItemID constant:", page, item_id, file=sys.stderr)
				continue
			fn = "maybe" if const in BOOK_MAYBE else "add"
			lines.append("\t\t%s(ItemID.%s, %s);" % (fn, const, quest_ref(quest)))
			book_consts.add(const)

	write("BookcaseBooks.java", BOOKS_TEMPLATE.replace("$ENTRIES", "\n".join(lines)))
	return book_consts


def recycling_items(items, match_quest):
	"""
	Items the Wise Old Man's Recycling Centre removes from the bank once their quest is complete: the
	game's own list of quest leftovers, so no wording check is needed. Returns {ItemID constant: quests}.
	"""
	wikitext = api(action="parse", page="Wise Old Man/Recycling Centre", prop="wikitext")["parse"]["wikitext"]
	rows = []
	for row in wikitext.split("|-")[1:]:
		cells = [c.lstrip("|").strip() for c in row.strip().split("\n|") if c.strip()]
		link = ITEM_LINK.search(cells[0]) if len(cells) >= 2 else None
		if not link:
			continue
		# "Bone in vinegar#Goblin skull" names one version of a multi-version page
		page, _, version = link.group(1).strip().partition("#")
		pic = re.search(r"pic=[^|}(]*\(([^)]*)\)", link.group(2) or "")
		quest = plain(cells[1]).split("|")[-1].strip()
		rows.append((page, version or (pic.group(1) if pic else None), quest))

	resolved, texts = resolve_pages(sorted({page for page, _, _ in rows}))
	found = {}
	for page, version, quest_name in rows:
		title, fragment = resolved[page]
		text = texts.get(title, "")
		quest = match_quest(quest_name, loose=True)
		if not quest or not text:
			print("  skipped recycling item:", page, "-", quest_name, file=sys.stderr)
			continue
		if "yes" in " ".join(fields(infobox(text), "equipable")).lower():
			continue  # equipable quest rewards are often kept as vanity items
		for item_id in versioned_ids(text, page, fragment or version):
			const = items.get(item_id)
			if const:
				found.setdefault(const, set()).add(quest)
	print("Recycling Centre:", len(rows), "rows,", len(found), "unequipable items", file=sys.stderr)
	return found


def generate_quest_items(items, match_quest, book_consts, review):
	texts = category_texts("Category:Quest items")
	print("Fetched", len(texts), "quest item pages", file=sys.stderr)

	entries = {}
	for title, text in sorted(texts.items()):
		if title in QUEST_DENY:
			continue
		box = infobox(text)
		if not box:
			continue
		if "yes" in " ".join(fields(box, "tradeable")).lower() or "yes" in " ".join(fields(box, "equipable")).lower():
			continue

		quests = [match_quest(q, loose=True) for q in re.findall(r"\[\[([^\]|#]+)", " ".join(fields(box, "quest")))]
		if not quests or None in quests:
			continue

		# Match on plain text: "[[quest item]] used only in" would otherwise slip past the wording rule
		text = plain(text)
		reason = NO_USE.search(text)
		if not reason and title not in QUEST_EXTRA:
			continue

		consts = [items.get(i) for i in item_ids(box)]
		if not consts or None in consts:
			continue
		consts = [c for c in consts if c not in book_consts]

		for const in consts:
			entries.setdefault(const, set()).update(quests)
		if review and consts:
			where = reason.start() if reason else 0
			snippet = plain(text[max(0, where - 150):where + 80]).replace("\n", " ")
			print("- %s [%s]%s\n    ...%s..." % (title, ", ".join(sorted(set(quests))),
				" (manual extra)" if title in QUEST_EXTRA else "", snippet))

	# Merge in the Recycling Centre's list; an item on both keeps every quest, so it waits for all of them
	recycled = recycling_items(items, match_quest)
	new = 0
	for const, quests in sorted(recycled.items()):
		if const in book_consts:
			continue
		new += const not in entries
		entries.setdefault(const, set()).update(quests)
	print(new, "quest items only found through the Recycling Centre", file=sys.stderr)

	lines = ["\t\tadd(ItemID.%s, %s);" % (const, ", ".join("Quest." + q for q in sorted(quests)))
		for const, quests in entries.items()]
	write("QuestItems.java", QUEST_ITEMS_TEMPLATE.replace("$ENTRIES", "\n".join(lines)))
	print(len(lines), "quest item entries", file=sys.stderr)


POH_FURNITURE = [
	# (wiki page, storage, section heading or None for the whole list)
	("Armour case space", "ARMOUR_CASE", None),
	("Cape rack space", "CAPE_RACK", None),
	("Magic wardrobe space", "MAGIC_WARDROBE", None),
	("Fancy dress box space", "FANCY_DRESS_BOX", None),
	("Toy box space", "TOY_BOX", None),
	# Only the treasure chest's tier changes what fits: oak holds beginner and easy, teak adds medium
	("Treasure chest space", "TREASURE_CHEST_OAK", "Beginner"),
	("Treasure chest space", "TREASURE_CHEST_OAK", "Easy"),
	("Treasure chest space", "TREASURE_CHEST_TEAK", "Medium"),
	("Treasure chest space", "TREASURE_CHEST_MAHOGANY", "Hard"),
	("Treasure chest space", "TREASURE_CHEST_MAHOGANY", "Elite"),
	("Treasure chest space", "TREASURE_CHEST_MAHOGANY", "Master"),
]

# Variants that share a wiki page but can't be stored: broken minigame armour and temporary event copies
POH_SKIP_VARIANT = re.compile(r"_(BROKEN|TEMP)$|_EVENT_|_GAME_\d*")

# Table cells use plinkp/plinkt; plain plink only appears in footnotes about related items
ITEM_LINK = re.compile(r"\{\{[Pp]link[pt]\|([^|}=]+)((?:\|[^}]*)?)\}\}")


def furniture_items(text, heading):
	body = text[text.find("<onlyinclude>"):] if "<onlyinclude>" in text else text
	if heading:
		start = re.search(r"^===\s*" + heading + r"\s*===", body, re.M)
		if not start:
			sys.exit("Treasure chest section not found: " + heading)
		end = re.search(r"^==", body[start.end():], re.M)
		body = body[start.end():start.end() + end.start()] if end else body[start.end():]
	elif "==Changes==" in body:
		body = body[:body.find("==Changes==")]

	links = []
	for name, rest in ITEM_LINK.findall(body):
		pic = re.search(r"pic=([^|}]+)", rest)
		# 'pic=Prescription goggles (focused)' picks one version of a multi-version page
		version = re.search(r"\(([^)]*)\)\s*$", pic.group(1)) if pic else None
		links.append((name.strip(), version.group(1) if version else None))
	return links


def generate_poh(items):
	pages = page_texts(sorted({page for page, _, _ in POH_FURNITURE}))
	links = []
	for page, storage, heading in POH_FURNITURE:
		links += [(name, version, storage) for name, version in furniture_items(pages[page], heading)]

	resolved, texts = resolve_pages(sorted({name for name, _, _ in links}))
	storage_by_const = {}
	for name, version, storage in links:
		title, fragment = resolved[name]
		text = texts.get(title)
		if text is None:
			print("  skipped storable item without wiki page:", name, file=sys.stderr)
			continue
		for item_id in versioned_ids(text, name, fragment or version):
			const = items.get(item_id)
			if const and not POH_SKIP_VARIANT.search(const):
				storage_by_const.setdefault(const, set()).add(storage)

	lines = ["\t\tadd(ItemID.%s, %s);" % (const, ", ".join("Storage." + s for s in sorted(storages)))
		for const, storages in sorted(storage_by_const.items())]
	write("PohStorage.java", POH_TEMPLATE.replace("$ENTRIES", "\n".join(lines)))
	print(len(lines), "POH storage entries", file=sys.stderr)


POH_TEMPLATE = """package com.housekeeping.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

/**
 * Items that can be stored in costume room furniture. Generated by scripts/generate_data.py from the
 * OSRS Wiki's armour case, cape rack, magic wardrobe, fancy dress box, toy box and treasure chest space pages.
 */
public final class PohStorage
{
\t@Getter
\t@RequiredArgsConstructor
\tpublic enum Storage
\t{
\t\tARMOUR_CASE("armour case"),
\t\tCAPE_RACK("cape rack"),
\t\tMAGIC_WARDROBE("magic wardrobe"),
\t\tFANCY_DRESS_BOX("fancy dress box"),
\t\tTOY_BOX("toy box"),
\t\t// The minimum treasure chest that holds the item
\t\tTREASURE_CHEST_OAK("treasure chest"),
\t\tTREASURE_CHEST_TEAK("treasure chest"),
\t\tTREASURE_CHEST_MAHOGANY("treasure chest");

\t\tprivate final String displayName;
\t}

\tprivate static final ImmutableMap.Builder<Integer, Set<Storage>> BUILDER = ImmutableMap.builder();

\tstatic
\t{
$ENTRIES
\t}

\tpublic static final Map<Integer, Set<Storage>> ITEMS = BUILDER.build();

\tprivate PohStorage()
\t{
\t}

\tprivate static void add(int itemId, Storage first, Storage... rest)
\t{
\t\tBUILDER.put(itemId, Sets.immutableEnumSet(first, rest));
\t}
}
"""


BOOKS_TEMPLATE = """package com.housekeeping.data;

import com.google.common.collect.ImmutableMap;
import com.housekeeping.Confidence;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

/**
 * Books a POH bookcase hands back for free, in bookcase order. Quest books are only stored once
 * their quest is complete. Generated by scripts/generate_data.py from https://oldschool.runescape.wiki/w/Book
 */
public final class BookcaseBooks
{
\t@Value
\tpublic static class Entry
\t{
\t\t@Nullable
\t\tQuest quest;
\t\tConfidence confidence;
\t}

\tprivate static final ImmutableMap.Builder<Integer, Entry> BUILDER = ImmutableMap.builder();

\tstatic
\t{
$ENTRIES
\t}

\tpublic static final Map<Integer, Entry> ITEMS = BUILDER.build();

\tprivate BookcaseBooks()
\t{
\t}

\tprivate static void add(int itemId, @Nullable Quest quest)
\t{
\t\tBUILDER.put(itemId, new Entry(quest, Confidence.HIGH));
\t}

\t// Books that still do something besides being read
\tprivate static void maybe(int itemId, @Nullable Quest quest)
\t{
\t\tBUILDER.put(itemId, new Entry(quest, Confidence.MAYBE));
\t}
}
"""

QUEST_ITEMS_TEMPLATE = """package com.housekeeping.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

/**
 * Untradeable, unequipable quest items the wiki says have no use once every quest that needs them
 * is complete. Generated by scripts/generate_data.py from https://oldschool.runescape.wiki/w/Category:Quest_items
 */
public final class QuestItems
{
\tprivate static final ImmutableMap.Builder<Integer, Set<Quest>> BUILDER = ImmutableMap.builder();

\tstatic
\t{
$ENTRIES
\t}

\tpublic static final Map<Integer, Set<Quest>> ITEMS = BUILDER.build();

\tprivate QuestItems()
\t{
\t}

\tprivate static void add(int itemId, Quest... quests)
\t{
\t\tBUILDER.put(itemId, ImmutableSet.copyOf(quests));
\t}
}
"""


def main():
	parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
	parser.add_argument("--review", action="store_true", help="print the wiki wording behind every quest item")
	args = parser.parse_args()

	items, quests = load_constants()
	match_quest = quest_matcher(quests)
	book_consts = generate_books(items, match_quest)
	generate_quest_items(items, match_quest, book_consts, args.review)
	generate_poh(items)


if __name__ == "__main__":
	main()
