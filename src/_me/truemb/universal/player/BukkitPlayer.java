package _me.truemb.universal.player;

import org.bukkit.entity.Player;

public class BukkitPlayer extends UniversalPlayer{
	
	private final Player player;

	public BukkitPlayer(Player player) {
		super(player.getUniqueId(), player.getName());
		this.player = player;
	}
	
	@Override
	public Player getBukkitPlayer() {
		return this.player;
	}

}
