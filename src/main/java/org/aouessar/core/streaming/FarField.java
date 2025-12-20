package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.world.TilePos;
import org.aouessar.core.world.WorldGenerator;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FarField {

    private final WorldGenerator generator;
    private final int seaLevel;

    private final ExecutorService pool;

    // Tiles currently being built
    private final ConcurrentHashMap<TilePos, Boolean> inFlight = new ConcurrentHashMap<>();

    // Tiles waiting to be built (scheduler state)
    private final ConcurrentHashMap<TilePos, Boolean> pendingSet = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<Job> pendingQ = new PriorityBlockingQueue<>();

    // Finished meshes ready for upload
    private final ConcurrentLinkedQueue<FarTileReady> ready = new ConcurrentLinkedQueue<>();

    // Hard caps (keep these)
    private static final int MAX_IN_FLIGHT = 128;
    private static final int MAX_PENDING   = 8192;
    private static final int MAX_READY     = 512;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public FarField(WorldGenerator generator, int seaLevel, int workers) {
        this.generator = generator;
        this.seaLevel = seaLevel;

        int w = Math.max(1, workers);
        this.pool = Executors.newFixedThreadPool(w);

        // Start worker loops
        for (int i = 0; i < w; i++) {
            pool.submit(this::workerLoop);
        }
    }

    // Enqueue a tile build request with a priority (lower = earlier)
    public void enqueue(TilePos pos, int priority) {
        if (!running.get()) return;

        if (ready.size() >= MAX_READY) return;
        if (pendingSet.size() >= MAX_PENDING) return;

        // Don't enqueue if already building or already pending
        if (inFlight.containsKey(pos)) return;
        if (pendingSet.putIfAbsent(pos, Boolean.TRUE) != null) return;

        pendingQ.offer(new Job(pos, priority));
    }

    public void dropReadyOutside(int cx, int cz, int keepRadius) {
        int r2 = keepRadius * keepRadius;
        ready.removeIf(r -> {
            int dx = r.pos().tx() - cx;
            int dz = r.pos().tz() - cz;
            return dx*dx + dz*dz > r2;
        });
    }

    public boolean isInFlight(TilePos pos) {
        return inFlight.containsKey(pos);
    }

    public List<FarTileReady> pollReady(int max) {
        ArrayList<FarTileReady> out = new ArrayList<>(Math.min(max, 256));
        for (int i = 0; i < max; i++) {
            FarTileReady r = ready.poll();
            if (r == null) break;
            out.add(r);
        }
        return out;
    }

    // Optional: drop old pending requests when player moved far
    public void dropPendingOutside(int cx, int cz, int keepRadius) {
        int r2 = keepRadius * keepRadius;

        // Remove from pendingSet; we cannot efficiently remove from PriorityBlockingQueue,
        // so we mark by removing from pendingSet; worker will skip stale jobs.
        pendingSet.forEach((pos, v) -> {
            int dx = pos.tx() - cx;
            int dz = pos.tz() - cz;
            if (dx*dx + dz*dz > r2) {
                pendingSet.remove(pos);
            }
        });
    }

    private void workerLoop() {
        while (running.get()) {
            try {
                Job job = pendingQ.take();

                // If job was dropped (stale), skip
                if (!pendingSet.containsKey(job.pos)) continue;

                // respect caps
                if (ready.size() >= MAX_READY) {
                    // small backoff
                    Thread.sleep(2);
                    continue;
                }

                if (inFlight.size() >= MAX_IN_FLIGHT) {
                    // requeue with same priority (try later)
                    pendingQ.offer(job);
                    Thread.sleep(1);
                    continue;
                }

                // Claim this job
                pendingSet.remove(job.pos);
                if (inFlight.putIfAbsent(job.pos, Boolean.TRUE) != null) continue;

                // Build
                MeshData mesh = FarTileBuilder.build(generator, seaLevel, job.pos);
                ready.add(new FarTileReady(job.pos, mesh));

                inFlight.remove(job.pos);

            } catch (InterruptedException ignored) {
                // shutdown
                break;
            } catch (Throwable t) {
                // Make sure we don't deadlock a tile forever
                // (best effort: ignore)
            }
        }
    }

    public void shutdown() {
        running.set(false);
        pool.shutdownNow();
    }

    private record Job(TilePos pos, int priority) implements Comparable<Job> {
        @Override public int compareTo(Job o) {
            return Integer.compare(this.priority, o.priority);
        }
    }
}
