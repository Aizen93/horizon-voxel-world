package org.aouessar.core.gen.structure;

import org.aouessar.core.util.Mathx;
import org.aouessar.core.world.layer.BiomeMap;
import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.LayerRect;
import org.aouessar.core.world.layer.StructureMap;

import java.util.ArrayList;
import java.util.List;

public final class DefaultStructureBuilder implements StructureBuilder {

    @Override
    public StructureMap placeStructures(long seed, Heightmap heightmap, BiomeMap biomeMap) {
        LayerRect rect = heightmap.rect();
        List<StructureMap.Placement> list = new ArrayList<>();

        // Deterministic “per-column RNG” via hashing (streaming safe, order-independent).
        for (int z = rect.minZ(); z < rect.minZ() + rect.sizeZ(); z++) {
            for (int x = rect.minX(); x < rect.minX() + rect.sizeX(); x++) {
                byte biome = biomeMap.biomeAt(x, z);

                long h = Mathx.mix64(seed ^ ((long)x * 341873128712L) ^ ((long)z * 132897987541L));
                float roll = (h & 0xFFFFFF) / (float)0x1000000; // 0..1

                if (biome == BiomeMap.FOREST && roll < 0.008f) {
                    int height = 4 + (int)((h >>> 24) & 3);
                    list.add(new StructureMap.TreePlacement(x, z, (byte)0, height));
                }
                if (biome == BiomeMap.DESERT && roll < 0.004f) {
                    int height = 2 + (int)((h >>> 28) & 3);
                    list.add(new StructureMap.CactusPlacement(x, z, height));
                }
            }
        }
        return new StructureMap(rect, list);
    }
}