package com.housekeeping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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

	private static final Type BANK_TYPE = new TypeToken<LinkedHashMap<Integer, Integer>>()
	{
	}.getType();
	private static final Type IGNORED_TYPE = new TypeToken<Set<Integer>>()
	{
	}.getType();

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
		Map<Integer, Integer> savedBank = read(BANK_KEY, BANK_TYPE);
		bank = savedBank == null ? Collections.emptyMap() : Collections.unmodifiableMap(savedBank);

		ignored.clear();
		Set<Integer> savedIgnored = read(IGNORED_KEY, IGNORED_TYPE);
		if (savedIgnored != null)
		{
			ignored.addAll(savedIgnored);
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

	private <T> T read(String key, Type type)
	{
		String json = configManager.getRSProfileConfiguration(HouseKeepingConfig.GROUP, key);
		return json == null ? null : gson.fromJson(json, type);
	}
}
