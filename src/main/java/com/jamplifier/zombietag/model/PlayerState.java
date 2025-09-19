package com.jamplifier.zombietag.model;

import java.util.UUID;

/**
 * Represents the in-game state of one player.
 */
public class PlayerState {
    private final UUID uuid;
    private boolean ingame;
    private boolean zombie;

    public PlayerState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isIngame() {
        return ingame;
    }

    public void setIngame(boolean ingame) {
        this.ingame = ingame;
    }

    public boolean isZombie() {
        return zombie;
    }

    public void setZombie(boolean zombie) {
        this.zombie = zombie;
    }
}
