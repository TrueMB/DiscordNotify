package _me.truemb.universal.player;

import _me.truemb.universal.enums.ServerType;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeePlayer extends UniversalPlayer{
	
	private final ProxiedPlayer player;

	public BungeePlayer(ProxiedPlayer player) {
		super(ServerType.BUNGEECORD, player.getUniqueId(), player.getName());
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

	@Override
	public void sendMessage(String message) {
		this.getBungeePlayer().sendMessage(new net.md_5.bungee.api.chat.TextComponent(message));
	}

}
