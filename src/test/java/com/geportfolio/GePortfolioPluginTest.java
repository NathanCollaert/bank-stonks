package com.geportfolio;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GePortfolioPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GePortfolioPlugin.class);
		RuneLite.main(args);
	}
}
