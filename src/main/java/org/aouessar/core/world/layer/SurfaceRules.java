package org.aouessar.core.world.layer;

import org.aouessar.core.world.block.BlockId;

import java.io.Serializable;

public interface SurfaceRules extends Serializable {
    /** Topmost block for column (x,z). */
    short topBlockAt(int x, int z);

    /** How many blocks below the top are "filler" (e.g. dirt or sand). */
    int fillerDepthAt(int x, int z);

    /** Filler block used under the top block (e.g. dirt, sand). */
    short fillerBlockAt(int x, int z);

}