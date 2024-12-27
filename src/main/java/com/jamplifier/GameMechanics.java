package com.jamplifier;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.jamplifier.PlayerData.PlayerManager;

public class GameMechanics implements Listener{
	
	
	private MainClass plugin = MainClass.getPlugin(MainClass.class);
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		
		plugin.playermanager.put(uuid, new PlayerManager(uuid, false, false));
		
	}
	
	
}
