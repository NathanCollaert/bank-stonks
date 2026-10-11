package com.bankstonks;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Start it with {@code gradlew dev}, which turns on the assertions RuneLite needs to load builtin plugins. */
public class BankStonksDevLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankStonksPlugin.class, UiWatchdogPlugin.class, BankStonksDevPlugin.class);
		RuneLite.main(args);
	}
}
