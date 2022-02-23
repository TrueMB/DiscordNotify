package _me.truemb.universal.player;

import org.spongepowered.api.entity.living.player.Player;

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

}
