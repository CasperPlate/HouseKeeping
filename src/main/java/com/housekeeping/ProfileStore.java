package com.housekeeping;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;

/**
 * Per-account state, persisted in the RuneScape profile config so it survives restarts.
 */
@Singleton
public class ProfileStore
{
	private static final String BANK_KEY = "bank";
	private static final String IGNORED_KEY = "ignored";

	private final ConfigManager configManager;
	private final Gson gson;

	/**
	 * Item id to quantity, in bank order. Empty until the bank has been opened once.
	 */
	@Getter
	private volatile Map<Integer, Integer> bank = Collections.emptyMap();
	private final Set<Integer> ignored = ConcurrentHashMap.newKeySet();

	@Inject
	ProfileStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	public void load()
	{
		// Saved as {"itemId": quantity, ...} in bank order, and [itemId, ...]
		Map<Integer, Integer> savedBank = new LinkedHashMap<>();
		JsonObject bankJson = read(BANK_KEY, JsonObject.class);
		if (bankJson != null)
		{
			for (Map.Entry<String, JsonElement> entry : bankJson.entrySet())
			{
				savedBank.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsInt());
			}
		}
		bank = Collections.unmodifiableMap(savedBank);

		ignored.clear();
		int[] savedIgnored = read(IGNORED_KEY, int[].class);
		if (savedIgnored != null)
		{
			for (int itemId : savedIgnored)
			{
				ignored.add(itemId);
			}
		}
	}

	public void clear()
	{
		bank = Collections.emptyMap();
		ignored.clear();
	}

	public void setBank(Map<Integer, Integer> items)
	{
		if (items.equals(bank))
		{
			return;
		}
		bank = Collections.unmodifiableMap(items);
		configManager.setRSProfileConfiguration(HouseKeepingConfig.GROUP, BANK_KEY, gson.toJson(items));
	}

	public Set<Integer> getIgnored()
	{
		return Collections.unmodifiableSet(ignored);
	}

	public void setIgnored(int itemId, boolean ignore)
	{
		boolean changed = ignore ? ignored.add(itemId) : ignored.remove(itemId);
		if (changed)
		{
			configManager.setRSProfileConfiguration(HouseKeepingConfig.GROUP, IGNORED_KEY, gson.toJson(ignored));
		}
	}

	private <T> T read(String key, Class<T> type)
	{
		String json = configManager.getRSProfileConfiguration(HouseKeepingConfig.GROUP, key);
		try
		{
			return json == null ? null : gson.fromJson(json, type);
		}
		catch (JsonParseException | NumberFormatException e)
		{
			// A damaged entry only costs the cached bank or ignore list; the bank is re-read when opened
			return null;
		}
	}
}
