package org.aouessar.platform.lwjgl.renderer;

import org.aouessar.core.math.Mat4;
import org.aouessar.core.mesh.GreedyMesher;
import org.aouessar.core.streaming.ChunkMeshReady;
import org.aouessar.core.streaming.FarField;
import org.aouessar.core.streaming.FarTileReady;
import org.aouessar.core.streaming.StreamingWorld;
import org.aouessar.core.world.*;
import org.aouessar.platform.lwjgl.camera.FlyCamera;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.aouessar.utils.Utils.TILE_SIZE;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.glViewport;

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


    private final FarField far;
    private final ConcurrentHashMap<TilePos, GlMesh> farGpuMeshes = new ConcurrentHashMap<>();
    int sea = 72;
    private int lastFarCx = Integer.MIN_VALUE;
    private int lastFarCz = Integer.MIN_VALUE;
    private float farCatchupSeconds = 0f;
    private float prevCamX, prevCamZ;
    private float camVx, camVz;
    private float camSpeed;






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
        WorldGenerator gen = new SimpleWorldGenerator(seed, sea);

        //world = new StreamingWorld(gen, new FaceCullingMesher(), Math.max(2, Runtime.getRuntime().availableProcessors() - 1)); // World using Face Culling
        world = new StreamingWorld(gen, new GreedyMesher(), Math.max(2, Runtime.getRuntime().availableProcessors() - 1)); // World using Greedy Mesher
        // new:
        far = new FarField(gen, sea, Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
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

        float x = camera.position.x;
        float z = camera.position.z;

        if (dt > 0f) {
            camVx = (x - prevCamX) / dt;
            camVz = (z - prevCamZ) / dt;
            camSpeed = (float) Math.sqrt(camVx * camVx + camVz * camVz);
        }

        prevCamX = x;
        prevCamZ = z;

        // 1) Request chunks around camera
        requestRingAroundCamera();
        requestFarTilesAroundCamera();
        unloadFarChunks();

        // 2) Poll completed meshes and upload a few per frame
        List<ChunkMeshReady> ready = world.pollReadyMeshes(meshUploadPerFrame);
        for (ChunkMeshReady r : ready) {
            var m = r.mesh();
            // empty mesh (air chunk) => skip
            if (m.indexCount() == 0) continue;

            float[] inter = MeshInterop.toInterleaved(m);
            float minX = r.pos().cx() * Chunk.SIZE;
            float minY = r.pos().cy() * Chunk.SIZE;
            float minZ = r.pos().cz() * Chunk.SIZE;
            float maxX = minX + Chunk.SIZE;
            float maxY = minY + Chunk.SIZE;
            float maxZ = minZ + Chunk.SIZE;

            GlMesh gl = new GlMesh(inter, m.indices, minX, minY, minZ, maxX, maxY, maxZ);

            GlMesh old = gpuMeshes.put(r.pos(), gl);
            if (old != null) old.dispose();
        }

        int cx = floorDiv((int) camera.position.x, TILE_SIZE);
        int cz = floorDiv((int) camera.position.z, TILE_SIZE);

        if (farGpuMeshes.size() > 600) {
            evictFarTiles(cx, cz, 20);
        }

        int uploads = 6;
        if (camSpeed > 600f)  uploads = 20;
        if (camSpeed > 1200f) uploads = 40;
        if (camSpeed > 2000f) uploads = 60;

        for (FarTileReady r : far.pollReady(uploads)) {
            var m = r.mesh();
            if (m.indexCount() == 0) continue;

            float[] inter = MeshInterop.toInterleaved(m);

            float minX = r.pos().tx() * TILE_SIZE;
            float minZ = r.pos().tz() * TILE_SIZE;
            float maxX = minX + TILE_SIZE;
            float maxZ = minZ + TILE_SIZE;

            float minY = -512f;
            float maxY = 2048f;

            GlMesh gl = new GlMesh(inter, m.indices,
                    minX, minY, minZ,
                    maxX, maxY, maxZ
            );

            GlMesh old = farGpuMeshes.put(r.pos(), gl);
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
        shader.setVec3("uCameraPos", camera.position.x, camera.position.y, camera.position.z);

        // draw FAR first
        for (GlMesh mesh : farGpuMeshes.values()) {
            if (frustum.aabbVisible(mesh.minX,mesh.minY,mesh.minZ, mesh.maxX,mesh.maxY,mesh.maxZ)) {
                mesh.draw();
            }
        }

        // draw NEAR after (so near hides far seams)
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
        for (GlMesh m : farGpuMeshes.values()) m.dispose();
        farGpuMeshes.clear();
        far.shutdown();

        for (GlMesh m : gpuMeshes.values()) m.dispose();
        gpuMeshes.clear();
        shader.dispose();
        world.shutdown();
    }

    private void requestFarTilesAroundCamera() {
        final int TILE = TILE_SIZE;
        float lookAhead = Math.min(12000f, 2.5f * camSpeed);
        float dirX = (camSpeed > 0.001f) ? (camVx / camSpeed) : 0f;
        float dirZ = (camSpeed > 0.001f) ? (camVz / camSpeed) : 0f;

        float preX = camera.position.x + dirX * lookAhead;
        float preZ = camera.position.z + dirZ * lookAhead;

        int cx = floorDiv((int)Math.floor(preX), TILE);
        int cz = floorDiv((int)Math.floor(preZ), TILE);

        final int keepRadius = 70;   // keep a bit more than the far ring

        // Detect center shift (moving into a new tile)
        if (cx != lastFarCx || cz != lastFarCz) {
            lastFarCx = cx;
            lastFarCz = cz;

            // Purge stale work so the scheduler focuses on the new area immediately
            far.dropPendingOutside(cx, cz, keepRadius);
            far.dropReadyOutside(cx, cz, keepRadius); // add this method below

            // Trigger a short burst so far tiles catch up quickly
            farCatchupSeconds = 0.8f;
        }

        // Always evict GPU meshes outside keep radius
        evictFarTiles(cx, cz, keepRadius);

        // Budgets: burst when we shifted, otherwise normal
        int requestBudget = (farCatchupSeconds > 0f) ? 600 : 200;

        // Schedule rings (same as before, but with a larger outer band)
        enqueueBand(cx, cz, 0, 0, 2, 0, requestBudget);        // LOD0
        enqueueBand(cx, cz, 1, 2, 5, 10_000, requestBudget);   // LOD1
        enqueueBand(cx, cz, 2, 5, 12, 20_000, requestBudget);  // LOD2
        enqueueBand(cx, cz, 3, 16, 64, 30_000, requestBudget); // LOD3
    }


    private void enqueueBand(int cx, int cz, int lod, int rMin, int rMax, int lodWeight, int budget) {
        int rMin2 = rMin * rMin;
        int rMax2 = rMax * rMax;

        for (int dz = -rMax; dz <= rMax; dz++) {
            for (int dx = -rMax; dx <= rMax; dx++) {
                int d2 = dx*dx + dz*dz;
                if (d2 < rMin2 || d2 > rMax2) continue;
                if (budget-- <= 0) return;

                TilePos p = new TilePos(lod, cx + dx, cz + dz);
                if (farGpuMeshes.containsKey(p)) continue;
                if (far.isInFlight(p)) continue;

                int seamBonus = (d2 - rMin2); // small => closer to seam => earlier
                int priority = lodWeight + d2 * 100 + seamBonus;

                far.enqueue(p, priority);
            }
        }
    }

    private void evictFarTiles(int cx, int cz, int maxRadius) {
        int r2 = maxRadius * maxRadius;
        farGpuMeshes.forEach((pos, mesh) -> {
            int dx = pos.tx() - cx;
            int dz = pos.tz() - cz;
            if (dx*dx + dz*dz > r2) {
                if (farGpuMeshes.remove(pos, mesh)) {
                    mesh.dispose();
                }
            }
        });
    }


    // -------- Shaders --------
    private static final String VS = """
        #version 330 core
        layout(location=0) in vec3 aPos;
        layout(location=1) in vec3 aNrm;
        layout(location=2) in vec2 aUv;
        layout(location=3) in float aMat;

        uniform mat4 uVP;

        out vec3 vWorldPos;
        out vec3 vNrm;
        flat out float vMat;

        void main() {
          vWorldPos = aPos;
          vNrm = aNrm;
          vMat = aMat;
          gl_Position = uVP * vec4(aPos, 1.0);
        }
    """;

    private static final String FS = """
        #version 330 core

        in vec3 vWorldPos;
        in vec3 vNrm;
        flat in float vMat;

        uniform vec3 uCameraPos;

        out vec4 FragColor;

        vec3 materialColor(float id, float height) {
            id = abs(id);

            if (id < 0.5) return vec3(0.0); // air

            float h = clamp((height - 20.0) / 120.0, 0.0, 1.0);

            if (id < 1.5) return mix(vec3(0.12, 0.55, 0.18), vec3(0.25, 0.80, 0.30), h); // grass
            if (id < 2.5) return vec3(0.50, 0.32, 0.16); // dirt
            if (id < 3.5) return vec3(0.62, 0.62, 0.66); // stone
            return vec3(0.10, 0.35, 0.75); // water
        }

        void main() {
            vec3 n = normalize(vNrm);

            vec3 lightDir = normalize(vec3(0.6, 1.0, 0.2));
            float ndl = max(dot(n, lightDir), 0.12);

            float upness = clamp(dot(n, vec3(0,1,0)), 0.0, 1.0);
            float slopeDark = mix(0.65, 1.0, upness);

            float fakeHeight = 60.0;
            vec3 base = materialColor(vMat, fakeHeight);

            vec3 color = base * ndl * slopeDark;

            // Fog (world-distance)
            float dist = length(vWorldPos - uCameraPos);
            float fog = clamp((dist - 600.0) / 1200.0, 0.0, 1.0);
            vec3 fogColor = vec3(0.55, 0.75, 0.95);

            color = mix(color, fogColor, fog);

            FragColor = vec4(color, 1.0);
        }
    """;
}
