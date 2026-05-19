package com.voltcraft.electric.wire;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 一个能被软线接入的方块（通常是机器 BlockEntity）。
 *
 * 实现者在构造时定义自己有哪些 {@link WireAnchor}（每个柱子绑定一个 phase + 本地坐标），
 * SoftCableEntity 通过 {@link WireAnchorRef} 反向解析到这里取 anchor。
 *
 * 能量传输：每个 anchor 背后有一个 IEnergyStorage buffer（实现者自管），
 * 软线 entity 在自己 tick 时从源端 buffer 拉、推到目标端 buffer。
 */
public interface WireAnchorOwner {

    /**
     * 拿到指定 index 的 anchor。index 越界或不存在返回 null。
     * 这是 lazy 查询接口，每次软线 tick 都会调一次，实现要做 O(1)。
     */
    @Nullable
    WireAnchor anchor(int index);

    /** 总共暴露多少个 anchor。 */
    int anchorCount();

    /**
     * 拿到指定 anchor 背后的能量缓存。null 表示该 anchor 当前不可用（停电、断路等）。
     * 软线 entity 的 tick 会同时从两端 buffer 取值，做差额转移。
     */
    @Nullable
    IEnergyStorage anchorBuffer(int index);

    /**
     * 解析 anchor 在世界中的绝对位置。默认实现：方块原点 + anchor.localOffset。
     * 朝向感知的机器（变压器、空开等）可以 override 让接线柱跟着 FACING 旋转。
     */
    default Vec3 anchorWorldPos(WireAnchor anchor, net.minecraft.core.BlockPos blockPos) {
        return new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()).add(anchor.localOffset());
    }
}

