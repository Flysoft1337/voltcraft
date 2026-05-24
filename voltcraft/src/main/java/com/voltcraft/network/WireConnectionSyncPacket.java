package com.voltcraft.network;

import com.voltcraft.VoltCraft;
import com.voltcraft.client.WireRendererState;
import com.voltcraft.electric.Phase;
import com.voltcraft.electric.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

/**
 * 线缆连接同步包。
 * 服务端发送所有连接信息到客户端用于渲染。
 */
public record WireConnectionSyncPacket(Set<WireConnectionData> connections) implements CustomPacketPayload {

    public static final Type<WireConnectionSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "wire_sync"));

    public static final StreamCodec<FriendlyByteBuf, WireConnectionSyncPacket> CODEC =
            new StreamCodec<>() {
                @Override
                public WireConnectionSyncPacket decode(FriendlyByteBuf buf) {
                    return WireConnectionSyncPacket.decode(buf);
                }

                @Override
                public void encode(FriendlyByteBuf buf, WireConnectionSyncPacket packet) {
                    WireConnectionSyncPacket.encode(packet, buf);
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(WireConnectionSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.connections.size());
        for (WireConnectionData data : packet.connections) {
            writeBlockPos(buf, data.startPos());
            buf.writeVarInt(data.startIndex());
            buf.writeEnum(data.startPhase());
            writeBlockPos(buf, data.endPos());
            buf.writeVarInt(data.endIndex());
            buf.writeEnum(data.endPhase());
            buf.writeEnum(data.wireType());
        }
    }

    public static WireConnectionSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<WireConnectionData> connections = new HashSet<>();
        for (int i = 0; i < count; i++) {
            BlockPos startPos = readBlockPos(buf);
            int startIndex = buf.readVarInt();
            Phase startPhase = buf.readEnum(Phase.class);
            BlockPos endPos = readBlockPos(buf);
            int endIndex = buf.readVarInt();
            Phase endPhase = buf.readEnum(Phase.class);
            WireType wireType = buf.readEnum(WireType.class);
            connections.add(new WireConnectionData(startPos, startIndex, startPhase, endPos, endIndex, endPhase, wireType));
        }
        return new WireConnectionSyncPacket(connections);
    }

    public static void handle(WireConnectionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 更新客户端渲染状态
            WireRendererState.setConnections(packet.connections);
        });
    }

    private static void writeBlockPos(FriendlyByteBuf buf, BlockPos pos) {
        buf.writeVarInt(pos.getX());
        buf.writeVarInt(pos.getY());
        buf.writeVarInt(pos.getZ());
    }

    private static BlockPos readBlockPos(FriendlyByteBuf buf) {
        return new BlockPos(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public record WireConnectionData(
            BlockPos startPos,
            int startIndex,
            Phase startPhase,
            BlockPos endPos,
            int endIndex,
            Phase endPhase,
            WireType wireType
    ) {}
}
