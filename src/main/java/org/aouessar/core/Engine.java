package org.aouessar.core;

import org.aouessar.core.gen.biome.BiomeGenerator;
import org.aouessar.core.gen.biome.DefaultBiomeGenerator;
import org.aouessar.core.gen.carver.DefaultWorldCarver;
import org.aouessar.core.gen.carver.WorldCarver;
import org.aouessar.core.gen.surface.BiomeDecorator;
import org.aouessar.core.gen.surface.SurfaceDecorator;
import org.aouessar.core.gen.structure.DefaultStructureBuilder;
import org.aouessar.core.gen.structure.StructureBuilder;
import org.aouessar.core.gen.terrain.SimpleWorldGenerator;
import org.aouessar.core.gen.terrain.WorldGenerator;
import org.aouessar.core.world.chunk.Chunk;
import org.aouessar.core.world.chunk.ChunkBuilder;
import org.aouessar.core.world.chunk.DefaultChunkBuilder;
import org.aouessar.core.world.layer.*;

public final class Engine {

    private final long seed;

    private final Heightmap heightmap;
    private final BiomeMap biomeMap;
    private final CarveMask carveMask;
    private final SurfaceRules surfaceRules;
    private final StructureMap structureMap;

    private final ChunkBuilder chunkBuilder;

    public Engine(long seed) {
        this.seed = seed;

        WorldGenerator terrain = new SimpleWorldGenerator();
        BiomeGenerator biomes = new DefaultBiomeGenerator();
        WorldCarver carver = new DefaultWorldCarver();
        SurfaceDecorator surface = new BiomeDecorator();
        StructureBuilder structures = new DefaultStructureBuilder();

        this.heightmap = terrain.generateHeightmap(seed);
        this.biomeMap = biomes.generateBiomes(seed, heightmap);
        this.carveMask = carver.generateCarveMask(seed, heightmap);
        this.surfaceRules = surface.generateSurfaceRules(heightmap, biomeMap);
        this.structureMap = structures.placeStructures(seed, heightmap, biomeMap);

        this.chunkBuilder = new DefaultChunkBuilder(heightmap, carveMask, surfaceRules, structureMap);
    }

    public long seed() {
        return seed;
    }

    public Heightmap heightmap() {
        return heightmap;
    }

    public Chunk getChunk(int cx, int cz) {
        return chunkBuilder.buildChunk(cx, cz);
    }
}