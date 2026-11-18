package com.lach.apothicpots;

/** Vanilla XP level curve. */
public final class XpMath {
    private XpMath() {}

    /** Total points to go from level 0 to the given level. */
    public static long pointsForLevel(int level) {
        if (level <= 16) return (long) level * level + 6L * level;
        if (level <= 31) return (long) (2.5 * level * level - 40.5 * level + 360);
        return (long) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** Whole levels a player at level 0 would reach with these points. */
    public static int levels(int points) {
        int level = 0;
        while (pointsForLevel(level + 1) <= points) level++;
        return level;
    }

    /** Levels including the partial one, for drawing bars. */
    public static double exactLevels(int points) {
        int whole = levels(points);
        long base = pointsForLevel(whole);
        long next = pointsForLevel(whole + 1);
        return whole + (points - base) / (double) Math.max(1, next - base);
    }
}
