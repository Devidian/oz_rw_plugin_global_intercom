package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerOverrideChangeMessage extends PlayerMessage {
    public boolean override = true;

    PlayerOverrideChangeMessage(Player player) {
        super(player);
    }
}