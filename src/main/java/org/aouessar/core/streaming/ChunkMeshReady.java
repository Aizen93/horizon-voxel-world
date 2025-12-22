package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.world.ChunkPos;

public record ChunkMeshReady(ChunkPos pos, MeshData mesh) {}