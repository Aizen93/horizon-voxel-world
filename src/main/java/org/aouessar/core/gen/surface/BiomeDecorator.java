package org.aouessar.core.gen.surface;

import org.aouessar.core.util.EngineConstants;
import org.aouessar.core.world.block.BlockId;
import org.aouessar.core.world.layer.*;

public final class BiomeDecorator implements SurfaceDecorator {

    @Override
    public SurfaceRules generateSurfaceRules(Heightmap heightmap, BiomeMap biomeMap) {
        LayerRect rect = heightmap.rect();
        int area = rect.area();

        short[] top = new short[area];
        short[] filler = new short[area];
        byte[] depth = new byte[area];

        for (int z = rect.minZ(); z < rect.minZ() + rect.sizeZ(); z++) {
            for (int x = rect.minX(); x < rect.minX() + rect.sizeX(); x++) {
                int idx = rect.index2D(x, z);

                byte biome = biomeMap.biomeAt(x, z);
                int h = heightmap.heightAt(x, z);

                // Defaults
                short topBlock = BlockId.GRASS;
                short fillerBlock = BlockId.DIRT;
                int fillerDepth = 3;

                // Ocean shores / underwater columns
                if (h <= EngineConstants.SEA_LEVEL) {
                    topBlock = BlockId.SAND;
                    fillerBlock = BlockId.SAND;
                    fillerDepth = 4;
                }

                // Biome overrides
                switch (biome) {
                    case BiomeMap.DESERT -> {
                        topBlock = BlockId.SAND;
                        fillerBlock = BlockId.SAND;
                        fillerDepth = 5;
                    }
                    case BiomeMap.MOUNTAINS -> {
                        topBlock = BlockId.STONE;
                        fillerBlock = BlockId.STONE;
                        fillerDepth = 1;
                    }
                    case BiomeMap.SWAMP -> {
                        topBlock = BlockId.GRASS;
                        fillerBlock = BlockId.DIRT;
                        fillerDepth = 4;
                    }
                    case BiomeMap.FOREST, BiomeMap.PLAINS -> {
                        topBlock = BlockId.GRASS;
                        fillerBlock = BlockId.DIRT;
                        fillerDepth = 3;
                    }
                    default -> { /* keep defaults */ }
                }

                top[idx] = topBlock;
                filler[idx] = fillerBlock;
                depth[idx] = (byte) Math.min(255, fillerDepth);
            }
        }

        return new ColumnSurfaceRules(rect, top, filler, depth);

    }
}