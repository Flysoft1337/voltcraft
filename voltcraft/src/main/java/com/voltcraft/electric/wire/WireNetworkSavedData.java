package com.voltcraft.electric.wire;

import com.voltcraft.electric.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class WireNetworkSavedData extends SavedData {

    public static final String ID = "voltcraft_wire_networks";
    public static final Factory<WireNetworkSavedData> FACTORY = new Factory<>(
            WireNetworkSavedData::new,
            WireNetworkSavedData::load
    );

    private final Set<WireConnection> connections = new HashSet<>();

    public static WireNetworkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WireNetworkSavedData data = new WireNetworkSavedData();
        ListTag entries = tag.getList("connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            WireType wireType = WireType.byName(entry.getString("wireType"));
            if (wireType == null) continue;

            WireEndpoint start = new WireEndpoint(
                    new BlockPos(entry.getInt("startX"), entry.getInt("startY"), entry.getInt("startZ")),
                    entry.getInt("startIndex")
            );
            WireEndpoint end = new WireEndpoint(
                    new BlockPos(entry.getInt("endX"), entry.getInt("endY"), entry.getInt("endZ")),
                    entry.getInt("endIndex")
            );
            data.connections.add(new WireConnection(start, end, wireType));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (WireConnection connection : connections) {
            CompoundTag entry = new CompoundTag();
            WireEndpoint start = connection.start();
            WireEndpoint end = connection.end();
            entry.putString("wireType", connection.wireType().getSerializedName());
            entry.putInt("startX", start.pos().getX());
            entry.putInt("startY", start.pos().getY());
            entry.putInt("startZ", start.pos().getZ());
            entry.putInt("startIndex", start.endpointIndex());
            entry.putInt("endX", end.pos().getX());
            entry.putInt("endY", end.pos().getY());
            entry.putInt("endZ", end.pos().getZ());
            entry.putInt("endIndex", end.endpointIndex());
            entries.add(entry);
        }
        tag.put("connections", entries);
        return tag;
    }

    public Set<WireConnection> connections() {
        return Set.copyOf(connections);
    }

    public void replaceConnections(Set<WireConnection> newConnections) {
        connections.clear();
        connections.addAll(newConnections);
        setDirty();
    }
}
