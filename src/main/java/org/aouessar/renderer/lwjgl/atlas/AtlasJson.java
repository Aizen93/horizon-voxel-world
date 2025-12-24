package org.aouessar.renderer.lwjgl.atlas;

import java.util.Map;

public final class AtlasJson {

    public int atlasWidth;
    public int atlasHeight;
    public int tileSize;
    public int padding;
    public int cols;
    public int rows;
    public Map<String, Tile> tiles;

    public static final class Tile {
        public int index;
        public int col;
        public int row;
        public float u0;
        public float v0;
        public float u1;
        public float v1;
    }
}