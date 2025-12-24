package org.aouessar.app;

import org.aouessar.core.gen.biome.BiomeGenerator;
import org.aouessar.core.gen.biome.DefaultBiomeGenerator;
import org.aouessar.core.gen.carver.DefaultWorldCarver;
import org.aouessar.core.gen.carver.WorldCarver;
import org.aouessar.core.gen.structure.DefaultStructureBuilder;
import org.aouessar.core.gen.structure.StructureBuilder;
import org.aouessar.core.gen.surface.BiomeDecorator;
import org.aouessar.core.gen.surface.SurfaceDecorator;
import org.aouessar.core.gen.terrain.SimpleWorldGenerator;
import org.aouessar.core.gen.terrain.WorldGenerator;
import org.aouessar.core.util.EngineConstants;
import org.aouessar.core.world.chunk.Chunk;
import org.aouessar.core.world.chunk.ChunkBuilder;
import org.aouessar.core.world.chunk.DefaultChunkBuilder;
import org.aouessar.core.world.layer.*;

public class Bootstrap {

    public static void main(String[] args) {
        // Optional: keep this if you added validate() to EngineConstants
        EngineConstants.validate();

        long seed = 123456789L;

        // ----- Pipeline: derived layers -----
        WorldGenerator terrainGenerator = new SimpleWorldGenerator();
        BiomeGenerator biomeGenerator = new DefaultBiomeGenerator();
        WorldCarver carver = new DefaultWorldCarver();
        SurfaceDecorator surfaceDecorator = new BiomeDecorator();
        StructureBuilder structureBuilder = new DefaultStructureBuilder();

        Heightmap heightmap = terrainGenerator.generateHeightmap(seed);
        BiomeMap biomeMap = biomeGenerator.generateBiomes(seed, heightmap);
        CarveMask carveMask = carver.generateCarveMask(seed, heightmap);
        SurfaceRules surfaceRules = surfaceDecorator.generateSurfaceRules(heightmap, biomeMap);
        StructureMap structures = structureBuilder.placeStructures(seed, heightmap, biomeMap);

        // ----- Chunk composition (biomeMap is NOT needed anymore here) -----
        ChunkBuilder builder = new DefaultChunkBuilder(
                heightmap,
                carveMask,
                surfaceRules,
                structures
        );

        // Build a couple of chunks for a quick sanity test
        Chunk c00 = builder.buildChunk(0, 0);
        Chunk c10 = builder.buildChunk(1, 0);

        System.out.println("Seed=" + seed);
        System.out.println("Chunk(0,0) blocks=" + c00.blocks.length +
                " size=" + c00.size + " height=" + c00.height +
                " expected=" + (EngineConstants.CHUNK_SIZE * EngineConstants.WORLD_HEIGHT * EngineConstants.CHUNK_SIZE));

        System.out.println("Chunk(1,0) blocks=" + c10.blocks.length +
                " size=" + c10.size + " height=" + c10.height);

        // Example: check a few block queries
        int wx = 0;
        int wz = 0;
        int surfaceY = heightmap.heightAt(wx, wz);
        System.out.println("Surface at (0,0) y=" + surfaceY);
        System.out.println("Top block at surface = " + builder.blockAt(wx, surfaceY, wz));
        System.out.println("Water at sea level? y=" + EngineConstants.SEA_LEVEL + " -> " + builder.blockAt(wx, EngineConstants.SEA_LEVEL, wz));
    }
}