package _me.truemb.universal.server;

import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

public class BungeecordServer extends UniversalServer {

	@Override
	public BungeecordServer getBungeeServer() {
		return this;
	}
	
	@Override
	public Logger getLogger() {
		return ProxyServer.getInstance().getLogger();
	}

	@Override
	public void broadcast(String message) {
		ProxyServer.getInstance().broadcast(new TextComponent(message));
	}

	@Override
	public void broadcast(String message, String permission) {
		ProxyServer.getInstance().getPlayers().forEach(player -> {
			if(player.hasPermission(permission)) player.sendMessage(new TextComponent(message));
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isOnlineMode() {
		return ProxyServer.getInstance().getConfig().isOnlineMode();
	}

}
