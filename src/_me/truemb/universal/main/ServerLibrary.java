package _me.truemb.universal.main;

import java.util.ArrayList;
import java.util.Collection;

import _me.truemb.universal.enums.ServerType;
import eu.mcdb.universal.player.UniversalPlayer;
import lombok.Getter;

public class ServerLibrary {
	
	@Getter private Collection<UniversalPlayer> onlinePlayers = new ArrayList<>();

	public ServerLibrary(ServerType type) {
		
	}
}
