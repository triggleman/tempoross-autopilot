package com.toadfx.temporossautopilot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches RuneLite with Tempoross Autopilot loaded. Run this from IntelliJ, or use the Gradle "run" task. */
public class TemporossAutopilotPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TemporossAutopilotPlugin.class);
		RuneLite.main(args);
	}
}
