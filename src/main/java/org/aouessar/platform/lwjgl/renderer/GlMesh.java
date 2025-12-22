package org.aouessar.platform.lwjgl.renderer;

import static org.lwjgl.opengl.GL33.*;

/**
 * GPU mesh wrapper.
 *
 * Interleaved vertex layout: 9 floats per vertex
 *  - 0..2  position xyz
 *  - 3..5  normal xyz
 *  - 6..7  uv xy
 *  - 8     materialId (float)
 *
 * Must match shader inputs:
 *  layout(location=0) in vec3 aPos;
 *  layout(location=1) in vec3 aNrm;
 *  layout(location=2) in vec2 aUv;
 *  layout(location=3) in float aMat;
 */
public final class GlMesh {

    public final int vao, vbo, ibo;
    public final int indexCount;

    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    private static final int FLOATS_PER_VERTEX = 9;
    private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * Float.BYTES;

    public GlMesh(
            float[] interleaved,
            int[] indices,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ
    ) {
        this.indexCount = indices.length;

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ibo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, interleaved, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // position (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE_BYTES, 0L);
        glEnableVertexAttribArray(0);

        // normal (location = 1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE_BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        // uv (location = 2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, STRIDE_BYTES, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        // materialId (location = 3)
        glVertexAttribPointer(3, 1, GL_FLOAT, false, STRIDE_BYTES, 8L * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    public void draw() {
        if (indexCount == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ibo);
        glDeleteVertexArrays(vao);
    }
}