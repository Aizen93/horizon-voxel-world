package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.world.BlockId;
import org.aouessar.core.world.TilePos;
import org.aouessar.core.world.WorldGenerator;

import java.util.ArrayList;

import static org.aouessar.core.mesh.GreedyMesher.*;
import static org.aouessar.utils.Utils.*;

final class FarTileBuilder {
    private FarTileBuilder() {}

    // Far-field snow thresholds (you can move these to Utils.java later if you want)
    private static final int FAR_SNOW_START_Y = 150; // starts appearing
    private static final int FAR_SNOW_FULL_Y  = 190; // fully snow

    // LOD -> grid step in blocks
    private static int stepForLod(int lod) {
        return switch (lod) {
            case 0 -> FAR_LOD0_STEP;
            case 1 -> FAR_LOD1_STEP;
            case 2 -> FAR_LOD2_STEP;
            default -> FAR_LOD3_STEP;
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

        // per-vertex material id as float (matches MeshData + MeshInterop layout)
        ArrayList<Float> mat = new ArrayList<>();

        int[][] height = new int[vertsZ][vertsX];

        // --- MAIN GRID VERTICES ---
        for (int z = 0; z < vertsZ; z++) {
            int wz = originZ + z * step;
            for (int x = 0; x < vertsX; x++) {
                int wx = originX + x * step;

                int h = heightAt(gen, wx, wz);
                height[z][x] = h;

                pos.add((float) wx);
                pos.add((float) h - FAR_Y_BIAS);
                pos.add((float) wz);

                uv.add((float) wx);
                uv.add((float) wz);

                short topMat;
                if (h <= seaLevel) {
                    topMat = BlockId.WATER;
                } else {
                    topMat = gen.blockAt(wx, h, wz);
                }

                // --- Snow on high elevations (optional BlockId.SNOW) ---
                // If you don't have SNOW in BlockId, SNOW_ID falls back to GRASS automatically.
                if (topMat != BlockId.WATER) {
                    // blend in snow with height (no allocations)
                    if (h >= FAR_SNOW_START_Y) {
                        if (h >= FAR_SNOW_FULL_Y) {
                            topMat = BlockId.SNOW;
                        } else {
                            // Between start/full: only snow-ify "green" surfaces to avoid turning deserts white
                            // (desert stays sand). You can broaden this if you want snow on stone too.
                            if (topMat == BlockId.GRASS) topMat = BlockId.SNOW;
                        }
                    }
                }

                // store as float per vertex
                mat.add((float) topMat);

                // placeholder normal (fixed later)
                nrm.add(0f); nrm.add(1f); nrm.add(0f);
            }
        }

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

                float inv = 1f / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

                int i = z * vertsX + x;

                nrm.set(i * 3,     nx * inv);
                nrm.set(i * 3 + 1, ny * inv);
                nrm.set(i * 3 + 2, nz * inv);
            }
        }

        // --- INDICES ---
        for (int z = 0; z < vertsZ - 1; z++) {
            for (int x = 0; x < vertsX - 1; x++) {
                int i0 = z * vertsX + x;
                int i1 = i0 + 1;
                int i2 = i0 + vertsX;
                int i3 = i2 + 1;

                // two triangles
                idx.add(i0); idx.add(i2); idx.add(i1);
                idx.add(i1); idx.add(i2); idx.add(i3);
            }
        }

        // --- SKIRTS ---
        float skirtDepth = FAR_SKIRT_DEPTH;

        addSkirt(pos, nrm, uv, idx, mat, vertsX, vertsZ, skirtDepth, 0); // north
        addSkirt(pos, nrm, uv, idx, mat, vertsX, vertsZ, skirtDepth, 1); // south
        addSkirt(pos, nrm, uv, idx, mat, vertsX, vertsZ, skirtDepth, 2); // west
        addSkirt(pos, nrm, uv, idx, mat, vertsX, vertsZ, skirtDepth, 3); // east

        return new MeshData(
                toFloatArray(pos),
                toFloatArray(nrm),
                toFloatArray(uv),
                toIntArray(idx),
                toFloatArray(mat)
        );
    }

    private static int heightAt(WorldGenerator gen, int x, int z) {
        return gen.heightAt(x, z);
    }

    private static void addSkirt(
            ArrayList<Float> pos, ArrayList<Float> nrm, ArrayList<Float> uv,
            ArrayList<Integer> idx, ArrayList<Float> mat,
            int vertsX, int vertsZ, float skirtDepth, int dir) {
        // dir: 0 north, 1 south, 2 west, 3 east
        int start, count, step;
        int[] normal = {0, 0, 0};

        switch (dir) {
            case 0 -> {
                start = 0;
                count = vertsX;
                step = 1;
                normal[2] = -1;
            }
            case 1 -> {
                start = (vertsZ - 1) * vertsX;
                count = vertsX;
                step = 1;
                normal[2] = 1;
            }
            case 2 -> {
                start = 0;
                count = vertsZ;
                step = vertsX;
                normal[0] = -1;
            }
            default -> { // east edge (x=vertsX-1)
                start = vertsX - 1;
                count = vertsZ;
                step = vertsX;
                normal[0] = 1;
            }
        }

        for (int i = 0; i < count; i++) {
            int top = start + i * step;

            float tx = pos.get(top * 3);
            float ty = pos.get(top * 3 + 1);
            float tz = pos.get(top * 3 + 2);

            float topU = uv.get(top * 2);
            float topV = uv.get(top * 2 + 1);

            int bottom = (pos.size() / 3);

            // bottom vertex
            pos.add(tx);
            pos.add(ty - skirtDepth);
            pos.add(tz);

            nrm.add((float) normal[0]);
            nrm.add((float) normal[1]);
            nrm.add((float) normal[2]);

            uv.add(topU);
            uv.add(topV);

            // copy material id from the top vertex (per-vertex float)
            mat.add(mat.get(top));

            // indices: connect top edge to bottom edge
            if (i < count - 1) {
                int top2 = start + (i + 1) * step;
                int bottom2 = bottom + 1;

                // quad as 2 triangles
                idx.add(top);    idx.add(bottom);  idx.add(top2);
                idx.add(top2);   idx.add(bottom);  idx.add(bottom2);
            }
        }
    }

}
