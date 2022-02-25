package _me.truemb.universal.player;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeePlayer extends UniversalPlayer{
	
	private final ProxiedPlayer player;

	public BungeePlayer(ProxiedPlayer player) {
		super(player.getUniqueId(), player.getName());
		this.player = player;
	}
	
	@Override
	public ProxiedPlayer getBungeePlayer() {
		return this.player;
	}

	@Override
	public UniversalLocation getLocation() {
		return null;
	}

}
