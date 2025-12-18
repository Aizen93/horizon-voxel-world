package org.aouessar.core.mesh;

public final class MeshData {

    // x,y,z per vertex
    public final float[] positions;

    // x,y,z per vertex
    public final float[] normals;

    // u,v per vertex
    public final float[] uvs;

    // triangles
    public final int[] indices;

    // per vertex (simple for now)
    public final short[] materialId;


    public MeshData(float[] positions, float[] normals, float[] uvs, int[] indices, short[] materialId) {
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.indices = indices;
        this.materialId = materialId;
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    public int indexCount() {
        return indices.length;
    }
}
