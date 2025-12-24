package org.aouessar.core.gen.carver;

import org.aouessar.core.util.noise.FastNoiseLite;
import org.aouessar.core.world.layer.CarveMask;
import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.LayerRect;

import static org.aouessar.core.util.EngineConstants.*;

public final class DefaultWorldCarver implements WorldCarver {

    @Override
    public CarveMask generateCarveMask(long seed, Heightmap heightmap) {
        LayerRect rect = heightmap.rect();
        int minY = 0;
        int sizeY = WORLD_HEIGHT;

        byte[] carved = new byte[rect.sizeX() * rect.sizeZ() * sizeY];

        // River field: use a low-frequency “valley” noise, then carve along low bands.
        FastNoiseLite river = new FastNoiseLite((int)(seed ^ 0xCAFEBABE1234L));
        river.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        river.SetFrequency(0.0012f);

        for (int z = rect.minZ(); z < rect.minZ() + rect.sizeZ(); z++) {
            for (int x = rect.minX(); x < rect.minX() + rect.sizeX(); x++) {
                int surfaceY = heightmap.heightAt(x, z);

                // “river-ness”: narrow band around 0.0
                float r = river.GetNoise(x, z); // -1..1
                float riverMask = 1f - Math.min(1f, Math.abs(r) / 0.06f); // 1 near center line

                if (riverMask <= 0f) continue;

                // Ensure rivers descend towards sea: carve down to at least seaLevel-2.
                int riverBed = Math.min(surfaceY - 1, SEA_LEVEL - 2);
                int depth = (int)(2 + 6 * riverMask);

                for (int y = riverBed; y > riverBed - depth && y >= 1; y--) {
                    setCarved(rect, carved, minY, sizeY, x, y, z, true);
                }
            }
        }

        return new CarveMask(rect, minY, sizeY, carved);
    }

    private static void setCarved(LayerRect rect, byte[] arr, int minY, int sizeY,
                                  int x, int y, int z, boolean v) {
        int sx = rect.sizeX();
        int idx = ((y - minY) * rect.sizeZ() + (z - rect.minZ())) * sx + (x - rect.minX());
        arr[idx] = (byte)(v ? 1 : 0);
    }
}