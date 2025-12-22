package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.mesh.Mesher;
import org.aouessar.core.world.Chunk;
import org.aouessar.core.world.ChunkBuilder;
import org.aouessar.core.world.ChunkPos;
import org.aouessar.core.world.WorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Near-field streaming manager.
 *
 * - schedules chunk generation + meshing on a worker pool
 * - stores completed chunks
 * - exposes a queue of ready meshes to be uploaded by the renderer
 */
public final class StreamingWorld {

    private final WorldGenerator generator;
    private final Mesher mesher;

    private final ExecutorService pool;

    /** Tracks currently building chunks to prevent duplicate work. */
    private final ConcurrentHashMap<ChunkPos, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();

    /** Fully built chunks (voxel data ready). */
    private final ConcurrentHashMap<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();

    /** Completed meshes ready for GPU upload. */
    private final ConcurrentLinkedQueue<ChunkMeshReady> readyMeshes = new ConcurrentLinkedQueue<>();

    public StreamingWorld(WorldGenerator generator, Mesher mesher, int workerThreads) {
        this.generator = Objects.requireNonNull(generator, "generator");
        this.mesher = Objects.requireNonNull(mesher, "mesher");

        int threads = Math.max(1, workerThreads);
        this.pool = Executors.newFixedThreadPool(threads);
    }

    /**
     * Ensure the given chunk exists; if not, generate + mesh it asynchronously.
     * Idempotent and safe to call every frame.
     */
    public void requestChunk(ChunkPos pos) {
        Objects.requireNonNull(pos, "pos");

        if (chunks.containsKey(pos)) return;

        inFlight.computeIfAbsent(pos, p ->
                CompletableFuture.runAsync(() -> buildChunkAndMesh(p), pool)
                        .whenComplete((ok, err) -> inFlight.remove(p))
        );
    }

    private void buildChunkAndMesh(ChunkPos pos) {
        if (chunks.containsKey(pos)) return;

        Chunk chunk = new Chunk(pos);

        ChunkBuilder.fillChunk(chunk, generator);

        chunks.put(pos, chunk);

        MeshData mesh = mesher.buildChunkMesh(chunk, generator);
        readyMeshes.add(new ChunkMeshReady(pos, mesh));
    }

    public Chunk getChunkOrNull(ChunkPos pos) {
        return chunks.get(pos);
    }

    /**
     * Poll up to {@code max} meshes that are ready for GPU upload.
     */
    public List<ChunkMeshReady> pollReadyMeshes(int max) {
        if (max <= 0) return List.of();

        ArrayList<ChunkMeshReady> out = new ArrayList<>(Math.min(max, 256));
        for (int i = 0; i < max; i++) {
            ChunkMeshReady r = readyMeshes.poll();
            if (r == null) break;
            out.add(r);
        }
        return out;
    }

    /**
     * Evict chunks outside a distance-squared from (centerCx, centerCz).
     * Will not evict chunks currently in-flight.
     */
    public void evictChunksOutside(int centerCx, int centerCz, int maxDist2) {
        chunks.forEach((pos, chunk) -> {
            int dx = pos.cx() - centerCx;
            int dz = pos.cz() - centerCz;
            if (dx * dx + dz * dz > maxDist2) {
                if (!inFlight.containsKey(pos)) {
                    chunks.remove(pos, chunk);
                }
            }
        });
    }

    /**
     * Stop all workers and clear queues/maps.
     */
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        } finally {
            inFlight.clear();
            readyMeshes.clear();
            chunks.clear();
        }
    }
}