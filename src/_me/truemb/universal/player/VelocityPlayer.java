package _me.truemb.universal.player;

import com.velocitypowered.api.proxy.Player;

public class VelocityPlayer extends UniversalPlayer{
	
	private final Player player;

	public VelocityPlayer(Player player) {
		super(player.getUniqueId(), player.getUsername());
		this.player = player;
	}
	
	@Override
	public Player getVelocityPlayer() {
		return this.player;
	}

}
