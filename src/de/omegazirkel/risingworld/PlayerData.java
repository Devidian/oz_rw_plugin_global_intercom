package de.omegazirkel.risingworld;

import java.util.List;

import net.risingworld.api.objects.Player;

public class PlayerData {

	public String playerName;
	public String playerUID;// should be long but JavaScript cant handle this (yet)
	public List<String> channelList;

	PlayerData(Player player) {
		// Player stuff
		this.playerName = player.getName();
		this.playerUID = "" + player.getUID();
	}
}