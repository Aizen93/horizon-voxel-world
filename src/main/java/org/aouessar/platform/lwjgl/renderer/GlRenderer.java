package org.aouessar.platform.lwjgl.renderer;

import org.aouessar.core.math.Mat4;
import org.aouessar.core.mesh.FaceCullingMesher;
import org.aouessar.core.streaming.ChunkMeshReady;
import org.aouessar.core.streaming.StreamingWorld;
import org.aouessar.core.world.*;
import org.aouessar.platform.lwjgl.camera.FlyCamera;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public final class GlRenderer {
    private final long window;
    private final FlyCamera camera = new FlyCamera();

    private final ShaderProgram shader;

    private int width = 1280, height = 720;

    // World (core)
    private final StreamingWorld world;

    // GPU meshes keyed by chunk pos
    private final ConcurrentHashMap<ChunkPos, GlMesh> gpuMeshes = new ConcurrentHashMap<>();

    private final int keepRadiusChunks = 12;   // keep in memory & GPU
    private final int requestRadiusChunks = 10; // request/generate
    private final int unloadMargin = 2;         // hysteresis to avoid thrashing

    // Tuning
    //private final int viewRadiusChunks = 10;     // near-field radius in chunks
    private final int worldHeightChunks = 4;     // 4 * 32 = 128 world height
    private final int meshUploadPerFrame = 8;    // throttle uploads to avoid spikes

    private final Frustum frustum = new Frustum();

    public GlRenderer(long window) {
        this.window = window;

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        shader = new ShaderProgram(VS, FS);

        glfwSetFramebufferSizeCallback(window, (w, newW, newH) -> {
            width = Math.max(newW, 1);
            height = Math.max(newH, 1);
            glViewport(0, 0, width, height);
        });
        glViewport(0, 0, width, height);

        // Core world setup
        WorldSeed seed = new WorldSeed(123456789L);
        WorldGenerator gen = new SimpleWorldGenerator(seed, 48);
        world = new StreamingWorld(gen, new FaceCullingMesher(), Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
    }

    public void render(float dt) {
        // Mouse capture toggle
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }

        camera.handleMouse(window);
        camera.updateMovement(window, dt);

        // 1) Request chunks around camera
        requestRingAroundCamera();
        unloadFarChunks();

        // 2) Poll completed meshes and upload a few per frame
        List<ChunkMeshReady> ready = world.pollReadyMeshes(meshUploadPerFrame);
        for (ChunkMeshReady r : ready) {
            var m = r.mesh();
            // empty mesh (air chunk) => skip
            if (m.indexCount() == 0) continue;

            float[] inter = MeshInterop.toInterleavedPosNrmUv(m);
            float minX = r.pos().cx() * Chunk.SIZE;
            float minY = r.pos().cy() * Chunk.SIZE;
            float minZ = r.pos().cz() * Chunk.SIZE;
            float maxX = minX + Chunk.SIZE;
            float maxY = minY + Chunk.SIZE;
            float maxZ = minZ + Chunk.SIZE;

            GlMesh gl = new GlMesh(inter, m.indices, minX,minY,minZ, maxX,maxY,maxZ);

            GlMesh old = gpuMeshes.put(r.pos(), gl);
            if (old != null) old.dispose();
        }

        // 3) Render all uploaded chunk meshes
        shader.use();
        float aspect = (float) width / (float) height;
        Mat4 proj = Mat4.perspective((float)Math.toRadians(75.0), aspect, 0.1f, 5000f);
        Mat4 view = camera.viewMatrix();
        Mat4 vp = Mat4.mul(proj, view);
        frustum.setFromMatrix(vp.m);
        shader.setMat4("uVP", vp.m);


        for (GlMesh mesh : gpuMeshes.values()) {
            if (frustum.aabbVisible(mesh.minX, mesh.minY, mesh.minZ, mesh.maxX, mesh.maxY, mesh.maxZ)) {
                mesh.draw();
            }
        }
    }

    private void requestRingAroundCamera() {
        int cx = floorDiv((int)Math.floor(camera.position.x), Chunk.SIZE);
        int cz = floorDiv((int)Math.floor(camera.position.z), Chunk.SIZE);

        for (int dz = -requestRadiusChunks; dz <= requestRadiusChunks; dz++) {
            for (int dx = -requestRadiusChunks; dx <= requestRadiusChunks; dx++) {
                int dist2 = dx*dx + dz*dz;
                if (dist2 > requestRadiusChunks * requestRadiusChunks) continue;

                int rcx = cx + dx;
                int rcz = cz + dz;

                for (int cy = 0; cy < worldHeightChunks; cy++) {
                    world.requestChunk(new ChunkPos(rcx, cy, rcz));
                }
            }
        }
    }

    private void unloadFarChunks() {
        int cx = floorDiv((int)Math.floor(camera.position.x), Chunk.SIZE);
        int cz = floorDiv((int)Math.floor(camera.position.z), Chunk.SIZE);

        int maxDist = keepRadiusChunks + unloadMargin;
        int maxDist2 = maxDist * maxDist;

        // Remove GPU meshes outside radius
        gpuMeshes.forEach((pos, mesh) -> {
            int dx = pos.cx() - cx;
            int dz = pos.cz() - cz;
            if (dx*dx + dz*dz > maxDist2) {
                if (gpuMeshes.remove(pos, mesh)) {
                    mesh.dispose();
                }
            }
        });

        // Optional (recommended): also drop voxel chunk data from CPU RAM
        world.evictChunksOutside(cx, cz, maxDist2);
    }


    private static int floorDiv(int a, int b) {
        int r = a / b;
        // Java / truncates toward 0; adjust for negatives
        if ((a ^ b) < 0 && (r * b != a)) r--;
        return r;
    }

    public void dispose() {
        for (GlMesh m : gpuMeshes.values()) m.dispose();
        gpuMeshes.clear();
        shader.dispose();
        world.shutdown();
    }

    // -------- Shaders --------
    private static final String VS = """
    #version 330 core
    layout(location=0) in vec3 aPos;
    layout(location=1) in vec3 aNrm;
    layout(location=2) in vec2 aUv;

    uniform mat4 uVP;

    out vec3 vNrm;
    out vec2 vUv;

    void main() {
      vNrm = aNrm;
      vUv = aUv;
      gl_Position = uVP * vec4(aPos, 1.0);
    }
  """;

    private static final String FS = """
    #version 330 core
    in vec3 vNrm;
    in vec2 vUv;

    out vec4 FragColor;

    void main() {
      vec3 n = normalize(vNrm);
      vec3 lightDir = normalize(vec3(0.6, 1.0, 0.2));
      float ndl = max(dot(n, lightDir), 0.15);

      float sx = floor(vUv.x * 8.0);
      float sy = floor(vUv.y * 8.0);
      float c = mod(sx + sy, 2.0);

      vec3 base = mix(vec3(0.18,0.65,0.25), vec3(0.35,0.25,0.18), c);
      FragColor = vec4(base * ndl, 1.0);
    }
  """;
}
