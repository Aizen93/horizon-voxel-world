package org.aouessar.core.world.layer;

import java.util.Arrays;

public final class ColumnSurfaceRules implements SurfaceRules {

    private final LayerRect rect;
    private final short[] topBlock;     // rect.area()
    private final short[] fillerBlock;  // rect.area()
    private final byte[] fillerDepth;   // rect.area(), 0..255

    public ColumnSurfaceRules(LayerRect rect, short[] topBlock, short[] fillerBlock, byte[] fillerDepth) {
        if (rect == null) throw new IllegalArgumentException("rect is null");
        if (topBlock == null) throw new IllegalArgumentException("topBlock is null");
        if (fillerBlock == null) throw new IllegalArgumentException("fillerBlock is null");
        if (fillerDepth == null) throw new IllegalArgumentException("fillerDepth is null");

        int area = rect.area();
        if (topBlock.length != area) throw new IllegalArgumentException("topBlock length mismatch");
        if (fillerBlock.length != area) throw new IllegalArgumentException("fillerBlock length mismatch");
        if (fillerDepth.length != area) throw new IllegalArgumentException("fillerDepth length mismatch");

        this.rect = rect;
        this.topBlock = Arrays.copyOf(topBlock, area);
        this.fillerBlock = Arrays.copyOf(fillerBlock, area);
        this.fillerDepth = Arrays.copyOf(fillerDepth, area);
    }

    public LayerRect rect() {
        return rect;
    }

    @Override
    public short topBlockAt(int x, int z) {
        requireInside(x, z);
        return topBlock[rect.index2D(x, z)];
    }

    @Override
    public int fillerDepthAt(int x, int z) {
        requireInside(x, z);
        return fillerDepth[rect.index2D(x, z)] & 0xFF;
    }

    @Override
    public short fillerBlockAt(int x, int z) {
        requireInside(x, z);
        return fillerBlock[rect.index2D(x, z)];
    }

    private void requireInside(int x, int z) {
        if (!rect.contains(x, z)) throw new IllegalArgumentException("Out of rect: " + x + "," + z);
    }
}