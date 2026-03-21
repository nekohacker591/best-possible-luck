package com.example.bestpossibleluck;

public final class LuckHooks {
    private LuckHooks() {
    }

    public static int bestNextInt(int bound) {
        return bound <= 1 ? 0 : bound - 1;
    }

    public static int bestNextIntUnbounded() {
        return Integer.MAX_VALUE;
    }

    public static long bestNextLong() {
        return Long.MAX_VALUE;
    }

    public static boolean bestNextBoolean() {
        return true;
    }

    public static float bestNextFloat() {
        return Math.nextDown(1.0F);
    }

    public static double bestNextDouble() {
        return Math.nextDown(1.0D);
    }

    public static double bestNextGaussian() {
        return 8.0D;
    }

    public static void touchSeed(long seed) {
        // Intentionally ignored so repeated random calls stay deterministic in the "best" direction.
    }
}
