package org.aouessar.platform.lwjgl.renderer;

import org.aouessar.core.math.Mat4;
import org.aouessar.core.mesh.GreedyMesher;
import org.aouessar.core.streaming.ChunkMeshReady;
import org.aouessar.core.streaming.FarField;
import org.aouessar.core.streaming.FarTileReady;
import org.aouessar.core.streaming.StreamingWorld;
import org.aouessar.core.world.*;
import org.aouessar.platform.lwjgl.camera.FlyCamera;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.aouessar.utils.Utils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.glViewport;

public final class GlRenderer {

    private final long window;
    private final FlyCamera camera = new FlyCamera();
    private final ShaderProgram shader;
    private int width = 1280, height = 720;

    // World (core)
    private final StreamingWorld world;
    private final WorldGenerator generator;

    // GPU meshes keyed by chunk pos
    private final ConcurrentHashMap<ChunkPos, GlMesh> gpuMeshes = new ConcurrentHashMap<>();

    // Near-field streaming tuning
    private final int keepRadiusChunks = 12;    // keep in memory & GPU
    private final int requestRadiusChunks = 10; // request/generate
    private final int unloadMargin = 2;         // hysteresis to avoid thrashing
    private final int meshUploadPerFrame = 8;   // throttle near uploads to avoid spikes

    private final int worldHeightChunks;

    private final Frustum frustum = new Frustum();

    // Far field
    private final FarField far;
    private final ConcurrentHashMap<TilePos, GlMesh> farGpuMeshes = new ConcurrentHashMap<>();

    private int lastFarCx = Integer.MIN_VALUE;
    private int lastFarCz = Integer.MIN_VALUE;
    private float farCatchupSeconds = 0f;

    // camera velocity/speed
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

        this.worldHeightChunks = (WORLD_MAX_Y / CHUNK_SIZE) + 2;

        WorldSeed seed = new WorldSeed(555555555L);
        this.generator = new SimpleWorldGenerator(seed, SEA_LEVEL);

        world = new StreamingWorld(generator, new GreedyMesher(),
                Math.max(2, Runtime.getRuntime().availableProcessors() - 1));

        far = new FarField(generator, SEA_LEVEL,
                Math.max(2, Runtime.getRuntime().availableProcessors() - 1));
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

        // Decay catch-up burst timer
        farCatchupSeconds = Math.max(0f, farCatchupSeconds - dt);

        // camera speed estimate (blocks/sec)
        float x = camera.position.x;
        float z = camera.position.z;

        if (dt > 0f) {
            camVx = (x - prevCamX) / dt;
            camVz = (z - prevCamZ) / dt;
            camSpeed = (float) Math.sqrt(camVx * camVx + camVz * camVz);
        }

        prevCamX = x;
        prevCamZ = z;

        // 1) Request near + far content
        requestRingAroundCamera();
        requestFarTilesAroundCamera();
        unloadFarChunks();

        // 2) Upload NEAR meshes (limited per frame)
        List<ChunkMeshReady> ready = world.pollReadyMeshes(meshUploadPerFrame);
        for (ChunkMeshReady r : ready) {
            var m = r.mesh();
            if (m.indexCount() == 0) continue;

            float[] inter = MeshInterop.toInterleaved(m);

            float minX = r.pos().cx() * CHUNK_SIZE;
            float minY = r.pos().cy() * CHUNK_SIZE;
            float minZ = r.pos().cz() * CHUNK_SIZE;
            float maxX = minX + CHUNK_SIZE;
            float maxY = minY + CHUNK_SIZE;
            float maxZ = minZ + CHUNK_SIZE;

            GlMesh gl = new GlMesh(inter, m.indices, minX, minY, minZ, maxX, maxY, maxZ);

            GlMesh old = gpuMeshes.put(r.pos(), gl);
            if (old != null) old.dispose();
        }

        // 3) Upload FAR meshes (scaled by speed, limited by readiness)
        int uploads = FAR_UPLOAD_BASE;
        if (camSpeed > 600f)  uploads = FAR_UPLOAD_FAST;
        if (camSpeed > 1200f) uploads = FAR_UPLOAD_VERY_FAST;
        if (camSpeed > 2000f) uploads = FAR_UPLOAD_ULTRA;

        for (FarTileReady r : far.pollReady(uploads)) {
            var m = r.mesh();
            if (m.indexCount() == 0) continue;

            float[] inter = MeshInterop.toInterleaved(m);

            float minX = r.pos().tx() * TILE_SIZE;
            float minZ = r.pos().tz() * TILE_SIZE;
            float maxX = minX + TILE_SIZE;
            float maxZ = minZ + TILE_SIZE;

            float minY = WORLD_MIN_Y;
            float maxY = WORLD_MAX_Y + 64f;

            GlMesh gl = new GlMesh(inter, m.indices,
                    minX, minY, minZ,
                    maxX, maxY, maxZ
            );

            GlMesh old = farGpuMeshes.put(r.pos(), gl);
            if (old != null) old.dispose();
        }

        // 4) Render
        shader.use();
        float aspect = (float) width / (float) height;
        Mat4 proj = Mat4.perspective((float) Math.toRadians(75.0), aspect, 0.1f, 5000f);
        Mat4 view = camera.viewMatrix();
        Mat4 vp = Mat4.mul(proj, view);

        frustum.setFromMatrix(vp.m);

        shader.setMat4("uVP", vp.m);
        shader.setVec3("uCameraPos", camera.position.x, camera.position.y, camera.position.z);
        shader.setFloat("uSeaLevel", SEA_LEVEL);
        shader.setFloat("uMaxHeight", WORLD_MAX_Y);

        // draw FAR first
        for (GlMesh mesh : farGpuMeshes.values()) {
            if (frustum.aabbVisible(mesh.minX, mesh.minY, mesh.minZ, mesh.maxX, mesh.maxY, mesh.maxZ)) {
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

    /**
     * Request near-field chunks around camera.
     *
     * IMPORTANT: We DO NOT request cy=0..surfaceCy.
     * That explodes work, tanks FPS, and temporarily exposes chunk-boundary "walls" while neighbors stream in.
     *
     * Instead: request only a small vertical window around the surface: [surfaceCy-1 .. surfaceCy].
     */
    private void requestRingAroundCamera() {
        int cx = floorDiv((int) Math.floor(camera.position.x), CHUNK_SIZE);
        int cz = floorDiv((int) Math.floor(camera.position.z), CHUNK_SIZE);

        final int verticalPaddingBelow = 2;
        final int verticalPaddingAbove = 1;

        for (int dz = -requestRadiusChunks; dz <= requestRadiusChunks; dz++) {
            for (int dx = -requestRadiusChunks; dx <= requestRadiusChunks; dx++) {
                int dist2 = dx * dx + dz * dz;
                if (dist2 > requestRadiusChunks * requestRadiusChunks) continue;

                int rcx = cx + dx;
                int rcz = cz + dz;

                int sampleWx = rcx * CHUNK_SIZE + (CHUNK_SIZE / 2);
                int sampleWz = rcz * CHUNK_SIZE + (CHUNK_SIZE / 2);

                int h = generator.heightAt(sampleWx, sampleWz);
                if (h < WORLD_MIN_Y) h = WORLD_MIN_Y;
                if (h > WORLD_MAX_Y - 1) h = WORLD_MAX_Y - 1;

                int surfaceCy = floorDiv(h, CHUNK_SIZE);

                int cyMin = surfaceCy - verticalPaddingBelow;
                int cyMax = surfaceCy + verticalPaddingAbove;

                if (cyMin < 0) cyMin = 0;
                if (cyMax < 0) cyMax = 0;

                int maxCy = worldHeightChunks - 1;
                if (cyMin > maxCy) cyMin = maxCy;
                if (cyMax > maxCy) cyMax = maxCy;

                for (int cy = cyMin; cy <= cyMax; cy++) {
                    world.requestChunk(new ChunkPos(rcx, cy, rcz));
                }
            }
        }
    }

    private void unloadFarChunks() {
        int cx = floorDiv((int) Math.floor(camera.position.x), CHUNK_SIZE);
        int cz = floorDiv((int) Math.floor(camera.position.z), CHUNK_SIZE);

        int maxDist = keepRadiusChunks + unloadMargin;
        int maxDist2 = maxDist * maxDist;

        // Remove GPU meshes outside radius
        gpuMeshes.forEach((pos, mesh) -> {
            int dx = pos.cx() - cx;
            int dz = pos.cz() - cz;
            if (dx * dx + dz * dz > maxDist2) {
                if (gpuMeshes.remove(pos, mesh)) {
                    mesh.dispose();
                }
            }
        });

        // Drop voxel chunk data from CPU RAM
        world.evictChunksOutside(cx, cz, maxDist2);
    }

    private void requestFarTilesAroundCamera() {
        final int TILE = TILE_SIZE;
        float lookAhead = Math.min(FAR_LOOKAHEAD_MAX, FAR_LOOKAHEAD_FACTOR * camSpeed);
        float dirX = (camSpeed > 0.001f) ? (camVx / camSpeed) : 0f;
        float dirZ = (camSpeed > 0.001f) ? (camVz / camSpeed) : 0f;

        float preX = camera.position.x + dirX * lookAhead;
        float preZ = camera.position.z + dirZ * lookAhead;

        int cx = floorDiv((int)Math.floor(preX), TILE);
        int cz = floorDiv((int)Math.floor(preZ), TILE);

        // Detect center shift (moving into a new tile)
        if (cx != lastFarCx || cz != lastFarCz) {
            lastFarCx = cx;
            lastFarCz = cz;

            // Purge stale work so the scheduler focuses on the new area immediately
            far.dropPendingOutside(cx, cz, FAR_KEEP_RADIUS);
            far.dropReadyOutside(cx, cz, FAR_KEEP_RADIUS); // add this method below

            // Trigger a short burst so far tiles catch up quickly
            farCatchupSeconds = 0.8f;
        }

        // Always evict GPU meshes outside keep radius
        evictFarTiles(cx, cz, FAR_KEEP_RADIUS);

        // Budgets: burst when we shifted, otherwise normal
        int requestBudget = (farCatchupSeconds > 0f) ? 600 : 200;

        // Schedule rings (same as before, but with a larger outer band)
        enqueueBand(cx, cz, 0, FAR_LOD0_RMIN, FAR_LOD0_RMAX, 0, requestBudget);      // LOD0
        enqueueBand(cx, cz, 1, FAR_LOD1_RMIN, FAR_LOD1_RMAX, 10_000, requestBudget); // LOD1
        enqueueBand(cx, cz, 2, FAR_LOD2_RMIN, FAR_LOD2_RMAX, 20_000, requestBudget); // LOD2
        enqueueBand(cx, cz, 3, FAR_LOD3_RMIN, FAR_LOD3_RMAX, 30_000, requestBudget); // LOD3
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

    private static int floorDiv(int a, int b) {
        int r = a / b;
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

    private static final String VS = """
        #version 330 core
        layout(location=0) in vec3 aPos;
        layout(location=1) in vec3 aNrm;
        layout(location=2) in vec2 aUv;
        layout(location=3) in float aMat;

        uniform mat4 uVP;

        out vec3 vWorldPos;
        out vec3 vNrm;
        out vec2 vUv;
        flat out float vMat;

        void main() {
            vWorldPos = aPos;
            vNrm = aNrm;
            vUv = aUv;
            vMat = aMat;
            gl_Position = uVP * vec4(aPos, 1.0);
        }
    """;

    private static final String FS = """
        #version 330 core

        in vec3 vWorldPos;
        in vec3 vNrm;
        in vec2 vUv;
        flat in float vMat;

        uniform vec3 uCameraPos;
        uniform float uSeaLevel;
        uniform float uMaxHeight;

        out vec4 FragColor;

        float hash12(vec2 p) {
            float h = dot(p, vec2(127.1, 311.7));
            return fract(sin(h) * 43758.5453123);
        }

        vec3 materialColor(float id, float hNorm, float biome) {
            float mid = abs(id);

            if (mid < 0.5) return vec3(0.0);

            if (mid < 1.5) { // GRASS
                vec3 plains   = vec3(0.18, 0.70, 0.24);
                vec3 forest   = vec3(0.10, 0.45, 0.14);
                vec3 mountain = vec3(0.55, 0.55, 0.58);
                vec3 snow     = vec3(0.92, 0.92, 0.95);

                vec3 base = mix(plains, forest, biome);

                float m = smoothstep(0.55, 0.75, hNorm);
                base = mix(base, mountain, m);

                float s = smoothstep(0.78, 0.92, hNorm);
                base = mix(base, snow, s);

                return base;
            }

            if (mid < 2.5) return vec3(0.50, 0.32, 0.16); // DIRT
            if (mid < 3.5) return vec3(0.62, 0.62, 0.66); // STONE
            if (mid < 4.5) return vec3(0.10, 0.35, 0.75); // WATER
            if (mid < 5.5) return vec3(0.78, 0.72, 0.45); // SAND

            return vec3(1.0, 0.0, 1.0);
        }

        void main() {
            vec3 n = normalize(vNrm);

            vec3 lightDir = normalize(vec3(0.6, 1.0, 0.2));
            float ndl = max(dot(n, lightDir), 0.12);

            float upness = clamp(dot(n, vec3(0,1,0)), 0.0, 1.0);
            float slopeDark = mix(0.65, 1.0, upness);

            float hNorm = clamp((vWorldPos.y - uSeaLevel) / (uMaxHeight - uSeaLevel), 0.0, 1.0);
            vec2 bCell = floor(vWorldPos.xz / 128.0);
            float biome = hash12(bCell);

            vec3 color = materialColor(vMat, hNorm, biome) * ndl * slopeDark;

            float dist = length(vWorldPos - uCameraPos);
            float camAlt = uCameraPos.y;
            float fogStart = mix(1400.0, 700.0, clamp((camAlt - 80.0) / 400.0, 0.0, 1.0));
            float fogRange = mix(3600.0, 2200.0, clamp((camAlt - 80.0) / 400.0, 0.0, 1.0));
            float fog = clamp((dist - fogStart) / fogRange, 0.0, 1.0);

            vec3 fogColor = vec3(0.55, 0.75, 0.95);
            color = mix(color, fogColor, fog);

            FragColor = vec4(color, 1.0);
        }
    """;
}