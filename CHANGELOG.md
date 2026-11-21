# Changelog

## 1.0.0 (Nov 20, 2026)

First version.

- Seven play styles: Recommended mass, Max permits / hr, Max points / game, Max Fishing XP,
  Firefighter, Chill: one permit, Custom.
- Mass, solo and duo modes, with wiki solo routes (17 / 19 / 19 / 27, 5 rounds, cooking + firefighting).
- Next step box, points and permits box, storm warnings with time left, scene highlights,
  crate load counter.
- Solo routes: catch the whole count (16, 19...) before cooking; only Solo 5 rounds cooks a first batch of 7-10.
- Solo routes: finish the whole load before going to the spirit pool, even if Tempoross goes under part way through.
- Solo routes rebuilt from the wiki step lists, counting loads over the whole game: Recommended solo and Solo max permits now load 17 then 19 before the first pool (they used to stop at 17). Solo 5 rounds follows the wiki step by step (18 + pause + the rest, 9 or 10 + pause + 17, no pool after round 2, load 1, then 5 and 13 in two crates, keep 9, catch 9 more). New style Solo 10 permits, following Jagd's video: 16 (to 10% energy), put the fires out, full bag before 93%, pool to 63%, full bag, pool to 33%, full bag dropped at cannon 1, 16 into cannon 2, pick up, load cannon 1; 7 buckets. Fish dropped next to a crate no longer count as loaded.
- Double spots and fishing spots are picked by walking time, like the spirit pool: a double across the water (a long walk round) is ignored, and only doubles within about 40 tiles of walking count.
- Channel buttons (YouTube, Twitch, Kick, TikTok) at the bottom of the side panel, open in your web browser; can be turned off. Badges in the README.
- What to bring: each style has its own packing list (buckets, Humidify runes, free slots, best harpoon and more) in the side panel, ticked against your inventory; solo styles ask for the wiki bucket counts (4 for Recommended solo, 6 for Solo max permits, none to start for 5 rounds).
- New duo style "ToadFx duo route": 3 buckets (hammer and rope unless you wear the full spirit angler set), the mass route's opener, then the spirit pool in thirds (100 to 60, 60 to 30, 30 to 0) with a full cooked bag ready for each next round.
- Styles follow the mode: Solo shows the wiki solo styles (Recommended solo = 17/19/19/27), Duo and Mass show the team styles, recommended one first. The separate "Solo route" setting is gone.
- The spirit pool is picked by walking distance (the straight-line nearest can be across the water), once per round.
- "Keep cooking" while you are cooking (any style), so it no longer says to fish half way through a batch.
- New play style "ToadFx mass route" (Todd's own route): opener of 8; in later rounds a fully cooked bag is loaded before 35%, then you wait at the spirit pool. Cooking no longer counts as loading near a crate.
- Spirit pools that open without respawning are now tracked; the pool on your side is picked when the pool step starts and kept for the round. Double spot timer (time up, then time left once it has learnt how long doubles last).
- Points at the nearest spirit pool (there is one on each side); a short loading blip no longer splits or ends a game; games with 0 points are not recorded.
- Path line to the next step's target (and on the minimap), ported from GOTR Coach.
- Fixes from the first in-game test: spirit pool phase follows the open pool (energy recharges while it is open), wins detected from the essence bar, permit count or subdued message, catches and cooks counted from chat, only nearby double spots count, side panel text no longer cut off.
- "Recommended mass" style (new default) is Theoatrix's mass run; Max permits / hr uses the wiki cooking method. Timers are yellow. Storm bar removed from the top box.
- Max points follows Theoatrix's opener (opener of 9, or 8 for max points, then a full bag). Imcando hammer reminder.
- New default colours: next step #FF00F1, double spot #FF7300, urgent #FF1E1E.
- Top instructions box: now, timer, coming-up timers and the next two steps (replaces the top-left box, which is now optional).
- Game timer; lightning, fire-spread and wave timers on the tiles, masts and totem poles.
- Heads you to the spirit pool when energy hits 0. Double spots are pink by default.
- Gear reminders: bring your best harpoon (by play style) and wear your angler or spirit angler outfit, if you own them.
- Pre-game gear check, alerts, post-game grade with tips and points breakdown, session stats,
  personal bests and history.
