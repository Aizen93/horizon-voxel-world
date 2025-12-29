package org.aouessar.platform.lwjgl.renderer;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

final class GlTexture {
    private final int id;

    GlTexture(String resourcePath, boolean flipVertically) {
        ByteBuffer image = loadResource(resourcePath);

        STBImage.stbi_set_flip_vertically_on_load(flipVertically);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = STBImage.stbi_load_from_memory(image, width, height, channels, 4);
            memFree(image);

            if (data == null) {
                throw new IllegalStateException("Failed to load texture " + resourcePath + ": " + STBImage.stbi_failure_reason());
            }

            id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            STBImage.stbi_image_free(data);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    void dispose() {
        glDeleteTextures(id);
    }

    private static ByteBuffer loadResource(String resourcePath) {
        try (InputStream stream = GlTexture.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing resource: " + resourcePath);
            }
            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource: " + resourcePath, e);
        }
    }
}
