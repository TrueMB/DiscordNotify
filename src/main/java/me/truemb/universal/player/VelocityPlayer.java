package me.truemb.universal.player;

import com.velocitypowered.api.proxy.Player;

import me.truemb.universal.enums.ServerType;
import net.kyori.adventure.text.Component;

public class VelocityPlayer extends UniversalPlayer{
	
	private final Player player;

	public VelocityPlayer(Player player) {
		super(ServerType.VELOCITY, player.getUniqueId(), player.getUsername());
		this.player = player;
	}
	
	@Override
	public Player getVelocityPlayer() {
		return this.player;
	}

	@Override
	public UniversalLocation getLocation() {
		return null;
	}

	@Override
	public void sendMessage(String message) {
		this.getVelocityPlayer().sendMessage(Component.text(message));
	}

	@Override
	public void sendMessage(Component message) {
		this.getVelocityPlayer().sendMessage(message);
	}

	@Override
	public String getIP() {
		return this.getVelocityPlayer().getRemoteAddress().getAddress().getHostAddress().split(":")[0].replace("/", "");
	}

}
