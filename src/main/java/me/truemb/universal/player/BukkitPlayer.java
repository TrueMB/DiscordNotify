package me.truemb.universal.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.truemb.universal.enums.ServerType;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;

public class BukkitPlayer extends UniversalPlayer{
	
	private final Player player;
	private final BukkitAudiences adventure;

	public BukkitPlayer(Player player, BukkitAudiences adventure) {
		super(ServerType.BUKKIT, player.getUniqueId(), player.getName());
		this.player = player;
		this.adventure = adventure;
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

	@Override
	public void sendMessage(Component message) {
		this.adventure.player(this.getBukkitPlayer()).sendMessage(message);
	}

	@Override
	public String getIP() {
		return this.getBukkitPlayer().getAddress().getAddress().toString().split(":")[0].replace("/", "");
	}

}
