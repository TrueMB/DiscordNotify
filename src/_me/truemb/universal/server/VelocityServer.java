package _me.truemb.universal.server;

import java.util.logging.Logger;

import com.velocitypowered.api.proxy.ProxyServer;

import eu.mcdb.util.SLF4JWrapper;
import net.kyori.adventure.text.Component;

public class VelocityServer extends UniversalServer{
	
	private ProxyServer proxyServer;
    private final Logger logger;
    
    public VelocityServer() { 
    	this.logger = new SLF4JWrapper();
	}
    
    public void setInstance(ProxyServer server) {
    	this.proxyServer = server;
    }

    @Override
    public VelocityServer getVelocityServer() {
    	return this;
    }
    
	@Override
	public Logger getLogger() {
		return this.logger;
	}

	@Override
	public void broadcast(String message) {
		this.proxyServer.getAllPlayers().forEach(player -> {
			player.sendMessage(Component.text(message));
		});
	}

	@Override
	public void broadcast(String message, String permission) {
		this.proxyServer.getAllPlayers().forEach(player -> {
			if(player.hasPermission(permission))
				player.sendMessage(Component.text(message));
		});
	}

	@Override
	public boolean isOnlineMode() {
		return this.proxyServer.getConfiguration().isOnlineMode();
	}

}
