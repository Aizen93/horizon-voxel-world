package org.aouessar.core.world.layer;

import java.io.Serializable;
import java.util.List;

public final class StructureMap implements Serializable {

    public sealed interface Placement extends Serializable permits TreePlacement, CactusPlacement {}

    public record TreePlacement(int x, int z, byte type, int height) implements Placement {}
    public record CactusPlacement(int x, int z, int height) implements Placement {}

    private final LayerRect rect;
    private final List<Placement> placements;

    public StructureMap(LayerRect rect, List<Placement> placements) {
        this.rect = rect;
        this.placements = List.copyOf(placements);
    }

    public LayerRect rect() {
        return rect;
    }

    public List<Placement> placements() {
        return placements;
    }

}