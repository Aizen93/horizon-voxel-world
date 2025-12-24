package org.aouessar.renderer.lwjgl;

import org.aouessar.core.world.block.BlockId;

public final class BlockTiles {

    private BlockTiles(){}

    // Must match names in atlas.json (your packer uses file names as keys)
    public static String tileForBlock(short id) {
        return switch (id) {
            case BlockId.GRASS -> "grass";
            case BlockId.DIRT -> "dirt";
            case BlockId.STONE -> "stone";
            case BlockId.SAND -> "sand";
            case BlockId.WATER -> "water";
            case BlockId.LOG -> "log";
            case BlockId.LEAVES -> "leaves";
            case BlockId.CACTUS -> "cactus";
            default -> "stone";
        };
    }
}