package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.world.TilePos;
import org.aouessar.core.world.WorldGenerator;

import java.util.*;
import java.util.concurrent.*;

import static org.aouessar.utils.Utils.TILE_SIZE;

public final class FarField {
    private final WorldGenerator generator;
    private final int seaLevel;

    private final ExecutorService pool;
    private final ConcurrentHashMap<TilePos, CompletableFuture<Void>> inFlight = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<FarTileReady> ready = new ConcurrentLinkedQueue<>();

    private static final int MAX_IN_FLIGHT = 32;
    private static final int MAX_READY_QUEUE = 64;


    // You can cache MeshData here if you want CPU-side caching too
    public FarField(WorldGenerator generator, int seaLevel, int workers) {
        this.generator = generator;
        this.seaLevel = seaLevel;
        this.pool = Executors.newFixedThreadPool(Math.max(1, workers));
    }

    public void request(TilePos pos) {
        if (inFlight.size() >= MAX_IN_FLIGHT) return;
        if (ready.size() >= MAX_READY_QUEUE) return;

        inFlight.computeIfAbsent(pos, p ->
                CompletableFuture.runAsync(() -> {
                    MeshData mesh = FarTileBuilder.build(generator, seaLevel, p);
                    ready.add(new FarTileReady(p, mesh));
                }, pool).whenComplete((ok, err) -> inFlight.remove(p))
        );
    }


    public List<FarTileReady> pollReady(int max) {
        ArrayList<FarTileReady> out = new ArrayList<>(Math.min(max, TILE_SIZE));
        for (int i = 0; i < max; i++) {
            FarTileReady r = ready.poll();
            if (r == null) break;
            out.add(r);
        }
        return out;
    }

    public void shutdown() {
        pool.shutdownNow();
    }

    public boolean isInFlight(TilePos pos) {
        return inFlight.containsKey(pos);
    }

    public void dropReadyOutside(int cx, int cz, int keepRadius) {
        int r2 = keepRadius * keepRadius;
        ready.removeIf(r -> {
            int dx = r.pos().tx() - cx;
            int dz = r.pos().tz() - cz;
            return dx*dx + dz*dz > r2;
        });
    }


    public void evictOutside(int centerTx, int centerTz, int maxDist2) {
        // For now we only evict GPU meshes in renderer; keep this for future CPU cache.
    }
}
