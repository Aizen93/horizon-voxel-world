package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.world.BlockId;
import org.aouessar.core.world.TilePos;
import org.aouessar.core.world.WorldGenerator;

import java.util.ArrayList;

import static org.aouessar.core.mesh.GreedyMesher.*;
import static org.aouessar.utils.Utils.TILE_SIZE;

final class FarTileBuilder {
    private FarTileBuilder() {}

    // LOD -> grid step in blocks
    private static int stepForLod(int lod) {
        return switch (lod) {
            case 0 -> 2;
            case 1 -> 4;
            case 2 -> 8;
            default -> 16;
        };
    }

    static MeshData build(WorldGenerator gen, int seaLevel, TilePos tile) {
        int step = stepForLod(tile.lod());

        int originX = tile.tx() * TILE_SIZE;
        int originZ = tile.tz() * TILE_SIZE;

        int vertsX = TILE_SIZE / step + 1;
        int vertsZ = TILE_SIZE / step + 1;

        ArrayList<Float> pos = new ArrayList<>();
        ArrayList<Float> nrm = new ArrayList<>();
        ArrayList<Float> uv  = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();
        ArrayList<Short> mat = new ArrayList<>();

        int[][] height = new int[vertsZ][vertsX];

        // --- MAIN GRID VERTICES ---
        for (int z = 0; z < vertsZ; z++) {
            int wz = originZ + z * step;
            for (int x = 0; x < vertsX; x++) {
                int wx = originX + x * step;

                int h = heightAt(gen, wx, wz);
                height[z][x] = h;

                pos.add((float) wx);
                pos.add((float) h - 0.15f);
                pos.add((float) wz);

                uv.add(x / (float)(vertsX - 1));
                uv.add(z / (float)(vertsZ - 1));

                mat.add(((h <= seaLevel) ? BlockId.WATER : BlockId.GRASS));

                // placeholder normal (fixed later)
                nrm.add(0f); nrm.add(1f); nrm.add(0f);
            }
        }

        int baseVertexCount = pos.size() / 3;

        // --- NORMALS ---
        for (int z = 0; z < vertsZ; z++) {
            for (int x = 0; x < vertsX; x++) {
                int xm = Math.max(x - 1, 0);
                int xp = Math.min(x + 1, vertsX - 1);
                int zm = Math.max(z - 1, 0);
                int zp = Math.min(z + 1, vertsZ - 1);

                float nx = height[z][xm] - height[z][xp];
                float ny = 2f * step;
                float nz = height[zm][x] - height[zp][x];

                float inv = 1f / (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
                int i = z * vertsX + x;

                nrm.set(i*3,     nx * inv);
                nrm.set(i*3 + 1, ny * inv);
                nrm.set(i*3 + 2, nz * inv);
            }
        }

        // --- INDICES ---
        for (int z = 0; z < vertsZ - 1; z++) {
            for (int x = 0; x < vertsX - 1; x++) {
                int i0 = z * vertsX + x;
                int i1 = i0 + 1;
                int i2 = i0 + vertsX;
                int i3 = i2 + 1;

                idx.add(i0); idx.add(i2); idx.add(i1);
                idx.add(i1); idx.add(i2); idx.add(i3);
            }
        }

        // --- SKIRTS ---
        float skirtDepth = 40f;

        addSkirt(pos,nrm,uv,idx,mat, vertsX,vertsZ, baseVertexCount, skirtDepth, 0); // north
        addSkirt(pos,nrm,uv,idx,mat, vertsX,vertsZ, baseVertexCount, skirtDepth, 1); // south
        addSkirt(pos,nrm,uv,idx,mat, vertsX,vertsZ, baseVertexCount, skirtDepth, 2); // west
        addSkirt(pos,nrm,uv,idx,mat, vertsX,vertsZ, baseVertexCount, skirtDepth, 3); // east

        return new MeshData(
                toFloatArray(pos),
                toFloatArray(nrm),
                toFloatArray(uv),
                toIntArray(idx),
                toShortArray(mat)
        );
    }


    private static int heightAt(WorldGenerator gen, int x, int z) {
        // If your generator is SimpleWorldGenerator, prefer direct heightAt(x,z)
        if (gen instanceof org.aouessar.core.world.SimpleWorldGenerator g) {
            return g.heightAt(x, z);
        }
        // Fallback: scan downward (slow, but shouldn’t be used)
        for (int y = TILE_SIZE; y >= -64; y--) {
            if (gen.blockAt(x, y, z) != BlockId.AIR) return y;
        }
        return 0;
    }

    private static void addSkirt(
            ArrayList<Float> pos, ArrayList<Float> nrm, ArrayList<Float> uv,
            ArrayList<Integer> idx, ArrayList<Short> mat,
            int vertsX, int vertsZ, int base,
            float depth, int edge
    ) {
        int count = pos.size() / 3;

        for (int i = 0; i < (edge < 2 ? vertsX : vertsZ); i++) {
            int top;
            if (edge == 0) top = i;                             // north
            else if (edge == 1) top = (vertsZ-1)*vertsX + i;   // south
            else if (edge == 2) top = i * vertsX;              // west
            else top = i * vertsX + (vertsX - 1);              // east

            float x = pos.get(top * 3);
            float y = pos.get(top * 3 + 1);
            float z = pos.get(top * 3 + 2);

            float nx = nrm.get(top * 3);
            float ny = nrm.get(top * 3 + 1);
            float nz = nrm.get(top * 3 + 2);

            pos.add(x); pos.add(y - depth); pos.add(z);
            nrm.add(nx); nrm.add(ny); nrm.add(nz);
            uv.add(0f); uv.add(0f);
            mat.add(mat.get(top));

            if (i > 0) {
                int a = top;
                int b = top - (edge < 2 ? 1 : vertsX);
                int c = count;
                int d = count - 1;

                idx.add(a); idx.add(c); idx.add(b);
                idx.add(b); idx.add(c); idx.add(d);
            }
            count++;
        }
    }

}

