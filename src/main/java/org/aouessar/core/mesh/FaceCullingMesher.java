package org.aouessar.core.mesh;

import org.aouessar.core.world.BlockId;
import org.aouessar.core.world.Chunk;
import org.aouessar.core.world.WorldGenerator;

import java.util.ArrayList;

public final class FaceCullingMesher implements Mesher {

    @Override
    public MeshData buildChunkMesh(Chunk chunk, WorldGenerator gen) {
        ArrayList<Float> pos = new ArrayList<>();
        ArrayList<Float> nrm = new ArrayList<>();
        ArrayList<Float> uv  = new ArrayList<>();
        ArrayList<Integer> idx = new ArrayList<>();
        ArrayList<Short> mat = new ArrayList<>();

        int baseX = chunk.pos.cx() * Chunk.SIZE;
        int baseY = chunk.pos.cy() * Chunk.SIZE;
        int baseZ = chunk.pos.cz() * Chunk.SIZE;

        int indexBase = 0;

        for (int y = 0; y < Chunk.SIZE; y++) {
            int wy = baseY + y;
            for (int z = 0; z < Chunk.SIZE; z++) {
                int wz = baseZ + z;
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int wx = baseX + x;

                    short id = chunk.get(x, y, z);
                    if (id == BlockId.AIR) continue;

                    // For each face: if neighbor is AIR => emit quad
                    if (isAir(neighbor(chunk, gen, wx, wy, wz, -1, 0, 0))) indexBase = emitFace(pos,nrm,uv,idx,mat,indexBase, wx,wy,wz, Face.NX, id);
                    if (isAir(neighbor(chunk, gen, wx, wy, wz,  1, 0, 0))) indexBase = emitFace(pos,nrm,uv,idx,mat,indexBase, wx,wy,wz, Face.PX, id);
                    if (isAir(neighbor(chunk, gen, wx, wy, wz, 0, -1, 0))) indexBase = emitFace(pos,nrm,uv,idx,mat,indexBase, wx,wy,wz, Face.NY, id);
                    if (isAir(neighbor(chunk, gen, wx, wy, wz, 0,  1, 0))) indexBase = emitFace(pos,nrm,uv,idx,mat,indexBase, wx,wy,wz, Face.PY, id);
                    if (isAir(neighbor(chunk, gen, wx, wy, wz, 0, 0, -1))) indexBase = emitFace(pos,nrm,uv,idx,mat,indexBase, wx,wy,wz, Face.NZ, id);
                    if (isAir(neighbor(chunk, gen, wx, wy, wz, 0, 0,  1))) indexBase = emitFace(pos,nrm,uv,idx,mat,indexBase, wx,wy,wz, Face.PZ, id);
                }
            }
        }

        return new MeshData(
                toFloatArray(pos),
                toFloatArray(nrm),
                toFloatArray(uv),
                toIntArray(idx),
                toShortArray(mat)
        );
    }

    private static boolean isAir(short id) {
        return id == BlockId.AIR;
    }

    private static short neighbor(Chunk chunk, WorldGenerator gen, int wx, int wy, int wz, int dx, int dy, int dz) {
        int nx = wx + dx, ny = wy + dy, nz = wz + dz;

        // If neighbor is inside same chunk, read from chunk array (fast)
        int lx = nx - chunk.pos.cx() * Chunk.SIZE;
        int ly = ny - chunk.pos.cy() * Chunk.SIZE;
        int lz = nz - chunk.pos.cz() * Chunk.SIZE;
        if (lx >= 0 && lx < Chunk.SIZE && ly >= 0 && ly < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
            return chunk.get(lx, ly, lz);
        }
        // else query generator (deterministic)
        return gen.blockAt(nx, ny, nz);
    }

    private enum Face {
        NX(-1,0,0, -1,0,0),
        PX( 1,0,0,  1,0,0),
        NY(0,-1,0,  0,-1,0),
        PY(0, 1,0,  0, 1,0),
        NZ(0,0,-1,  0,0,-1),
        PZ(0,0, 1,  0,0, 1);

        final int dx,dy,dz;
        final float nx,ny,nz;
        Face(int dx,int dy,int dz, float nx,float ny,float nz){
            this.dx=dx; this.dy=dy; this.dz=dz;
            this.nx=nx; this.ny=ny; this.nz=nz;
        }
    }

    private static int emitFace(
            ArrayList<Float> pos, ArrayList<Float> nrm, ArrayList<Float> uv,
            ArrayList<Integer> idx, ArrayList<Short> mat,
            int indexBase,
            int wx, int wy, int wz,
            Face f,
            short materialId
    ) {
        // Block is [wx..wx+1], etc.
        float x0 = wx, x1 = wx + 1f;
        float y0 = wy, y1 = wy + 1f;
        float z0 = wz, z1 = wz + 1f;

        // 4 vertices for the face (order chosen for outward normal + CCW)
        float[] v;
        switch (f) {
            case NX -> v = new float[]{ x0,y0,z0,  x0,y0,z1,  x0,y1,z1,  x0,y1,z0 };
            case PX -> v = new float[]{ x1,y0,z0,  x1,y1,z0,  x1,y1,z1,  x1,y0,z1 };
            case NY -> v = new float[]{ x0,y0,z0,  x1,y0,z0,  x1,y0,z1,  x0,y0,z1 };
            case PY -> v = new float[]{ x0,y1,z1,  x1,y1,z1,  x1,y1,z0,  x0,y1,z0 };
            case NZ -> v = new float[]{ x0,y0,z0,  x0,y1,z0,  x1,y1,z0,  x1,y0,z0 };
            case PZ -> v = new float[]{ x1,y0,z1,  x1,y1,z1,  x0,y1,z1,  x0,y0,z1 };
            default -> throw new IllegalStateException();
        }

        float[] uvs = new float[]{ 0,0, 1,0, 1,1, 0,1 };

        // push vertices
        for (int i = 0; i < 4; i++) {
            pos.add(v[i*3]); pos.add(v[i*3+1]); pos.add(v[i*3+2]);
            nrm.add(f.nx); nrm.add(f.ny); nrm.add(f.nz);
            uv.add(uvs[i*2]); uv.add(uvs[i*2+1]);
            mat.add(materialId);
        }

        // 2 triangles
        idx.add(indexBase + 0); idx.add(indexBase + 1); idx.add(indexBase + 2);
        idx.add(indexBase + 2); idx.add(indexBase + 3); idx.add(indexBase + 0);

        return indexBase + 4;
    }

    private static float[] toFloatArray(ArrayList<Float> list) {
        float[] a = new float[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    private static int[] toIntArray(ArrayList<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    private static short[] toShortArray(ArrayList<Short> list) {
        short[] a = new short[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }
}
