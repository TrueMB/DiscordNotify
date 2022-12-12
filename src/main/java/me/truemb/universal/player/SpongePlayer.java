package me.truemb.universal.player;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3d;

import me.truemb.universal.enums.ServerType;
import net.kyori.adventure.text.Component;

public class SpongePlayer extends UniversalPlayer{
	
	private final ServerPlayer player;

	public SpongePlayer(ServerPlayer player) {
		super(ServerType.SPONGE, player.uniqueId(), player.name());
		this.player = player;
	}
	
	@Override
	public ServerPlayer getSpongePlayer() {
		return this.player;
	}

	@Override
	public UniversalLocation getLocation() {
		ServerLocation loc = this.player.location().onServer().get();
		Vector3d rotation = this.player.headRotation().get();
		return new UniversalLocation(loc.world().properties().key().value(), loc.x(), loc.y(), loc.z(), rotation.x(), rotation.y());
	}

	@Override
	public void sendMessage(String message) {
		this.getSpongePlayer().sendMessage(Component.text(message));
	}

	@Override
	public void sendMessage(Component message) {
		this.getSpongePlayer().sendMessage(message);
	}

	@Override
	public String getIP() {
		return this.getSpongePlayer().connection().address().toString().replace("/", "");
	}

}
