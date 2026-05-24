package com.voltcraft.electric.wire;

import com.voltcraft.electric.Phase;
import net.minecraft.core.BlockPos;

public record WireEndpoint(BlockPos pos, int endpointIndex, Phase phase) {

    public WireEndpoint(BlockPos pos, int endpointIndex) {
        this(pos, endpointIndex, phaseFromIndex(endpointIndex));
    }

    public net.minecraft.world.phys.Vec3 getWorldPosition() {
        return net.minecraft.world.phys.Vec3.atCenterOf(pos);
    }

    public static Phase phaseFromIndex(int endpointIndex) {
        return switch (Math.floorMod(endpointIndex, 3)) {
            case 0 -> Phase.LIVE;
            case 1 -> Phase.NEUTRAL;
            default -> Phase.EARTH;
        };
    }

    @Override
    public String toString() {
        return "WireEndpoint{" + pos.toShortString() + ", index=" + endpointIndex + ", phase=" + phase.shortLabel() + "}";
    }
}
