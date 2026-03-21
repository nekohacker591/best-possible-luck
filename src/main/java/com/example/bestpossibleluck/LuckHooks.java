package com.example.bestpossibleluck;

import java.util.Arrays;

public final class LuckHooks {
    private static final double ALMOST_ONE_DOUBLE = Math.nextDown(1.0D);
    private static final float ALMOST_ONE_FLOAT = Math.nextDown(1.0F);

    private LuckHooks() {
    }

    public static int bestNext(int bits) {
        if (bits <= 0) {
            return 0;
        }
        if (bits >= Integer.SIZE) {
            return Integer.MAX_VALUE;
        }
        return (1 << bits) - 1;
    }

    public static int bestNextInt(int bound) {
        return bound <= 1 ? 0 : bound - 1;
    }

    public static int bestNextIntUnbounded() {
        return Integer.MAX_VALUE;
    }

    public static int bestNextIntRange(int origin, int bound) {
        return bound <= origin ? origin : bound - 1;
    }

    public static long bestNextLong() {
        return Long.MAX_VALUE;
    }

    public static long bestNextLongBounded(long bound) {
        return bound <= 1L ? 0L : bound - 1L;
    }

    public static long bestNextLongRange(long origin, long bound) {
        return bound <= origin ? origin : bound - 1L;
    }

    public static boolean bestNextBoolean() {
        return true;
    }

    public static float bestNextFloat() {
        return ALMOST_ONE_FLOAT;
    }

    public static double bestNextDouble() {
        return ALMOST_ONE_DOUBLE;
    }

    public static double bestNextDoubleBounded(double bound) {
        return Math.max(0.0D, Math.nextDown(bound));
    }

    public static double bestNextDoubleRange(double origin, double bound) {
        return bound <= origin ? origin : Math.nextDown(bound);
    }

    public static double bestNextGaussian() {
        return 8.0D;
    }

    public static void bestNextBytes(byte[] bytes) {
        Arrays.fill(bytes, (byte) 0xFF);
    }

    public static void touchSeed(long seed) {
        // Intentionally ignored so repeated random calls stay deterministic in the "best" direction.
    }

    public static double bestMathRandom() {
        return ALMOST_ONE_DOUBLE;
    }

    public static int maximizeCount(int current, int limit) {
        if (limit <= 0) {
            return Math.max(1, current);
        }
        return Math.max(Math.max(1, current), limit);
    }

    public static int maximizeExperience(int current) {
        return Math.max(current, 32767);
    }

    public static int maximizeLootingLevel(int current) {
        return Math.max(current, 10);
    }

    public static int maximizeEnchantLevel(int current) {
        return Math.max(current, 30);
    }
}
