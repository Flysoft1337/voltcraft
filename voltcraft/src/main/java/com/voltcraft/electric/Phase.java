package com.voltcraft.electric;

import net.minecraft.util.StringRepresentable;

public enum Phase implements StringRepresentable {
    LIVE("live", "L"),
    NEUTRAL("neutral", "N"),
    EARTH("earth", "E");

    private final String name;
    private final String shortLabel;

    Phase(String name, String shortLabel) {
        this.name = name;
        this.shortLabel = shortLabel;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public boolean isWorkingPhase() {
        return this == LIVE || this == NEUTRAL;
    }

    public static Phase byName(String name) {
        for (Phase phase : values()) {
            if (phase.name.equals(name)) {
                return phase;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
