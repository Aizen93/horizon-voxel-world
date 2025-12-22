package org.aouessar.platform.lwjgl.renderer;

import org.aouessar.core.mesh.MeshData;

final class MeshInterop {
    /** Interleaved vertex stride: pos3 + nrm3 + uv2 + mat1 = 9 floats */
    public static final int FLOATS_PER_VERTEX = 9;

    private MeshInterop() {}

    /**
     * Interleaves MeshData into a float[] with layout:
     * [px,py,pz, nx,ny,nz, u,v, mat]
     *
     * = 9 floats per vertex
     */
    static float[] toInterleaved(MeshData m) {
        int v = m.vertexCount();
        float[] out = new float[v * FLOATS_PER_VERTEX]; // 9 floats/vertex

        for (int i = 0; i < v; i++) {
            int p3 = i * 3;
            int t2 = i * 2;
            int o9 = i * 9;

            // position
            out[o9]     = m.positions[p3];
            out[o9 + 1] = m.positions[p3 + 1];
            out[o9 + 2] = m.positions[p3 + 2];

            // normal
            out[o9 + 3] = m.normals[p3];
            out[o9 + 4] = m.normals[p3 + 1];
            out[o9 + 5] = m.normals[p3 + 2];

            // uv
            out[o9 + 6] = m.uvs[t2];
            out[o9 + 7] = m.uvs[t2 + 1];

            // material id
            out[o9 + 8] = m.materialId[i];
        }

        return out;
    }
}