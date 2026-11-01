package com.housekeeping;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Forgiving search over suggestion names and reasons. Case, spaces and punctuation are ignored, query
 * words may come in any order, initials match ("bdm" finds "Blue dragon mask"), and longer words
 * tolerate a typo or two. Typo matches score at most {@link #TYPO_MATCH_MAX} so callers can hide them
 * whenever something matched exactly.
 */
final class ItemSearch
{
	static final int NO_MATCH = 0;
	/**
	 * Scores at or below this come from typo-tolerant matching; everything above matched exactly.
	 */
	static final int TYPO_MATCH_MAX = 40;

	private ItemSearch()
	{
	}

	/**
	 * @return how well the query matches, higher is better, or {@link #NO_MATCH}
	 */
	static int score(String query, String name, String reason)
	{
		String compactQuery = compact(query);
		if (compactQuery.isEmpty())
		{
			return NO_MATCH;
		}

		String compactName = compact(name);
		List<String> tokens = tokens(query);

		if (compactName.equals(compactQuery))
		{
			return 100;
		}
		if (compactName.startsWith(compactQuery))
		{
			return 95;
		}
		if (compactName.contains(compactQuery))
		{
			return 90;
		}
		if (tokens.stream().allMatch(compactName::contains))
		{
			return 80;
		}
		if (compactQuery.length() >= 2 && initials(name).startsWith(compactQuery))
		{
			return 70;
		}

		String compactReason = compact(reason);
		if (compactReason.contains(compactQuery) || tokens.stream().allMatch(compactReason::contains))
		{
			return 50;
		}

		int distance = fuzzyDistance(tokens, compactName);
		if (distance < 0 && tokens.size() > 1)
		{
			// "spot tiercape": the words may not line up with the name's, so try the query as a whole
			distance = fuzzyDistance(List.of(compactQuery), compactName);
		}
		if (distance >= 0)
		{
			return Math.max(1, TYPO_MATCH_MAX - distance * 5);
		}
		return NO_MATCH;
	}

	/**
	 * Lower case letters and digits only, so "Fire  Cape", "fire-cape" and "firecape" all compare equal.
	 */
	static String compact(String text)
	{
		StringBuilder out = new StringBuilder(text.length());
		for (char c : text.toLowerCase(Locale.ROOT).toCharArray())
		{
			if (Character.isLetterOrDigit(c))
			{
				out.append(c);
			}
		}
		return out.toString();
	}

	private static List<String> tokens(String query)
	{
		List<String> tokens = new ArrayList<>();
		for (String word : query.trim().split("\\s+"))
		{
			String token = compact(word);
			if (!token.isEmpty())
			{
				tokens.add(token);
			}
		}
		return tokens;
	}

	private static String initials(String name)
	{
		StringBuilder out = new StringBuilder();
		for (String word : name.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
		{
			if (!word.isEmpty())
			{
				out.append(word.charAt(0));
			}
		}
		return out.toString();
	}

	/**
	 * Sum of each token's closest approximate match anywhere in the text, or -1 if any token is too far off.
	 */
	private static int fuzzyDistance(List<String> tokens, String text)
	{
		int total = 0;
		for (String token : tokens)
		{
			int distance = substringDistance(token, text);
			if (distance > allowedTypos(token.length()))
			{
				return -1;
			}
			total += distance;
		}
		return total;
	}

	private static int allowedTypos(int length)
	{
		if (length <= 3)
		{
			return 0;
		}
		return length <= 5 ? 1 : 2;
	}

	/**
	 * Smallest edit distance between the pattern and any substring of the text (Sellers' algorithm).
	 */
	static int substringDistance(String pattern, String text)
	{
		int[] previous = new int[text.length() + 1];
		int[] current = new int[text.length() + 1];
		// Row 0 stays all zeroes: a match may start anywhere in the text
		for (int i = 1; i <= pattern.length(); i++)
		{
			current[0] = i;
			for (int j = 1; j <= text.length(); j++)
			{
				int substitution = previous[j - 1] + (pattern.charAt(i - 1) == text.charAt(j - 1) ? 0 : 1);
				current[j] = Math.min(substitution, Math.min(previous[j], current[j - 1]) + 1);
			}
			int[] swap = previous;
			previous = current;
			current = swap;
		}

		int best = Integer.MAX_VALUE;
		for (int distance : previous)
		{
			best = Math.min(best, distance);
		}
		return best;
	}
}
