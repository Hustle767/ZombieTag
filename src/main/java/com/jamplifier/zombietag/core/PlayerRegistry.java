package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.model.PlayerState;

import java.util.*;

/**
 * Tracks all PlayerState objects in memory.
 * Replaces the old HashMap<UUID, PlayerManager>.
 */
public class PlayerRegistry {
    private final Map<UUID, PlayerState> states = new HashMap<>();

    /** Get existing state or create a new one */
    public PlayerState getOrCreate(UUID id) {
        return states.computeIfAbsent(id, PlayerState::new);
    }

    /** Get state if present */
    public PlayerState get(UUID id) {
        return states.get(id);
    }

    public boolean contains(UUID id) {
        return states.containsKey(id);
    }

    public void remove(UUID id) {
        states.remove(id);
    }

    /** Iterate all known states */
    public Collection<PlayerState> all() {
        return states.values();
    }
    public List<UUID> getSurvivors() {
        List<UUID> list = new ArrayList<>();
        for (var entry : states.entrySet()) {
            PlayerState ps = entry.getValue();
            if (ps.isIngame() && !ps.isZombie()) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public List<UUID> getZombies() {
        List<UUID> list = new ArrayList<>();
        for (var entry : states.entrySet()) {
            PlayerState ps = entry.getValue();
            if (ps.isIngame() && ps.isZombie()) {
                list.add(entry.getKey());
            }
        }
        return list;
    }


    /** Reset round flags (ingame/zombie) for a set of participants */
    public void resetRoundFlags(Collection<UUID> participants) {
        for (UUID id : participants) {
            PlayerState ps = states.get(id);
            if (ps != null) {
                ps.setIngame(false);
                ps.setZombie(false);
            }
        }
    }
}
