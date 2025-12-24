package org.aouessar.app;

import org.aouessar.core.Engine;
import org.aouessar.renderer.api.RenderWorldView;
import org.aouessar.renderer.api.Renderer;
import org.aouessar.renderer.lwjgl.LwjglRenderer;

public class Main {

    public static void main(String[] args) {
        System.err.println("[BOOT] Main started");

        // 2) Print all uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("[FATAL] Uncaught exception in thread: " + t.getName());
            e.printStackTrace(System.err);
        });

        try {
            long seed = 123456789L;

            Engine engine = new Engine(seed);

            RenderWorldView view = new RenderWorldView() {
                @Override public org.aouessar.core.world.chunk.Chunk chunkAt(int cx, int cz) {
                    return engine.getChunk(cx, cz);
                }
                @Override public org.aouessar.core.world.layer.Heightmap heightmap() {
                    return engine.heightmap();
                }
            };

            Renderer renderer = new LwjglRenderer(view);
            renderer.run();
        } catch (Throwable t) {
            System.err.println("[FATAL] Crash during startup:");
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
