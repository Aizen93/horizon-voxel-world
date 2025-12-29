package org.aouessar.core.mesh;

import org.aouessar.core.world.BlockId;
import org.aouessar.core.world.Chunk;
import org.aouessar.core.world.WorldGenerator;

import java.util.ArrayList;
import java.util.List;

public final class GreedyMesher implements Mesher {

    @Override
    public MeshData buildChunkMesh(Chunk chunk, WorldGenerator gen) {
        ArrayList<Float> pos = new ArrayList<>();
        ArrayList<Float> nrm = new ArrayList<>();
        ArrayList<Float> uv  = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();
        ArrayList<Float> mat = new ArrayList<>();

        int baseX = chunk.pos.cx() * Chunk.SIZE;
        int baseY = chunk.pos.cy() * Chunk.SIZE;
        int baseZ = chunk.pos.cz() * Chunk.SIZE;

        // mask stores "material id" for visible faces, 0 for none.
        // sign indicates face direction (+/-) to encode normal.
        short[] mask = new short[Chunk.SIZE * Chunk.SIZE];

        int indexBase = 0;

        // We run greedy meshing for each axis (d = 0,1,2) and for both directions.
        for (int d = 0; d < 3; d++) {
            int u = (d + 1) % 3;
            int v = (d + 2) % 3;

            int[] q = new int[]{0,0,0};
            q[d] = 1;

            int[] x = new int[]{0,0,0};

            int[] dims = new int[]{Chunk.SIZE, Chunk.SIZE, Chunk.SIZE};

            // iterate slices along axis d, from -1 to SIZE-1 to compare neighbor blocks
            for (x[d] = -1; x[d] < dims[d]; x[d]++) {
                // build mask for this slice
                int n = 0;
                for (x[v] = 0; x[v] < dims[v]; x[v]++) {
                    for (x[u] = 0; x[u] < dims[u]; x[u]++) {

                        short a = voxelAt(chunk, gen, baseX, baseY, baseZ, x[0], x[1], x[2]);
                        short b = voxelAt(chunk, gen, baseX, baseY, baseZ, x[0] + q[0], x[1] + q[1], x[2] + q[2]);

                        // if one is solid and the other is air => face exists
                        short m = 0;
                        if (isSolid(a) && !isSolid(b)) {
                            // face toward +d
                            m = a; // positive: +normal
                        } else if (!isSolid(a) && isSolid(b)) {
                            // face toward -d
                            m = (short)-b; // negative: -normal
                        }

                        mask[n++] = m;
                    }
                }

                // Greedy merge rectangles in the 2D mask
                n = 0;
                for (int j = 0; j < dims[v]; j++) {
                    for (int i = 0; i < dims[u]; ) {
                        short m = mask[n];
                        if (m == 0) { i++; n++; continue; }

                        // compute width
                        int w;
                        for (w = 1; i + w < dims[u] && mask[n + w] == m; w++) {}

                        // compute height
                        int h;
                        outer:
                        for (h = 1; j + h < dims[v]; h++) {
                            int row = n + h * dims[u];
                            for (int k = 0; k < w; k++) {
                                if (mask[row + k] != m) break outer;
                            }
                        }

                        // emit quad
                        x[u] = i;
                        x[v] = j;

                        int[] du = new int[]{0,0,0};
                        int[] dv = new int[]{0,0,0};
                        du[u] = w;
                        dv[v] = h;

                        indexBase = emitQuad(
                                pos,nrm,uv,idx,mat,indexBase,
                                baseX, baseY, baseZ,
                                x, du, dv, d,
                                m
                        );

                        // clear mask
                        for (int y = 0; y < h; y++) {
                            int row = n + y * dims[u];
                            for (int k = 0; k < w; k++) mask[row + k] = 0;
                        }

                        i += w;
                        n += w;
                    }
                }
            }
        }

        return new MeshData(
                toFloatArray(pos),
                toFloatArray(nrm),
                toFloatArray(uv),
                toIntArray(idx),
                toFloatArray(mat)
        );
    }

    private static boolean isSolid(short id) {
        return id != BlockId.AIR;
    }

    private static short voxelAt(Chunk chunk, WorldGenerator gen,
                                 int baseX, int baseY, int baseZ,
                                 int lx, int ly, int lz) {
        int wx = baseX + lx;
        int wy = baseY + ly;
        int wz = baseZ + lz;

        // fast path: inside chunk local coords [0..SIZE-1]
        if (lx >= 0 && lx < Chunk.SIZE && ly >= 0 && ly < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
            return chunk.get(lx, ly, lz);
        }
        return gen.blockAt(wx, wy, wz);
    }

    private static int emitQuad(
            ArrayList<Float> pos, ArrayList<Float> nrm, ArrayList<Float> uv,
            ArrayList<Integer> idx, ArrayList<Float> mat,
            int indexBase,
            int baseX, int baseY, int baseZ,
            int[] x, int[] du, int[] dv, int d,
            short maskVal
    ) {
        // maskVal sign encodes face direction
        boolean positive = maskVal > 0;
        short materialId = (short) Math.abs(maskVal);

        if (materialId == BlockId.GRASS) {
            boolean isTopFace = (d == 1) && positive; // +Y
            if (!isTopFace) {
                materialId = BlockId.GRASS_SIDE;
            }
        }

        if (materialId == BlockId.GRASS_SIDE) {
            boolean isBottomFace = (d == 1) && !positive;
            if (isBottomFace) {
                materialId = BlockId.DIRT;
            }
        }

        // Compute normal
        float nx=0, ny=0, nz=0;
        if (d == 0) nx = positive ? 1 : -1;
        if (d == 1) ny = positive ? 1 : -1;
        if (d == 2) nz = positive ? 1 : -1;

        // Corner positions in local coords
        // x is the “slice” coordinate; face lies on x[d]+1 if positive else x[d]+1? / careful:
        // In the classic algorithm, face is between voxel a and b; when positive, it is at x[d]+1, when negative at x[d]+1 as well,
        // because x[d] runs -1..SIZE-1 and we sample (x) and (x+q). Use x[d]+1 as plane coordinate.
        int[] p = new int[]{x[0], x[1], x[2]};
        p[d] = x[d] + 1;

        float x0 = baseX + p[0];
        float y0 = baseY + p[1];
        float z0 = baseZ + p[2];

        float x1 = baseX + p[0] + du[0];
        float y1 = baseY + p[1] + du[1];
        float z1 = baseZ + p[2] + du[2];

        float x2 = baseX + p[0] + dv[0];
        float y2 = baseY + p[1] + dv[1];
        float z2 = baseZ + p[2] + dv[2];

        float x3 = baseX + p[0] + du[0] + dv[0];
        float y3 = baseY + p[1] + du[1] + dv[1];
        float z3 = baseZ + p[2] + du[2] + dv[2];

        // We need consistent winding (CCW) with outward normal.
        // Choose vertex order depending on face direction.
        if (positive) {
            // v0 = p
            pushVertex(pos,nrm,uv,mat, x0,y0,z0, nx,ny,nz, 0,0, materialId);
            pushVertex(pos,nrm,uv,mat, x1,y1,z1, nx,ny,nz, 1,0, materialId);
            pushVertex(pos,nrm,uv,mat, x3,y3,z3, nx,ny,nz, 1,1, materialId);
            pushVertex(pos,nrm,uv,mat, x2,y2,z2, nx,ny,nz, 0,1, materialId);
        } else {
            // flip winding
            pushVertex(pos,nrm,uv,mat, x0,y0,z0, nx,ny,nz, 0,0, materialId);
            pushVertex(pos,nrm,uv,mat, x2,y2,z2, nx,ny,nz, 1,0, materialId);
            pushVertex(pos,nrm,uv,mat, x3,y3,z3, nx,ny,nz, 1,1, materialId);
            pushVertex(pos,nrm,uv,mat, x1,y1,z1, nx,ny,nz, 0,1, materialId);
        }

        idx.add(indexBase + 0); idx.add(indexBase + 1); idx.add(indexBase + 2);
        idx.add(indexBase + 2); idx.add(indexBase + 3); idx.add(indexBase + 0);

        return indexBase + 4;
    }

    private static void pushVertex(ArrayList<Float> pos, ArrayList<Float> nrm, ArrayList<Float> uv,
                                   ArrayList<Float> mat,
                                   float x,float y,float z,
                                   float nx,float ny,float nz,
                                   float u,float v,
                                   short materialId) {
        pos.add(x); pos.add(y); pos.add(z);
        nrm.add(nx); nrm.add(ny); nrm.add(nz);
        uv.add(u); uv.add(v);
        mat.add((float) materialId);
    }

    public static float[] toFloatArray(List<Float> list) {
        float[] a = new float[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    public static int[] toIntArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    public static short[] toShortArray(List<Short> list) {
        short[] a = new short[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }
}
