package com.bankstonks.ui;

import net.runelite.client.util.AsyncBufferedImage;

/** Lets the layout test supply sprites without an ItemManager, which can't be built outside the client. */
@FunctionalInterface
public interface ItemImages
{
	AsyncBufferedImage get(int itemId, int quantity, boolean stackable);
}
