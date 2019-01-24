package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerRemoveChannelMessage extends PlayerMessage {
    public String channel;

    PlayerRemoveChannelMessage(Player player) {
        super(player);
    }
}