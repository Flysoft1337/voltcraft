package com.voltcraft.network;

import com.voltcraft.VoltCraft;
import com.voltcraft.client.WireRendererState;
import com.voltcraft.electric.WireType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 待连接线缆同步包。
 * 服务端通知客户端：玩家正在连接线缆，起点在哪里。
 */
public record PendingWireSyncPacket(BlockPos startPos, WireType wireType, boolean active) implements CustomPacketPayload {

    public static final Type<PendingWireSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VoltCraft.MOD_ID, "pending_wire"));

    public static final StreamCodec<FriendlyByteBuf, PendingWireSyncPacket> CODEC =
            new StreamCodec<>() {
                @Override
                public PendingWireSyncPacket decode(FriendlyByteBuf buf) {
                    return new PendingWireSyncPacket(
                            buf.readBlockPos(),
                            buf.readEnum(WireType.class),
                            buf.readBoolean()
                    );
                }

                @Override
                public void encode(FriendlyByteBuf buf, PendingWireSyncPacket packet) {
                    buf.writeBlockPos(packet.startPos());
                    buf.writeEnum(packet.wireType());
                    buf.writeBoolean(packet.active());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PendingWireSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active()) {
                WireRendererState.setPendingWire(packet.startPos(), packet.wireType());
            } else {
                WireRendererState.clearPendingWire();
            }
        });
    }
}
