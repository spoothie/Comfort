package net.spoothie.comfort;

import java.util.Map;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.Packet18ArmAnimation;
import net.minecraft.server.Packet40EntityMetadata;
import net.spoothie.comfort.Comfort;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public class EventListener implements Listener {

	public Comfort plugin;
	
	public EventListener(Comfort plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.hasBlock() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();
			
			if(plugin.comfortBlocks.containsKey(block.getType())) {
				Player player = event.getPlayer();				
				
				// Permissions check.
				if(!player.hasPermission("comfort.sit"))
					return;
				
				// Check if player is sitting.
				if(plugin.comfortPlayers.containsKey(player)) {
					plugin.standUp(player);
					return;
				}
				
				// Check for distance between player and chair.
				if(plugin.distance > 0 && player.getLocation().distance(block.getLocation().add(0.5, 0.5, 0.5)) > plugin.distance)
					return;
				
				// Check if player is sneaking.
				if(plugin.sneaking == false || (plugin.sneaking == true && player.isSneaking())) {
					plugin.sitDown(player, block);
					
					// Cancel BlockPlaceEvent Result, if player is rightclicking with a block in his hand.
					event.setUseInteractedBlock(Result.DENY);
				}
			}
		}
	}
	
	// Let sitting/laying players stand up when their chair/bed is destroyed
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		
		if(plugin.comfortPlayers.containsValue(block)) {				
			for(Map.Entry<Player, Block> e : plugin.comfortPlayers.entrySet()) {
				if(e.getValue() == block) {
					plugin.standUp(e.getKey());
					return;
				}
			}
		}
	}
	
	// Make standing up possible for any blocks
	@EventHandler
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		if(event.getBed().getType() != Material.BED || event.getBed().getType() != Material.BED_BLOCK) {
			EntityPlayer ep = ((CraftPlayer)event.getPlayer()).getHandle();
			Packet18ArmAnimation arm = new Packet18ArmAnimation(ep, 3);
			
			for(Player p : plugin.getServer().getOnlinePlayers())
				((CraftPlayer)p).getHandle().netServerHandler.sendPacket(arm);
			
			event.getPlayer().setFlying(false);
			event.getPlayer().setAllowFlight(false);
			plugin.comfortPlayers.remove(event.getPlayer());
		}
	}
	
	// Send packets to joining players (1 tick delayed because you can't send packets to players right on join).
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			   public void run() {
				   	for(Map.Entry<Player, Block> e : plugin.comfortPlayers.entrySet()) {
						Packet40EntityMetadata packet = new Packet40EntityMetadata(e.getKey().getEntityId(), new ComfortDataWatcher((byte) 0x04));
						((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
					}
			   }
		}, 1L);
	}
	
	// Let players stand up on disconnect.
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if(plugin.comfortPlayers.containsKey(event.getPlayer()))
			plugin.standUp(event.getPlayer());
	}
	
	// Let players stand up on teleport.
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if(event.getCause() != TeleportCause.UNKNOWN && plugin.comfortPlayers.containsKey(event.getPlayer())) {
			if(!plugin.comfortBlocks.containsKey(event.getTo().getBlock()))
				plugin.standUp(event.getPlayer());
		}
	}
	
	// Freeze players on chairs
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if(plugin.comfortPlayers.containsKey(event.getPlayer()) && event.getFrom().distance(event.getTo()) > 0)
			event.setTo(event.getFrom());
	}
	
	// Prevent players from standing up by starting to sprint
	@EventHandler
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		if(plugin.comfortPlayers.containsKey(event.getPlayer()))
			event.setCancelled(true);
	}
	
	// Prevent players from standing up by starting to crouch
	@EventHandler
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		if(plugin.comfortPlayers.containsKey(event.getPlayer()))
			event.setCancelled(true);
	}
}
