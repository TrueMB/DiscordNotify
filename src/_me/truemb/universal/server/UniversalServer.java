package _me.truemb.universal.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import _me.truemb.universal.enums.ServerType;
import _me.truemb.universal.player.UniversalPlayer;
import lombok.Getter;

public abstract class UniversalServer {

	@Getter private Collection<UniversalPlayer> onlinePlayers = new ArrayList<>();
	
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
											.toList();
		
		return players.size() > 0 ? players.get(0) : null;
	}

	public abstract Logger getLogger();
	
	public abstract boolean isOnlineMode();
	
	public abstract boolean isProxySubServer();

	public abstract void broadcast(String message);

	public abstract void broadcast(String message, String permission);
	
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
