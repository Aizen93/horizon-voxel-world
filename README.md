# PROJECT CONTEXT — Minecraft-like Java Voxel Engine (STABLE BASELINE)

## Engine status (IMPORTANT: this is a known good state)
- Java 21
- LWJGL / OpenGL renderer
- Chunk-based voxel world (CHUNK_SIZE = 32)
- Far-field heightmesh LOD system (Distant Horizons–style)
- TILE_SIZE = 256
- WORLD_MAX_Y = 260
- SEA_LEVEL = 62

### Stability
- ✅ No holes
- ✅ No cracks
- ✅ No missing faces
- ✅ Near + far field both render correctly
- ✅ Shaders fully working (materials, fog, biome tinting)
- ✅ FPS stable (~3.5K moving, ~4K idle)

### Architecture (locked)
- `WorldGenerator` is the **single generator interface**
    - `short blockAt(int wx, int wy, int wz)`
    - `int heightAt(int wx, int wz)`
- `HeightmapWorldGenerator` has been removed
- Far field, chunk streaming, and meshing rely on `heightAt`
- Chunk filling uses `blockAt` only
- GreedyMesher is stable and samples generator for out-of-chunk queries
- `ChunkBuilder.fillChunk()` delegates fully to `gen.blockAt(...)`
- Constants centralized in `Utils.java`

### Rendering
- Greedy meshing
- Material ID passed per vertex
- Shader computes color from:
    - material id
    - world height
    - biome hash
- Far field uses height sampling only (no blockAt calls)

---

## NEXT PHASE: TERRAIN GENERATION (GOAL)

We want to implement **terrain generation logic inside `SimpleWorldGenerator`**, split cleanly as:

- `heightAt(x,z)` → terrain geometry
- `blockAt(x,y,z)` → materials / biomes

### Target terrain features
- 🌍 Big oceans (continent-scale)
- 🏔️ Really big mountains (near WORLD_MAX_Y)
- 🌄 Hills
- 🌊 Rivers
- 🏞️ Lakes
- 🌲 Forests
- 🏜️ Deserts

### Constraints
- Deterministic
- Far-field friendly (heightAt must be fast)
- No behavior regressions (engine stability is priority)
- Performance-safe (no allocations in hot paths)

### Available blocks
- AIR = 0
- GRASS = 1
- DIRT = 2
- STONE = 3
- WATER = 4
- SAND = 5

---

## REQUEST
Design and implement a **production-grade terrain generator**:
- Continents + oceans
- Mountains + hills
- Rivers + lakes
- Forest/desert biomes
- Clean separation between geometry and materials
- Constants placed in `Utils.java`

Return **full updated files only**, starting with:
- `SimpleWorldGenerator.java`

(No refactors outside terrain logic unless explicitly requested.)
