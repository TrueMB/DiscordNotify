package _me.truemb.universal.player;

import com.velocitypowered.api.proxy.Player;

import _me.truemb.universal.enums.ServerType;

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
		this.getVelocityPlayer().sendMessage(net.kyori.adventure.text.Component.text(message));
	}

}
