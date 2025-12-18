package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.mesh.Mesher;
import org.aouessar.core.world.*;

import java.util.*;
import java.util.concurrent.*;

public final class StreamingWorld {
    private final WorldGenerator generator;
    private final Mesher mesher;

    private final ExecutorService pool;
    private final ConcurrentHashMap<ChunkPos, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkPos, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ChunkMeshReady> readyMeshes = new ConcurrentLinkedQueue<>();

    public StreamingWorld(WorldGenerator generator, Mesher mesher, int workerThreads) {
        this.generator = generator;
        this.mesher = mesher;
        this.pool = Executors.newFixedThreadPool(Math.max(1, workerThreads));
    }

    public void requestChunk(ChunkPos pos) {
        if (chunks.containsKey(pos)) return;

        inFlight.computeIfAbsent(pos, p -> CompletableFuture.runAsync(() -> {
            Chunk c = new Chunk(p);
            ChunkBuilder.fillChunk(c, generator);
            chunks.put(p, c);

            MeshData mesh = mesher.buildChunkMesh(c, generator);
            readyMeshes.add(new ChunkMeshReady(p, mesh));
        }, pool).whenComplete((ok, err) -> inFlight.remove(p)));
    }

    public Chunk getChunkOrNull(ChunkPos pos) {
        return chunks.get(pos);
    }

    public List<ChunkMeshReady> pollReadyMeshes(int max) {
        ArrayList<ChunkMeshReady> out = new ArrayList<>(Math.min(max, 256));
        for (int i = 0; i < max; i++) {
            ChunkMeshReady r = readyMeshes.poll();
            if (r == null) break;
            out.add(r);
        }
        return out;
    }

    public void shutdown() {
        pool.shutdownNow();
    }

    public void evictChunksOutside(int centerCx, int centerCz, int maxDist2) {
        chunks.forEach((pos, chunk) -> {
            int dx = pos.cx() - centerCx;
            int dz = pos.cz() - centerCz;
            if (dx*dx + dz*dz > maxDist2) {
                // only evict if not currently building
                if (!inFlight.containsKey(pos)) {
                    chunks.remove(pos, chunk);
                }
            }
        });
    }

}
