package me.truemb.universal.server;

import java.util.logging.Logger;

import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;

public class SpongeServer extends UniversalServer {

	private final Server server;

	public SpongeServer() {
		this.server = Sponge.getServer();
	}

	@Override
	public SpongeServer getSpongeServer() {
		return this;
	}

	@Override
	public Logger getLogger() {
		return Logger.getLogger("DiscordNotify");
	}

	@Override
	public void broadcast(String message) {
		this.server.getBroadcastChannel().send(Text.of(message));
	}

	@Override
	public void broadcast(String message, String permission) {
		this.server.getOnlinePlayers().forEach(player -> {
			if(player.hasPermission(permission))
				player.sendMessage(Text.of(message));
		});
	}

	@Override
	public boolean isOnlineMode() {
		return this.server.getOnlineMode();
	}

	@Override
	public boolean isProxySubServer() {
		// TODO Auto-generated method stub
		return false;
	}

}
