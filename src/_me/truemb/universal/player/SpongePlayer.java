package _me.truemb.universal.player;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import _me.truemb.universal.enums.ServerType;

public class SpongePlayer extends UniversalPlayer{
	
	private final Player player;

	public SpongePlayer(Player player) {
		super(ServerType.SPONGE, player.getUniqueId(), player.getName());
		this.player = player;
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

}
