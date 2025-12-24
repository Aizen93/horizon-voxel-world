package org.aouessar.renderer.lwjgl;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public final class GLMesh {

    private GLMesh(){}

    public static Mesh upload(float[] vertices, int[] indices) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ibo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer vb = MemoryUtil.memAllocFloat(vertices.length);
        vb.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        MemoryUtil.memFree(vb);

        IntBuffer ib = MemoryUtil.memAllocInt(indices.length);
        ib.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        MemoryUtil.memFree(ib);

        int stride = 5 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

        return new Mesh(vao, vbo, ibo, indices.length);
    }

    public static void draw(Mesh m) {
        glBindVertexArray(m.vao);
        glDrawElements(GL_TRIANGLES, m.indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public static void destroy(Mesh m) {
        glDeleteBuffers(m.vbo);
        glDeleteBuffers(m.ibo);
        glDeleteVertexArrays(m.vao);
    }
}