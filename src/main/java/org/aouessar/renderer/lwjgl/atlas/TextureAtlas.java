package org.aouessar.renderer.lwjgl.atlas;

import java.util.Map;

public record TextureAtlas(
        int width,
        int height,
        Map<String, UV> uvs) {

    public record UV(float u0, float v0, float u1, float v1) {}

}