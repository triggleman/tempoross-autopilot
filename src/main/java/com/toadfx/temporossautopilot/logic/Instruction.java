package com.toadfx.temporossautopilot.logic;

/** One step for the player: a short title, a line of detail and what to highlight. */
public final class Instruction
{
	public static final Instruction NONE = new Instruction("", "", Target.NONE, Urgency.NORMAL);

	private final String title;
	private final String detail;
	private final Target target;
	private final Urgency urgency;

	public Instruction(String title, String detail, Target target, Urgency urgency)
	{
		this.title = title;
		this.detail = detail == null ? "" : detail;
		this.target = target;
		this.urgency = urgency;
	}

	public static Instruction of(String title, String detail, Target target)
	{
		return new Instruction(title, detail, target, Urgency.NORMAL);
	}

	public static Instruction soon(String title, String detail, Target target)
	{
		return new Instruction(title, detail, target, Urgency.SOON);
	}

	public static Instruction now(String title, String detail, Target target)
	{
		return new Instruction(title, detail, target, Urgency.NOW);
	}

	public String getTitle()
	{
		return title;
	}

	public String getDetail()
	{
		return detail;
	}

	public Target getTarget()
	{
		return target;
	}

	public Urgency getUrgency()
	{
		return urgency;
	}

	public boolean isEmpty()
	{
		return title.isEmpty();
	}

	@Override
	public String toString()
	{
		return title + (detail.isEmpty() ? "" : " - " + detail);
	}
}
