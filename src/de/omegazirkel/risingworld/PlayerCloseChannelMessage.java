package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerCloseChannelMessage extends PlayerMessage {
    public String channel;

    PlayerCloseChannelMessage(Player player) {
        super(player);
    }
}