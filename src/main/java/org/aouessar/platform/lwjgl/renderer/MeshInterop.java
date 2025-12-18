package org.aouessar.platform.lwjgl.renderer;

import org.aouessar.core.mesh.MeshData;

final class MeshInterop {
    private MeshInterop() {}

    static float[] toInterleavedPosNrmUv(MeshData m) {
        int v = m.vertexCount();
        float[] out = new float[v * 8];
        for (int i = 0; i < v; i++) {
            int p3 = i * 3;
            int t2 = i * 2;
            int o8 = i * 8;

            out[o8]     = m.positions[p3];
            out[o8 + 1] = m.positions[p3 + 1];
            out[o8 + 2] = m.positions[p3 + 2];

            out[o8 + 3] = m.normals[p3];
            out[o8 + 4] = m.normals[p3 + 1];
            out[o8 + 5] = m.normals[p3 + 2];

            out[o8 + 6] = m.uvs[t2];
            out[o8 + 7] = m.uvs[t2 + 1];
        }
        return out;
    }
}

