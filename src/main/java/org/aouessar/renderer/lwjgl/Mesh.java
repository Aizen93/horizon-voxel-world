package org.aouessar.renderer.lwjgl;

public final class Mesh {

    public final int vao;
    public final int vbo;
    public final int ibo;
    public final int indexCount;

    public Mesh(int vao, int vbo, int ibo, int indexCount) {
        this.vao = vao;
        this.vbo = vbo;
        this.ibo = ibo;
        this.indexCount = indexCount;
    }

}