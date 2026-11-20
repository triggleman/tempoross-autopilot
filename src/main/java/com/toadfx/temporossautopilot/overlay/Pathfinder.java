package com.toadfx.temporossautopilot.overlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Walking routes inside the loaded scene. Each tick it floods out from the player once, so the
 * distance to every target (and the route to the current one) is a cheap lookup afterwards.
 * Costs are in tiles; running covers two tiles per tick.
 */
public class Pathfinder
{
	private static final int FULL = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
	private static final int N = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
	private static final int E = CollisionDataFlag.BLOCK_MOVEMENT_EAST;
	private static final int S = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
	private static final int W = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
	private static final int NE = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST;
	private static final int NW = CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST;
	private static final int SE = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST;
	private static final int SW = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST;
	/** A shortcut between two tiles counts as this many tiles (unused at Tempoross). */
	private static final int SHORTCUT_COST = 6;
	/** Give up beyond this many tiles; everything in the cove is closer. */
	private static final int MAX_COST = 200;
	/** How far out to look for a standing tile when nothing next to the target is walkable. */
	private static final int MAX_REACH_RING = 5;

	/** The order the game's own pathfinding checks neighbours in. */
	private static final int[][] GAME_ORDER = {
		{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
	};

	private java.util.List<java.util.ArrayDeque<Integer>> bucket;
	private int baseX;
	private int baseY;
	private int plane;
	private int sizeX;
	private int sizeY;
	private int[][] flags;
	private int[] dist = new int[0];
	private int[] prev = new int[0];
	private boolean ready;

	/**
	 * Floods out from the player's tile.
	 *
	 * @param shortcutA one end of a shortcut (or null)
	 * @param shortcutB the other end
	 */
	public void update(WorldView wv, WorldPoint start, WorldPoint shortcutA, WorldPoint shortcutB)
	{
		ready = false;
		if (wv == null || start == null)
		{
			return;
		}
		CollisionData[] maps = wv.getCollisionMaps();
		plane = wv.getPlane();
		if (maps == null || plane < 0 || plane >= maps.length || maps[plane] == null || start.getPlane() != plane)
		{
			return;
		}
		flags = maps[plane].getFlags();
		baseX = wv.getBaseX();
		baseY = wv.getBaseY();
		sizeX = flags.length;
		sizeY = flags.length == 0 ? 0 : flags[0].length;
		int n = sizeX * sizeY;
		if (dist.length != n)
		{
			dist = new int[n];
			prev = new int[n];
		}
		Arrays.fill(dist, Integer.MAX_VALUE);
		Arrays.fill(prev, -1);

		int sx = start.getX() - baseX;
		int sy = start.getY() - baseY;
		if (!inScene(sx, sy))
		{
			return;
		}
		int shortA = shortcutA == null ? -1 : index(shortcutA.getX() - baseX, shortcutA.getY() - baseY);
		int shortB = shortcutB == null ? -1 : index(shortcutB.getX() - baseX, shortcutB.getY() - baseY);

		// Breadth-first, first visit wins, neighbours in the game's own order (W, E, S, N, SW, SE, NW, NE),
		// so equally short routes break the same way the client walks them and the line matches your steps.
		// Buckets by distance keep that order while letting a shortcut cost more than one step.
		int buckets = MAX_COST + SHORTCUT_COST + 2;
		if (bucket == null || bucket.size() != buckets)
		{
			bucket = new ArrayList<>(buckets);
			for (int i = 0; i < buckets; i++)
			{
				bucket.add(new java.util.ArrayDeque<>());
			}
		}
		else
		{
			for (java.util.ArrayDeque<Integer> q : bucket)
			{
				q.clear();
			}
		}
		int startIdx = index(sx, sy);
		dist[startIdx] = 0;
		bucket.get(0).add(startIdx);
		for (int d = 0; d <= MAX_COST; d++)
		{
			java.util.ArrayDeque<Integer> q = bucket.get(d);
			while (!q.isEmpty())
			{
				int idx = q.poll();
				if (dist[idx] != d)
				{
					continue;
				}
				int x = idx / sizeY;
				int y = idx % sizeY;
				for (int[] dir : GAME_ORDER)
				{
					if (canMove(x, y, dir[0], dir[1]))
					{
						visit(idx, index(x + dir[0], y + dir[1]), d + 1);
					}
				}
				if (idx == shortA && shortB >= 0)
				{
					visit(idx, shortB, d + SHORTCUT_COST);
				}
				else if (idx == shortB && shortA >= 0)
				{
					visit(idx, shortA, d + SHORTCUT_COST);
				}
			}
		}
		ready = true;
	}

	/** Only a strictly shorter route replaces the first one found, so the game's tie order holds. */
	private void visit(int from, int to, int cost)
	{
		if (to >= 0 && cost < dist[to] && cost < bucket.size())
		{
			dist[to] = cost;
			prev[to] = from;
			bucket.get(cost).add(to);
		}
	}

	private boolean canMove(int x, int y, int dx, int dy)
	{
		int tx = x + dx;
		int ty = y + dy;
		if (!inScene(tx, ty) || (flags[tx][ty] & FULL) != 0)
		{
			return false;
		}
		int here = flags[x][y];
		if (dx == 0 || dy == 0)
		{
			return !blockedStraight(here, dx, dy);
		}
		// Diagonal: both straight moves must be possible, and no corner blocks.
		if (!canMove(x, y, dx, 0) || !canMove(x, y, 0, dy))
		{
			return false;
		}
		if (blockedStraight(flags[x + dx][y], 0, dy) || blockedStraight(flags[x][y + dy], dx, 0))
		{
			return false;
		}
		int corner = dx > 0 ? (dy > 0 ? NE : SE) : (dy > 0 ? NW : SW);
		return (here & corner) == 0;
	}

	private static boolean blockedStraight(int f, int dx, int dy)
	{
		if (dy > 0)
		{
			return (f & N) != 0;
		}
		if (dy < 0)
		{
			return (f & S) != 0;
		}
		if (dx > 0)
		{
			return (f & E) != 0;
		}
		return (f & W) != 0;
	}

	private boolean inScene(int x, int y)
	{
		return x >= 0 && y >= 0 && x < sizeX && y < sizeY;
	}

	private int index(int x, int y)
	{
		return inScene(x, y) ? x * sizeY + y : -1;
	}

	/** Tiles to reach an interaction tile next to the object, or -1. */
	public int distanceTo(TileObject object)
	{
		int best = bestGoal(object);
		return best < 0 ? -1 : dist[best];
	}

	/** Running ticks to reach the object, or -1. */
	public int ticksTo(TileObject object)
	{
		int d = distanceTo(object);
		return d < 0 ? -1 : (d + 1) / 2;
	}

	public int ticksTo(WorldPoint point)
	{
		if (!ready || point == null)
		{
			return -1;
		}
		int idx = index(point.getX() - baseX, point.getY() - baseY);
		if (idx < 0 || dist[idx] == Integer.MAX_VALUE)
		{
			return -1;
		}
		return (dist[idx] + 1) / 2;
	}

	/** The route to the object as world points, starting next to the player. */
	public List<WorldPoint> pathTo(TileObject object)
	{
		return trace(bestGoal(object));
	}

	/** Running ticks to reach a tile next to an area, e.g. an NPC's footprint (south-west tile + size). */
	public int ticksToArea(WorldPoint southWest, int size)
	{
		int best = bestGoalArea(southWest, size);
		return best < 0 ? -1 : (dist[best] + 1) / 2;
	}

	/** Route to a tile next to an area, e.g. an NPC like the Great Guardian. */
	public List<WorldPoint> pathToArea(WorldPoint southWest, int size)
	{
		return trace(bestGoalArea(southWest, size));
	}

	private int bestGoalArea(WorldPoint southWest, int size)
	{
		if (!ready || southWest == null || southWest.getPlane() != plane)
		{
			return -1;
		}
		int minX = southWest.getX() - baseX;
		int minY = southWest.getY() - baseY;
		int n = Math.max(1, size);
		return bestInRect(minX, minY, minX + n - 1, minY + n - 1);
	}

	private List<WorldPoint> trace(int best)
	{
		if (best < 0)
		{
			return Collections.emptyList();
		}
		List<WorldPoint> out = new ArrayList<>();
		for (int idx = best; idx >= 0; idx = prev[idx])
		{
			out.add(new WorldPoint(baseX + idx / sizeY, baseY + idx % sizeY, plane));
		}
		Collections.reverse(out);
		return out;
	}

	/** Cheapest reachable tile from which the object can be used. */
	private int bestGoal(TileObject object)
	{
		if (!ready || object == null || object.getPlane() != plane)
		{
			return -1;
		}
		int minX;
		int minY;
		int maxX;
		int maxY;
		if (object instanceof GameObject)
		{
			GameObject go = (GameObject) object;
			Point min = go.getSceneMinLocation();
			Point max = go.getSceneMaxLocation();
			minX = min.getX();
			minY = min.getY();
			maxX = max.getX();
			maxY = max.getY();
		}
		else
		{
			LocalPoint lp = object.getLocalLocation();
			minX = maxX = lp.getSceneX();
			minY = maxY = lp.getSceneY();
		}
		return bestInRect(minX, minY, maxX, maxY);
	}

	/** Cheapest reachable tile touching the rectangle's sides (or inside it, for walkable things). */
	private int bestInRect(int minX, int minY, int maxX, int maxY)
	{
		int best = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int x = minX - 1; x <= maxX + 1; x++)
		{
			for (int y = minY - 1; y <= maxY + 1; y++)
			{
				boolean corner = (x < minX || x > maxX) && (y < minY || y > maxY);
				if (corner)
				{
					continue;
				}
				int idx = index(x, y);
				if (idx >= 0 && dist[idx] < bestDist)
				{
					bestDist = dist[idx];
					best = idx;
				}
			}
		}
		if (best >= 0)
		{
			return best;
		}
		// Nothing walkable right next to it (e.g. a fishing spot out in the water):
		// widen the ring until we find the closest tile you can stand on.
		for (int r = 2; r <= MAX_REACH_RING && best < 0; r++)
		{
			for (int x = minX - r; x <= maxX + r; x++)
			{
				for (int y = minY - r; y <= maxY + r; y++)
				{
					boolean onRing = x == minX - r || x == maxX + r || y == minY - r || y == maxY + r;
					int idx = onRing ? index(x, y) : -1;
					if (idx >= 0 && dist[idx] < bestDist)
					{
						bestDist = dist[idx];
						best = idx;
					}
				}
			}
		}
		return best;
	}

	/**
	 * Removes the staircase zigzags a tile path has: keeps only the corners you can't see past.
	 * Used for drawing; travel time still comes from the full tile path.
	 */
	public List<WorldPoint> simplify(List<WorldPoint> path)
	{
		return simplify(path, Integer.MAX_VALUE);
	}

	/**
	 * Like {@link #simplify(List)}, but no straight leg spans more than maxLeg tiles of the route,
	 * so the line keeps following the walk with regular bends. maxLeg 1 keeps every tile.
	 */
	public List<WorldPoint> simplify(List<WorldPoint> path, int maxLeg)
	{
		if (!ready || path.size() < 3 || maxLeg <= 1)
		{
			return path;
		}
		List<WorldPoint> out = new ArrayList<>();
		int anchor = 0;
		out.add(path.get(0));
		while (anchor < path.size() - 1)
		{
			int next = anchor + 1;
			int far = (int) Math.min(path.size() - 1, (long) anchor + maxLeg);
			for (int j = far; j > anchor + 1; j--)
			{
				if (clearLine(path.get(anchor), path.get(j)))
				{
					next = j;
					break;
				}
			}
			out.add(path.get(next));
			anchor = next;
		}
		return out;
	}

	/** True when a straight walk between two tiles stays on walkable tiles. */
	private boolean clearLine(WorldPoint a, WorldPoint b)
	{
		double ax = a.getX() - baseX;
		double ay = a.getY() - baseY;
		double bx = b.getX() - baseX;
		double by = b.getY() - baseY;
		int steps = (int) Math.ceil(Math.max(Math.abs(bx - ax), Math.abs(by - ay)) * 3);
		int px = (int) ax;
		int py = (int) ay;
		for (int i = 1; i <= steps; i++)
		{
			double t = i / (double) steps;
			int x = (int) Math.round(ax + (bx - ax) * t);
			int y = (int) Math.round(ay + (by - ay) * t);
			if (x == px && y == py)
			{
				continue;
			}
			int dx = Integer.signum(x - px);
			int dy = Integer.signum(y - py);
			if (Math.abs(x - px) > 1 || Math.abs(y - py) > 1 || !canMove(px, py, dx, dy))
			{
				return false;
			}
			px = x;
			py = y;
		}
		return true;
	}

	public boolean isReady()
	{
		return ready;
	}
}
