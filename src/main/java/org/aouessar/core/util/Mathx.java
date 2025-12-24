package org.aouessar.core.util;

public class Mathx {

    private Mathx() {}

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float clamp01(float v) {
        return clamp(v, 0f, 1f);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Hermite smoothstep between edge0 and edge1.
     */
    public static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3f - 2f * t);
    }

    /**
     * Maps [-1..1] noise to [0..1].
     */
    public static float noise01(float n) {
        return (n + 1f) * 0.5f;
    }

    public static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

}