package org.aouessar.platform.lwjgl;

import org.aouessar.platform.lwjgl.renderer.GlRenderer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class LwjglApp {

    private long window;

    public static void main(String[] args) {
        new LwjglApp().run();
    }

    public void run() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(1280, 720, "VoxelHorizon (Java 21)", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0); // uncapped, you can set 1 later
        glfwShowWindow(window);

        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        GlRenderer renderer = new GlRenderer(window);

        int frames = 0;
        double acc = 0.0;
        double last = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float)(now - last);
            last = now;

            acc += dt;
            frames++;
            if (acc >= 1.0) {
                glfwSetWindowTitle(window, "Horizon (FPS: " + frames + ")");
                frames = 0;
                acc = 0.0;
            }

            glfwPollEvents();

            glClearColor(0.55f, 0.75f, 0.95f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            renderer.render(dt);

            glfwSwapBuffers(window);
        }

        renderer.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}

