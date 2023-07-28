package me.truemb.universal.server;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import lombok.Getter;
import me.truemb.universal.enums.ServerType;
import me.truemb.universal.player.UniversalPlayer;

public abstract class UniversalServer {

	@Getter private Collection<UniversalPlayer> onlinePlayers = new ArrayList<>();
	@Getter private HashMap<String, SocketAddress> servers = new HashMap<String, SocketAddress>();
	
	public void loadPlayers(Collection<UniversalPlayer> players) {
		this.onlinePlayers = players;
	}
	
	public void addPlayer(UniversalPlayer up) {
		this.onlinePlayers.add(up);
	}
	
	public void removePlayer(UniversalPlayer up) {
		this.onlinePlayers.remove(up);
	}

	public UniversalPlayer getPlayer(UUID uuid) {
		List<UniversalPlayer> players = this.onlinePlayers.stream()
											.filter(up -> up.getUUID().equals(uuid))
											.collect(Collectors.toList());
		
		return players.size() > 0 ? players.get(0) : null;
	}

	public UniversalPlayer getPlayer(String username) {
		List<UniversalPlayer> players = this.onlinePlayers.stream()
											.filter(up -> up.getIngameName().equals(username))
											.collect(Collectors.toList());
		
		return players.size() > 0 ? players.get(0) : null;
	}
	
	public abstract int getMaxPlayers();
	
	public abstract String getMotd();

	public void loadServers(HashMap<String, SocketAddress> servers) {
		this.servers = servers;
	}
	
	public abstract Logger getLogger();
	
	public abstract boolean isOnlineMode();
	
	public abstract boolean isProxySubServer();

	public abstract void broadcast(String message);

	public abstract void broadcast(String message, String permission);
	
	public abstract void sendCommandToConsole(String command);
	
	//SEVER INSTANCE
	public static UniversalServer buildServer(ServerType type) {
        if (type.equals(ServerType.BUKKIT)) return new BukkitServer();
        else if (type.equals(ServerType.SPONGE)) return new SpongeServer();
        else if (type.equals(ServerType.VELOCITY)) return new VelocityServer();
        else if (type.equals(ServerType.BUNGEECORD)) return new BungeecordServer();
        else return null;
	}
	
    public BungeecordServer getBungeeServer() {
        return null;
    }

    public BukkitServer getBukkitServer() {
        return null;
    }

    public VelocityServer getVelocityServer() {
        return null;
    }

    public SpongeServer getSpongeServer() {
        return null;
    }
    
    //CHECKING 
    public ServerType getServerPlatform() {
        if (this.getBukkitServer() != null) return ServerType.BUKKIT;
        else if (this.getSpongeServer() != null) return ServerType.SPONGE;
        else if (this.getVelocityServer() != null) return ServerType.VELOCITY;
        else if (this.getBungeeServer() != null) return ServerType.BUNGEECORD;
        else return ServerType.UNKNOWN;
    }
    
    public boolean isProxy() {
       return this.getServerPlatform().equals(ServerType.BUNGEECORD) || this.getServerPlatform().equals(ServerType.VELOCITY);
    }
}
