package me.truemb.universal.player;

import me.truemb.universal.enums.ServerType;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeePlayer extends UniversalPlayer{
	
	private final ProxiedPlayer player;
	private final BungeeAudiences adventure;

	public BungeePlayer(ProxiedPlayer player, BungeeAudiences adventure) {
		super(ServerType.BUNGEECORD, player.getUniqueId(), player.getName());
		this.player = player;
		this.adventure = adventure;
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
		this.getBungeePlayer().sendMessage(TextComponent.fromLegacyText(message));
	}

	@Override
	public void sendMessage(Component message) {
		this.adventure.player(this.getBungeePlayer()).sendMessage(message);
	}

	@Override
	public String getIP() {
		return this.getBungeePlayer().getSocketAddress().toString().split(":")[0].replace("/", "");
	}

}
