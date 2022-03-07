package me.truemb.universal.player;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import me.truemb.universal.enums.ServerType;
import net.kyori.adventure.platform.spongeapi.SpongeAudiences;
import net.kyori.adventure.text.Component;

public class SpongePlayer extends UniversalPlayer{
	
	private final Player player;
	private final SpongeAudiences adventure;

	public SpongePlayer(Player player, SpongeAudiences adventure) {
		super(ServerType.SPONGE, player.getUniqueId(), player.getName());
		this.player = player;
		this.adventure = adventure;
	}
	
	@Override
	public Player getSpongePlayer() {
		return this.player;
	}

	@Override
	public UniversalLocation getLocation() {
		Location<World> loc = this.player.getLocation();
		Vector3d rotation = this.player.getHeadRotation();
		return new UniversalLocation(loc.getExtent().getName(), loc.getX(), loc.getY(), loc.getZ(), rotation.getX(), rotation.getY());
	}

	@Override
	public void sendMessage(String message) {
		this.getSpongePlayer().sendMessage(org.spongepowered.api.text.Text.of(message));
	}

	@Override
	public void sendMessage(Component message) {
		this.adventure.player(this.getSpongePlayer()).sendMessage(message);
	}

}
