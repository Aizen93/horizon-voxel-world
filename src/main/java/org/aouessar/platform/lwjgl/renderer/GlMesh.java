package org.aouessar.platform.lwjgl.renderer;

import static org.lwjgl.opengl.GL33.*;

public final class GlMesh {
    public final int vao, vbo, ibo;
    public final int indexCount;
    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;


    public GlMesh(float[] interleaved, int[] indices,
                  float minX,float minY,float minZ,
                  float maxX,float maxY,float maxZ) {

        this.indexCount = indices.length;
        this.minX=minX; this.minY=minY; this.minZ=minZ;
        this.maxX=maxX; this.maxY=maxY; this.maxZ=maxZ;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, interleaved, GL_STATIC_DRAW);

        ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        int stride = 9 * Float.BYTES;

        // position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);

        // normal
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);

        // uv
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);

        // material id (float for now)
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 8L * Float.BYTES);

        glBindVertexArray(0);
    }

    public void draw() {
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
