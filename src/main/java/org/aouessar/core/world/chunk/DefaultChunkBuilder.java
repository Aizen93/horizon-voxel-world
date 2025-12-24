package org.aouessar.core.world.chunk;

import org.aouessar.core.util.EngineConstants;
import org.aouessar.core.world.block.BlockId;
import org.aouessar.core.world.layer.BiomeMap;
import org.aouessar.core.world.layer.CarveMask;
import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.StructureMap;
import org.aouessar.core.world.layer.SurfaceRules;

import java.util.List;

public final class DefaultChunkBuilder implements ChunkBuilder {

    private final Heightmap heightmap;
    private final CarveMask carveMask;
    private final SurfaceRules surfaceRules;
    private final StructureMap structures;

    public DefaultChunkBuilder(
            Heightmap heightmap,
            CarveMask carveMask,
            SurfaceRules surfaceRules,
            StructureMap structures
    ) {
        if (heightmap == null) throw new IllegalArgumentException("heightmap is null");
        if (carveMask == null) throw new IllegalArgumentException("carveMask is null");
        if (surfaceRules == null) throw new IllegalArgumentException("surfaceRules is null");
        if (structures == null) throw new IllegalArgumentException("structures is null");

        this.heightmap = heightmap;
        this.carveMask = carveMask;
        this.surfaceRules = surfaceRules;
        this.structures = structures;
    }

    @Override
    public Chunk buildChunk(int cx, int cz) {
        final int cs = EngineConstants.CHUNK_SIZE;
        final int wh = EngineConstants.WORLD_HEIGHT;

        final int wx0 = cx * cs;
        final int wz0 = cz * cs;

        short[] blocks = new short[cs * wh * cs];

        for (int lz = 0; lz < cs; lz++) {
            int wz = wz0 + lz;
            for (int lx = 0; lx < cs; lx++) {
                int wx = wx0 + lx;

                for (int y = 0; y < wh; y++) {
                    short id = blockAt(wx, y, wz);
                    blocks[index(lx, y, lz)] = id;
                }
            }
        }

        // Structure placements are composed at chunk-build time (not in generation phases).
        applyStructures(cx, cz, blocks);

        return new Chunk(cx, cz, cs, wh, blocks);
    }

    @Override
    public short blockAt(int wx, int wy, int wz) {
        // Outside generated window? Fail fast so you notice cache/window issues early.
        // (You can relax this later by generating per-window per chunk.)
        int surfaceY = heightmap.heightAt(wx, wz);

        // Bedrock
        if (wy == 0) return BlockId.BEDROCK;

        // Air / water above the terrain surface
        if (wy > surfaceY) {
            return (wy <= EngineConstants.SEA_LEVEL) ? BlockId.WATER : BlockId.AIR;
        }

        // Carving overrides solids (caves/rivers/ravines)
        if (carveMask.isCarvedToAir(wx, wy, wz)) {
            return (wy <= EngineConstants.SEA_LEVEL) ? BlockId.WATER : BlockId.AIR;
        }

        // Surface rules: top + filler
        if (wy == surfaceY) {
            return surfaceRules.topBlockAt(wx, wz);
        }

        int fillerDepth = surfaceRules.fillerDepthAt(wx, wz);
        if (fillerDepth > 0 && wy >= surfaceY - fillerDepth) {
            return surfaceRules.fillerBlockAt(wx, wz);
        }

        // Base material
        return BlockId.STONE;
    }

    // ------------------------------------------------------------
    // Structures: apply placements (still composition step)
    // ------------------------------------------------------------

    private void applyStructures(int cx, int cz, short[] blocks) {
        final int cs = EngineConstants.CHUNK_SIZE;

        final int wx0 = cx * cs;
        final int wz0 = cz * cs;
        final int wx1 = wx0 + cs;
        final int wz1 = wz0 + cs;

        List<StructureMap.Placement> placements = structures.placements();
        if (placements.isEmpty()) return;

        for (StructureMap.Placement p : placements) {
            if (p instanceof StructureMap.TreePlacement t) {
                int x = t.x();
                int z = t.z();
                if (x < wx0 || x >= wx1 || z < wz0 || z >= wz1) continue;

                int lx = x - wx0;
                int lz = z - wz0;
                int y0 = heightmap.heightAt(x, z) + 1;

                // Don’t plant trees in water columns
                if (y0 <= EngineConstants.SEA_LEVEL) continue;

                placeSimpleTree(blocks, lx, lz, y0, t.height());
            } else if (p instanceof StructureMap.CactusPlacement c) {
                int x = c.x();
                int z = c.z();
                if (x < wx0 || x >= wx1 || z < wz0 || z >= wz1) continue;

                int lx = x - wx0;
                int lz = z - wz0;
                int y0 = heightmap.heightAt(x, z) + 1;

                // Allow cactus near sea but not underwater
                if (y0 <= EngineConstants.SEA_LEVEL) continue;

                placeCactus(blocks, lx, lz, y0, c.height());
            }
        }
    }

    private void placeSimpleTree(short[] blocks, int lx, int lz, int y0, int height) {
        // Trunk
        for (int i = 0; i < height; i++) {
            set(blocks, lx, y0 + i, lz, BlockId.LOG);
        }

        // Simple leaf blob
        int leafY = y0 + height;
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                // cheap circle-ish
                if (dx * dx + dz * dz > 4) continue;
                set(blocks, lx + dx, leafY, lz + dz, BlockId.LEAVES);
            }
        }
        // cap
        set(blocks, lx, leafY + 1, lz, BlockId.LEAVES);
    }

    private void placeCactus(short[] blocks, int lx, int lz, int y0, int height) {
        for (int i = 0; i < height; i++) {
            set(blocks, lx, y0 + i, lz, BlockId.CACTUS);
        }
    }

    // ------------------------------------------------------------
    // Storage layout helpers (Y-major is fine, just be consistent)
    // index = ((y * CHUNK_SIZE) + z) * CHUNK_SIZE + x
    // ------------------------------------------------------------

    private static int index(int x, int y, int z) {
        final int cs = EngineConstants.CHUNK_SIZE;
        return ((y * cs) + z) * cs + x;
    }

    private static void set(short[] blocks, int x, int y, int z, short id) {
        final int cs = EngineConstants.CHUNK_SIZE;
        final int wh = EngineConstants.WORLD_HEIGHT;

        if (x < 0 || z < 0 || x >= cs || z >= cs) return;
        if (y < 0 || y >= wh) return;

        int idx = ((y * cs) + z) * cs + x;

        // Avoid overwriting solid blocks with leaves/etc unless the target is air.
        // (Keeps structures from nuking terrain accidentally)
        if (blocks[idx] == BlockId.AIR || blocks[idx] == 0) {
            blocks[idx] = id;
        }
    }
}