package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerLeaveChannelMessage extends PlayerMessage {
    public String channel;

    PlayerLeaveChannelMessage(Player player) {
        super(player);
    }
}