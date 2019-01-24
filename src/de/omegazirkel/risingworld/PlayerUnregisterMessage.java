package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerUnregisterMessage extends PlayerMessage {
    public boolean unregister;

    PlayerUnregisterMessage(Player player) {
        super(player);
        this.unregister = true;
    }
}