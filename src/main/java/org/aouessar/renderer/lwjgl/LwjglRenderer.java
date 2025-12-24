package org.aouessar.renderer.lwjgl;

import org.aouessar.core.util.EngineConstants;
import org.aouessar.core.world.chunk.Chunk;
import org.aouessar.renderer.api.Camera;
import org.aouessar.renderer.api.RenderWorldView;
import org.aouessar.renderer.api.Renderer;
import org.aouessar.renderer.lwjgl.atlas.AtlasLoader;
import org.aouessar.renderer.lwjgl.atlas.TextureAtlas;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class LwjglRenderer implements Renderer {

    private final RenderWorldView world;
    private final Camera camera = new Camera();

    private long window;

    private TextureAtlas atlas;
    private int atlasTex;

    private int program;
    private int uViewProj;
    private int uChunkOffset;

    // Simple caches for demo
    private final java.util.Map<Long, Mesh> chunkMeshes = new java.util.HashMap<>();
    private Mesh farMesh;

    public LwjglRenderer(RenderWorldView world) {
        this.world = world;
    }

    @Override
    public void run() {
        initWindow();
        initGL();
        initResources();
        loop();
        cleanup();
    }

    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) throw new IllegalStateException("Failed to init GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(1600, 900, "Voxel Engine", NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Failed to create window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    private void initGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    private void initResources() {
        atlas = AtlasLoader.load("/atlas.json");
        atlasTex = TextureUtils.loadTexture2D("/atlas.png");

        program = Shaders.createProgram(VERT, FRAG);
        glUseProgram(program);

        uViewProj = glGetUniformLocation(program, "uViewProj");
        uChunkOffset = glGetUniformLocation(program, "uChunkOffset");

        int uTex = glGetUniformLocation(program, "uAtlas");
        glUniform1i(uTex, 0);

        // Build far mesh once
        FarFieldMesher ff = new FarFieldMesher();
        var buf = ff.buildHeightMesh(world.heightmap(), 8); // step=8 => cheap LOD
        farMesh = GLMesh.upload(buf.vertices(), buf.indices());
    }

    private void loop() {
        long last = System.nanoTime();

        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;

            glfwPollEvents();
            updateCamera(dt);

            int[] w = new int[1], h = new int[1];
            glfwGetFramebufferSize(window, w, h);
            glViewport(0, 0, w[0], h[0]);

            glClearColor(0.55f, 0.75f, 1.0f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glUseProgram(program);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, atlasTex);

            Matrix4f vp = camera.projMatrix((float)w[0] / (float)h[0]).mul(camera.viewMatrix());
            FloatBuffer fb = org.lwjgl.system.MemoryStack.stackPush().mallocFloat(16);
            vp.get(fb);
            glUniformMatrix4fv(uViewProj, false, fb);
            org.lwjgl.system.MemoryStack.stackPop();

            // --- Far field (no atlas usage, but shader expects it; UV=0 ok)
            glUniform3f(uChunkOffset, 0, 0, 0);
            GLMesh.draw(farMesh);

            // --- Near field chunks
            int radius = 6; // tweak
            int cx0 = (int)Math.floor(camera.position.x / EngineConstants.CHUNK_SIZE);
            int cz0 = (int)Math.floor(camera.position.z / EngineConstants.CHUNK_SIZE);

            ChunkMesher mesher = new ChunkMesher();

            for (int cz = cz0 - radius; cz <= cz0 + radius; cz++) {
                for (int cx = cx0 - radius; cx <= cx0 + radius; cx++) {
                    long key = (((long)cx) << 32) ^ (cz & 0xFFFFFFFFL);
                    Mesh m = chunkMeshes.get(key);
                    if (m == null) {
                        Chunk c = world.chunkAt(cx, cz);
                        var mb = mesher.build(c, atlas);
                        m = GLMesh.upload(mb.vertices(), mb.indices());
                        chunkMeshes.put(key, m);
                    }

                    glUniform3f(uChunkOffset,
                            cx * EngineConstants.CHUNK_SIZE,
                            0f,
                            cz * EngineConstants.CHUNK_SIZE);

                    GLMesh.draw(m);
                }
            }

            glfwSwapBuffers(window);
        }
    }

    private void updateCamera(float dt) {
        float speed = 40f * dt;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) camera.position.fma(speed, camera.forward());
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) camera.position.fma(-speed, camera.forward());
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) camera.position.fma(speed, camera.right());
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) camera.position.fma(-speed, camera.right());
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) camera.position.y += speed;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) camera.position.y -= speed;

        // mouse look
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        glfwSetCursorPos(window, 800, 450);
        float dx = (float)(mx[0] - 800);
        float dy = (float)(my[0] - 450);

        float sens = 0.0025f;
        camera.yaw += dx * sens;
        camera.pitch += dy * sens;
        camera.pitch = Math.max((float)-Math.PI/2f + 0.01f, Math.min((float)Math.PI/2f - 0.01f, camera.pitch));
    }

    private void cleanup() {
        for (Mesh m : chunkMeshes.values()) GLMesh.destroy(m);
        if (farMesh != null) GLMesh.destroy(farMesh);
        glDeleteProgram(program);
        glDeleteTextures(atlasTex);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private static final String VERT = """
        #version 330 core
        layout(location=0) in vec3 aPos;
        layout(location=1) in vec2 aUv;

        uniform mat4 uViewProj;
        uniform vec3 uChunkOffset;

        out vec2 vUv;

        void main(){
          vec3 wp = aPos + uChunkOffset;
          vUv = aUv;
          gl_Position = uViewProj * vec4(wp, 1.0);
        }
        """;

    private static final String FRAG = """
        #version 330 core
        in vec2 vUv;
        uniform sampler2D uAtlas;
        out vec4 FragColor;

        void main(){
          vec4 c = texture(uAtlas, vUv);
          if (c.a < 0.1) discard;
          FragColor = c;
        }
        """;
}