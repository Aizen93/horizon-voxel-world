package org.aouessar.renderer.lwjgl;

import static org.lwjgl.opengl.GL33.*;

public final class Shaders {
    private Shaders(){}

    public static int createProgram(String vs, String fs) {
        int v = compile(GL_VERTEX_SHADER, vs);
        int f = compile(GL_FRAGMENT_SHADER, fs);
        int p = glCreateProgram();
        glAttachShader(p, v);
        glAttachShader(p, f);
        glLinkProgram(p);

        if (glGetProgrami(p, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(p);
            throw new IllegalStateException("Program link failed:\n" + log);
        }

        glDeleteShader(v);
        glDeleteShader(f);
        return p;
    }

    private static int compile(int type, String src) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(s);
            throw new IllegalStateException("Shader compile failed:\n" + log);
        }
        return s;
    }
}