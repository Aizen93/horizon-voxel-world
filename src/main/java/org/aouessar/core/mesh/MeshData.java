package org.aouessar.core.mesh;

/**
 * Raw mesh data produced by meshers and consumed by the renderer.
 *
 * Vertex layout (separate arrays):
 * - positions: 3 floats per vertex (x,y,z)
 * - normals:   3 floats per vertex (x,y,z)
 * - uvs:       2 floats per vertex (u,v)
 * - materialId: 1 float per vertex (material/block id packed as float)
 *
 * Indices: triangle indices into vertex arrays.
 */
public final class MeshData {

    // x,y,z per vertex
    public final float[] positions;

    // x,y,z per vertex
    public final float[] normals;

    // u,v per vertex
    public final float[] uvs;

    // 1 float per vertex (material id)
    public final float[] materialId;

    // triangles
    public final int[] indices;

    public MeshData(float[] positions, float[] normals, float[] uvs, int[] indices, float[] materialId) {
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