package org.aouessar.platform.lwjgl.renderer;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

public final class ShaderProgram {
    public final int programId;

    public ShaderProgram(String vsSource, String fsSource) {
        int vs = compile(GL_VERTEX_SHADER, vsSource);
        int fs = compile(GL_FRAGMENT_SHADER, fsSource);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Program link failed: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Shader compile failed: " + glGetShaderInfoLog(id));
        }
        return id;
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setMat4(String name, float[] m16) {
        int loc = glGetUniformLocation(programId, name);
        if (loc < 0) return;
        FloatBuffer fb = memAllocFloat(16);
        fb.put(m16).flip();
        glUniformMatrix4fv(loc, false, fb);
        memFree(fb);
    }

    public void setVec3(String name, float x, float y, float z) {
        int loc = glGetUniformLocation(programId, name);
        if (loc < 0) return;
        glUniform3f(loc, x, y, z);
    }

    public void setFloat(String name, float v) {
        int loc = glGetUniformLocation(programId, name);
        if (loc < 0) return;
        glUniform1f(loc, v);
    }

    public void setInt(String name, int v) {
        int loc = glGetUniformLocation(programId, name);
        if (loc < 0) return;
        glUniform1i(loc, v);
    }

    public void setVec4Array(String name, float[] data, int count) {
        int loc = glGetUniformLocation(programId, name + "[0]");
        if (loc < 0) return;
        FloatBuffer fb = memAllocFloat(count * 4);
        fb.put(data, 0, count * 4).flip();
        glUniform4fv(loc, fb);
        memFree(fb);
    }

    public void dispose() {
        glDeleteProgram(programId);
    }
}
