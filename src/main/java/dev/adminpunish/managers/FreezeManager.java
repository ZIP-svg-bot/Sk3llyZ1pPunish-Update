package dev.adminpunish.managers;

import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FreezeManager {

    private final Set<UUID> frozen = new HashSet<>();
    // Location the player was at when frozen, used to snap them back if they try to move
    private final Map<UUID, Location> freezeAnchor = new HashMap<>();

    public boolean isFrozen(UUID uuid) {
        return frozen.contains(uuid);
    }

    public void freeze(Player player) {
        frozen.add(player.getUniqueId());
        freezeAnchor.put(player.getUniqueId(), player.getLocation().clone());
    }

    public void unfreeze(Player player) {
        frozen.remove(player.getUniqueId());
        freezeAnchor.remove(player.getUniqueId());
    }

    public Location getAnchor(UUID uuid) {
        return freezeAnchor.get(uuid);
    }

    public void updateAnchor(UUID uuid, Location location) {
        freezeAnchor.put(uuid, location);
    }

    public Set<UUID> getFrozen() { return frozen; }
}
