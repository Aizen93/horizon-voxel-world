package org.aouessar.renderer.lwjgl;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public final class TextureUtils {
    private TextureUtils(){}

    public static int loadTexture2D(String resourcePath) {
        ByteBuffer image;
        int w, h;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer iw = stack.mallocInt(1);
            IntBuffer ih = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(false);

            var in = TextureUtils.class.getResourceAsStream(resourcePath);
            if (in == null) throw new IllegalStateException("Missing resource: " + resourcePath);

            byte[] bytes;
            try {
                bytes = in.readAllBytes();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read: " + resourcePath, e);
            }

            ByteBuffer data = org.lwjgl.system.MemoryUtil.memAlloc(bytes.length);
            data.put(bytes).flip();

            image = STBImage.stbi_load_from_memory(data, iw, ih, comp, 4);
            org.lwjgl.system.MemoryUtil.memFree(data);

            if (image == null) throw new IllegalStateException("stbi_load failed: " + STBImage.stbi_failure_reason());
            w = iw.get(0);
            h = ih.get(0);
        }

        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        STBImage.stbi_image_free(image);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);
        return tex;
    }
}