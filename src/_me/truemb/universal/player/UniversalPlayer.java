package _me.truemb.universal.player;

import java.util.UUID;

import _me.truemb.universal.enums.ServerType;
import lombok.Getter;

@Getter
public class UniversalPlayer {
	
	private UUID UUID;
	private String ingameName;
	private String server;
	
	public UniversalPlayer(UUID uuid, String ingameName) {
		this.UUID = uuid;
		this.ingameName = ingameName;
	}

	public void setServer(String server) {
		this.server = server;
	}

	
	//PLAYER INSTANCE
	public net.md_5.bungee.api.connection.ProxiedPlayer getBungeePlayer() {
        return null;
    }

    public org.bukkit.entity.Player getBukkitPlayer() {
        return null;
    }

    public com.velocitypowered.api.proxy.Player getVelocityPlayer() {
        return null;
    }

    public org.spongepowered.api.entity.living.player.Player getSpongePlayer() {
        return null;
    }
    
    public UniversalLocation getLocation() {
		return null;
	}
    
    //CHECKING 
    public ServerType getServerPlatform() {
        if (this.getBukkitPlayer() != null) return ServerType.BUKKIT;
        else if (this.getSpongePlayer() != null) return ServerType.SPONGE;
        else if (this.getVelocityPlayer() != null) return ServerType.VELOCITY;
        else if (this.getBungeePlayer() != null) return ServerType.BUNGEECORD;
        else return ServerType.UNKNOWN;
    }
    
    //TODO SWITCH STATEMENT GOES THROUGH EVERYTHING. IMPORT ERROR?
    public void sendMessage(String message) {
    	switch (this.getServerPlatform()) {
		case BUKKIT:
			this.getBukkitPlayer().sendMessage(message);
			break;
		case SPONGE:
			this.getSpongePlayer().sendMessage(org.spongepowered.api.text.Text.of(message));
			break;
		case BUNGEECORD:
			this.getBungeePlayer().sendMessage(new net.md_5.bungee.api.chat.TextComponent(message));
			break;
		case VELOCITY:
			this.getVelocityPlayer().sendMessage(net.kyori.adventure.text.Component.text(message));
			break;
		default:
			break;
		}
    }

    public boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) return true;

        switch (this.getServerPlatform()) {
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
        switch (this.getServerPlatform()) {
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
