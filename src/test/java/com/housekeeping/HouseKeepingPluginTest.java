package com.housekeeping;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HouseKeepingPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HouseKeepingPlugin.class);
		RuneLite.main(args);
	}
}
