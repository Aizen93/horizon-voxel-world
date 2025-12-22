package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.world.TilePos;
import org.aouessar.core.world.WorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.aouessar.utils.Utils.*;

public final class FarField {

    private final WorldGenerator generator;
    private final int seaLevel;

    private final ExecutorService pool;

    private final ConcurrentHashMap<TilePos, Boolean> inFlight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TilePos, Boolean> pendingSet = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<Job> pendingQ = new PriorityBlockingQueue<>();
    private final ConcurrentLinkedQueue<FarTileReady> ready = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean running = new AtomicBoolean(true);

    public FarField(WorldGenerator generator, int seaLevel, int workers) {
        this.generator = generator;
        this.seaLevel = seaLevel;

        int w = Math.max(1, workers);
        this.pool = Executors.newFixedThreadPool(w);

        for (int i = 0; i < w; i++) {
            pool.submit(this::workerLoop);
        }
    }

    public void enqueue(TilePos pos, int priority) {
        if (!running.get()) return;

        if (ready.size() >= FAR_MAX_READY) return;
        if (pendingSet.size() >= FAR_MAX_PENDING) return;

        if (inFlight.containsKey(pos)) return;
        if (pendingSet.putIfAbsent(pos, Boolean.TRUE) != null) return;

        pendingQ.offer(new Job(pos, priority));
    }

    public boolean isInFlight(TilePos pos) {
        return inFlight.containsKey(pos);
    }

    public boolean isPending(TilePos pos) {
        return pendingSet.containsKey(pos);
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

    public void dropReadyOutside(int cx, int cz, int keepRadius) {
        int r2 = keepRadius * keepRadius;
        ready.removeIf(r -> {
            int dx = r.pos().tx() - cx;
            int dz = r.pos().tz() - cz;
            return dx * dx + dz * dz > r2;
        });
    }

    public void dropPendingOutside(int cx, int cz, int keepRadius) {
        int r2 = keepRadius * keepRadius;
        pendingSet.forEach((pos, v) -> {
            int dx = pos.tx() - cx;
            int dz = pos.tz() - cz;
            if (dx * dx + dz * dz > r2) {
                pendingSet.remove(pos);
            }
        });
    }

    private void workerLoop() {
        while (running.get()) {
            try {
                Job job = pendingQ.take();

                // stale job -> skip
                if (!pendingSet.containsKey(job.pos)) continue;

                if (ready.size() >= FAR_MAX_READY) {
                    Thread.sleep(2);
                    continue;
                }

                if (inFlight.size() >= FAR_MAX_IN_FLIGHT) {
                    pendingQ.offer(job);
                    Thread.sleep(1);
                    continue;
                }

                // claim job
                pendingSet.remove(job.pos);
                if (inFlight.putIfAbsent(job.pos, Boolean.TRUE) != null) continue;

                // build
                MeshData mesh = FarTileBuilder.build(generator, seaLevel, job.pos);
                ready.add(new FarTileReady(job.pos, mesh));

                inFlight.remove(job.pos);

            } catch (InterruptedException ignored) {
                break;
            } catch (Throwable t) {
                // prevent deadlocks if something throws
                // (best effort) remove any in-flight marker if present
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