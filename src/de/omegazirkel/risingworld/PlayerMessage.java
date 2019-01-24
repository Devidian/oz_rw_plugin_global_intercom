package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public abstract class PlayerMessage {
    public String playerUID; // should be long but JavaScript cant handle this (yet)
    public String playerName;

    PlayerMessage(Player player) {
        // Player stuff
        this.playerName = player.getName();
        this.playerUID = "" + player.getUID();
    }
}