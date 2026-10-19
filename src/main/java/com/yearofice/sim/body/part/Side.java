package com.yearofice.sim.body.part;

/** Body side. The main side follows Minecraft's Main Hand option (right by default). */
public enum Side {
    LEFT, RIGHT, NONE;

    public Side other() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case NONE -> NONE;
        };
    }
}
