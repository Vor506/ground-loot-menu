package com.vor506.groundlootmenu;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GroundLootMenuPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(GroundLootMenuPlugin.class);
        RuneLite.main(args);
    }
}