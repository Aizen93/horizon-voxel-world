# Minecraft-Like Voxel Engine — Clean Architecture Bootstrap Prompt

I want to start a new Java 21 project using LWJGL to build a Minecraft-like voxel engine, but this time with a **clean, production-grade architecture**.

The design must strictly separate responsibilities and avoid mutating a single World object in place.

The engine must be built as a data pipeline that derives independent layers (heightmap, biome map, carve mask, surface rules, structure placements). Each generation phase must produce **derived data** and then composes them into blocks only at chunk build time.

---

## Core rules:

- Java 21
- LWJGL renderer (OpenGL)
- Texture atlas rendering (Minecraft-style)
- Deterministic world generation
- Far-field LOD friendly
- UE5-friendly (world data must be serializable and streamable)
- No OpenGL or LWJGL code outside the renderer layer

---

```java
Heightmap heightmap = terrainGenerator.generateHeightmap(seed);
BiomeMap biomeMap = biomeGenerator.generateBiomes(seed, heightmap);
CarveMask carveMask = carver.generateCarveMask(seed, heightmap);
SurfaceRules surfaceRules = surfaceDecorator.generateSurfaceRules(heightmap, biomeMap);
StructureMap structures = structureBuilder.placeStructures(seed, heightmap, biomeMap);

ChunkBuilder builder = new ChunkBuilder(
    heightmap,
    biomeMap,
    carveMask,
    surfaceRules,
    structures
);

Chunk chunk = builder.buildChunk(cx, cz);
```

## Required architecture:
1. ### Terrain
    ```java
    interface WorldGenerator {
        Heightmap generateHeightmap(long seed);
    }
    ```

    Implementation: 
    - SimpleWorldGenerator
      - Uses FastNoiseLite
      - Responsible only for : base terrain shape, continents, oceans, Large-scale elevation ...etc

2. ### Carvers
    ```java
    interface WorldCarver {
        CarveMask generateCarveMask(long seed, Heightmap heightmap);
    }
    ```

    Implementation: 
    - DefaultWorldCarver
      - Responsibilities: Rivers, caves, ravines ...etc
      - Rivers must always reach oceans
      - Outputs carve/density masks, not blocks

3. Surface
    ```java
    interface SurfaceDecorator {
        SurfaceRules generateSurfaceRules(Heightmap heightmap, BiomeMap biomeMap);
    }
    ```

    Implementation: 
    - BiomeDecorator
      - Responsibilities : Grass, sand, snow, podzol, etc.
      - Biome-dependent surface logic
      - No structure placement

4. Structures
    ```java
    interface StructureBuilder {
        StructureMap placeStructures(long seed, Heightmap heightmap, BiomeMap biomeMap);
    }
    ```

    Implementation: 
    - DefaultStructureBuilder
      - Responsibilities : Oak trees, jungle trees, savanna trees, swamp trees, cactus, bushes, flowers ..etc
      - Produces structure placement data only

5. Chunk composition
    ```java
    final class ChunkBuilder {
        short blockAt(int wx, int wy, int wz);
    }
    ```
   - Responsibilities :   
     - Compose: Heightmap, Carve mask, Surface rules, Structure placements

6. Rendering
    ```java
    interface Renderer {
        void render(Camera camera);
    }
    ```

    Implementation: 
    - LwjglRenderer
      - Responsibilities : Near field rendering, Far field LOD rendering, Texture atlas sampling

## Important constraints:
- No OpenGL or LWJGL code outside renderer
- No block mutation during generation phases
- All generation stages must be cacheable and inspectable
- The system must allow replacing LWJGL with UE5 later


Please generate:
- Package structure
- Core interfaces
- Data classes (Heightmap, BiomeMap, etc.)
- Minimal bootstrap example

Focus on correctness, clarity, and long-term scalability.