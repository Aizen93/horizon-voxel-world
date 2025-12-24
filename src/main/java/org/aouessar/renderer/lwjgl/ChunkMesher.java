package org.aouessar.renderer.lwjgl;

import org.aouessar.core.util.EngineConstants;
import org.aouessar.core.world.block.BlockId;
import org.aouessar.core.world.chunk.Chunk;
import org.aouessar.renderer.lwjgl.atlas.TextureAtlas;

import java.util.ArrayList;

import static org.aouessar.renderer.lwjgl.BlockTiles.tileForBlock;

public final class ChunkMesher {

    // Vertex layout: pos(3) uv(2)
    public record MeshBuffers(float[] vertices, int[] indices) {}

    public MeshBuffers build(Chunk chunk, TextureAtlas atlas) {
        int cs = EngineConstants.CHUNK_SIZE;
        int wh = EngineConstants.WORLD_HEIGHT;

        ArrayList<Float> v = new ArrayList<>(cs * cs * 200);
        ArrayList<Integer> i = new ArrayList<>(cs * cs * 300);
        int baseIndex = 0;

        for (int y = 0; y < wh; y++) {
            for (int z = 0; z < cs; z++) {
                for (int x = 0; x < cs; x++) {
                    short id = chunk.blocks[((y * cs) + z) * cs + x];
                    if (id == BlockId.AIR) continue;

                    // for now, treat water as solid (later: separate pass with blending)
                    if (isFaceVisible(chunk, x, y, z,  0,  1,  0)) baseIndex = addFace(v, i, baseIndex, atlas, id, x, y, z, Face.UP);
                    if (isFaceVisible(chunk, x, y, z,  0, -1,  0)) baseIndex = addFace(v, i, baseIndex, atlas, id, x, y, z, Face.DOWN);
                    if (isFaceVisible(chunk, x, y, z,  1,  0,  0)) baseIndex = addFace(v, i, baseIndex, atlas, id, x, y, z, Face.EAST);
                    if (isFaceVisible(chunk, x, y, z, -1,  0,  0)) baseIndex = addFace(v, i, baseIndex, atlas, id, x, y, z, Face.WEST);
                    if (isFaceVisible(chunk, x, y, z,  0,  0,  1)) baseIndex = addFace(v, i, baseIndex, atlas, id, x, y, z, Face.SOUTH);
                    if (isFaceVisible(chunk, x, y, z,  0,  0, -1)) baseIndex = addFace(v, i, baseIndex, atlas, id, x, y, z, Face.NORTH);
                }
            }
        }

        float[] vertices = new float[v.size()];
        for (int k = 0; k < v.size(); k++) vertices[k] = v.get(k);

        int[] indices = new int[i.size()];
        for (int k = 0; k < i.size(); k++) indices[k] = i.get(k);

        return new MeshBuffers(vertices, indices);
    }

    private boolean isFaceVisible(Chunk c, int x, int y, int z, int dx, int dy, int dz) {
        int cs = EngineConstants.CHUNK_SIZE;
        int wh = EngineConstants.WORLD_HEIGHT;

        int nx = x + dx, ny = y + dy, nz = z + dz;
        if (nx < 0 || nz < 0 || nx >= cs || nz >= cs || ny < 0 || ny >= wh) {
            return true; // edge -> visible (neighbor chunk handled later)
        }
        short nid = c.blocks[((ny * cs) + nz) * cs + nx];
        return nid == BlockId.AIR; // later: handle water/transparent blocks
    }

    private enum Face { UP, DOWN, EAST, WEST, SOUTH, NORTH }

    private int addFace(ArrayList<Float> v, ArrayList<Integer> i, int base, TextureAtlas atlas, short blockId,
                        int x, int y, int z, Face f) {

        TextureAtlas.UV uv = atlas.uvs().getOrDefault(tileForBlock(blockId), atlas.uvs().values().iterator().next());
        float u0 = uv.u0(), v0 = uv.v0(), u1 = uv.u1(), v1 = uv.v1();

        // 4 verts per face, 6 indices
        // Positions are chunk-local; renderer will translate by chunk world origin.
        // For simplicity, each cube is [x..x+1], [y..y+1], [z..z+1]
        float[][] p = switch (f) {
            case UP -> new float[][]{
                    {x, y+1, z}, {x+1, y+1, z}, {x+1, y+1, z+1}, {x, y+1, z+1}
            };
            case DOWN -> new float[][]{
                    {x, y, z+1}, {x+1, y, z+1}, {x+1, y, z}, {x, y, z}
            };
            case EAST -> new float[][]{
                    {x+1, y, z}, {x+1, y, z+1}, {x+1, y+1, z+1}, {x+1, y+1, z}
            };
            case WEST -> new float[][]{
                    {x, y, z+1}, {x, y, z}, {x, y+1, z}, {x, y+1, z+1}
            };
            case SOUTH -> new float[][]{
                    {x+1, y, z+1}, {x, y, z+1}, {x, y+1, z+1}, {x+1, y+1, z+1}
            };
            case NORTH -> new float[][]{
                    {x, y, z}, {x+1, y, z}, {x+1, y+1, z}, {x, y+1, z}
            };
        };

        // UVs (no rotation for now)
        float[][] t = new float[][]{
                {u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}
        };

        for (int k = 0; k < 4; k++) {
            v.add(p[k][0]); v.add(p[k][1]); v.add(p[k][2]);
            v.add(t[k][0]); v.add(t[k][1]);
        }

        i.add(base); i.add(base+1); i.add(base+2);
        i.add(base); i.add(base+2); i.add(base+3);

        return base + 4;
    }
}