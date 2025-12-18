package org.aouessar.platform.lwjgl.renderer;

import org.aouessar.core.mesh.MeshData;

final class MeshInterop {
    private MeshInterop() {}

    static float[] toInterleaved(MeshData m) {
        int v = m.vertexCount();
        float[] out = new float[v * 9];

        for (int i = 0; i < v; i++) {
            int p3 = i * 3;
            int t2 = i * 2;
            int o9 = i * 9;

            out[o9]     = m.positions[p3];
            out[o9 + 1] = m.positions[p3 + 1];
            out[o9 + 2] = m.positions[p3 + 2];

            out[o9 + 3] = m.normals[p3];
            out[o9 + 4] = m.normals[p3 + 1];
            out[o9 + 5] = m.normals[p3 + 2];

            out[o9 + 6] = m.uvs[t2];
            out[o9 + 7] = m.uvs[t2 + 1];

            out[o9 + 8] = m.materialId[i];
        }
        return out;
    }
}

