package _me.truemb.universal.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import _me.truemb.universal.enums.ServerType;

public class BukkitPlayer extends UniversalPlayer{
	
	private final Player player;

	public BukkitPlayer(Player player) {
		super(ServerType.BUKKIT, player.getUniqueId(), player.getName());
		this.player = player;
	}
	
	@Override
	public Player getBukkitPlayer() {
		return this.player;
	}

	@Override
	public UniversalLocation getLocation() {
		Location loc = this.player.getLocation();
		return new UniversalLocation(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
	}

	@Override
	public void sendMessage(String message) {
		this.getBukkitPlayer().sendMessage(message);
	}

}
