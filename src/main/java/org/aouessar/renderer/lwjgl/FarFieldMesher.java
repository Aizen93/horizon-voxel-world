package org.aouessar.renderer.lwjgl;

import org.aouessar.core.world.layer.Heightmap;
import org.aouessar.core.world.layer.LayerRect;

import java.util.ArrayList;

public final class FarFieldMesher {

    public ChunkMesher.MeshBuffers buildHeightMesh(Heightmap hm, int step) {
        LayerRect r = hm.rect();

        ArrayList<Float> v = new ArrayList<>();
        ArrayList<Integer> i = new ArrayList<>();
        int base = 0;

        // grid over heightmap with stride "step"
        for (int z = r.minZ(); z < r.minZ() + r.sizeZ() - step; z += step) {
            for (int x = r.minX(); x < r.minX() + r.sizeX() - step; x += step) {
                float h00 = hm.heightAt(x, z);
                float h10 = hm.heightAt(x + step, z);
                float h11 = hm.heightAt(x + step, z + step);
                float h01 = hm.heightAt(x, z + step);

                // 4 vertices: pos(3) uv(2) – UV not used, set 0
                addV(v, x,        h00, z,        0, 0);
                addV(v, x + step, h10, z,        0, 0);
                addV(v, x + step, h11, z + step, 0, 0);
                addV(v, x,        h01, z + step, 0, 0);

                i.add(base); i.add(base+1); i.add(base+2);
                i.add(base); i.add(base+2); i.add(base+3);
                base += 4;
            }
        }

        float[] verts = new float[v.size()];
        for (int k = 0; k < v.size(); k++) verts[k] = v.get(k);

        int[] inds = new int[i.size()];
        for (int k = 0; k < i.size(); k++) inds[k] = i.get(k);

        return new ChunkMesher.MeshBuffers(verts, inds);
    }

    private void addV(ArrayList<Float> v, float x, float y, float z, float u, float vv) {
        v.add(x); v.add(y); v.add(z);
        v.add(u); v.add(vv);
    }
}