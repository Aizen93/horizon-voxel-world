package org.aouessar.core.streaming;

import org.aouessar.core.mesh.MeshData;
import org.aouessar.core.world.TilePos;

public record FarTileReady(TilePos pos, MeshData mesh) {}
