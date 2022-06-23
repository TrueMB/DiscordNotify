package me.truemb.universal.player;

import java.util.UUID;

import lombok.Getter;
import me.truemb.universal.enums.ServerType;
import net.kyori.adventure.text.Component;

@Getter
public abstract class UniversalPlayer {
	
	private UUID UUID;
	private String ingameName;
	
	private ServerType serverType;
	private String server;
	
	public UniversalPlayer(ServerType type, UUID uuid, String ingameName) {
		this.serverType = type;
		this.UUID = uuid;
		this.ingameName = ingameName;
	}

	public void setServer(String server) {
		this.server = server;
	}

	
	//PLAYER 
	public net.md_5.bungee.api.connection.ProxiedPlayer getBungeePlayer() {
        return null;
    }

    public org.bukkit.entity.Player getBukkitPlayer() {
        return null;
    }

    public com.velocitypowered.api.proxy.Player getVelocityPlayer() {
        return null;
    }

    public org.spongepowered.api.entity.living.player.server.ServerPlayer getSpongePlayer() {
        return null;
    }
    
    public abstract String getIP();
    
    public abstract UniversalLocation getLocation();
    
    public abstract void sendMessage(String message);
    
    public abstract void sendMessage(Component message);

    public boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) return true;

        switch (this.getServerType()) {
		case BUKKIT:
			return this.getBukkitPlayer().hasPermission(permission);
		case SPONGE:
			return this.getSpongePlayer().hasPermission(permission);
		case BUNGEECORD:
			return this.getBungeePlayer().hasPermission(permission);
		case VELOCITY:
			return this.getVelocityPlayer().hasPermission(permission);
		default:
			return false;
		}
    }
    
    public boolean isOnline() {
        switch (this.getServerType()) {
		case BUKKIT:
			return this.getBukkitPlayer().isOnline();
		case SPONGE:
			return this.getSpongePlayer().isOnline();
		case BUNGEECORD:
			return this.getBungeePlayer().isConnected();
		case VELOCITY:
			return this.getVelocityPlayer().isActive();
		default:
			return false;
		}
    }


}
