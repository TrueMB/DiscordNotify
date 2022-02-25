package _me.truemb.universal.player;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

public class SpongePlayer extends UniversalPlayer{
	
	private final Player player;

	public SpongePlayer(Player player) {
		super(player.getUniqueId(), player.getName());
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

}
