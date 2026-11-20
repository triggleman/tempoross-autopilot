package com.toadfx.temporossautopilot.model;

/** Who you are playing with. */
public enum GameMode
{
	MASS("Mass"),
	SOLO("Solo"),
	DUO("Duo");

	private final String label;

	GameMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
