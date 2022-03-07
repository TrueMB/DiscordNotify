package me.truemb.universal.server;

import java.util.logging.Logger;

import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;

import eu.mcdb.util.SLF4JWrapper;

public class SpongeServer extends UniversalServer {

	private final Server server;
	private final Logger logger;

	public SpongeServer() {
		this.server = Sponge.getServer();
		this.logger = new SLF4JWrapper();
	}

	@Override
	public SpongeServer getSpongeServer() {
		return this;
	}

	@Override
	public Logger getLogger() {
		return this.logger;
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
