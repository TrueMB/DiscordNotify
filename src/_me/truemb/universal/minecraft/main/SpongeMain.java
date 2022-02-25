package _me.truemb.universal.minecraft.main;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;

import com.google.inject.Inject;

import _me.truemb.universal.enums.ServerType;
import _me.truemb.universal.minecraft.events.SpongeEventsListener;
import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.main.DiscordNotifyMain;

@Plugin(id = "discordnotify", name = "DiscordNotify", version = "3.0.0", authors = {"TrueMB"})
public class SpongeMain {
	
	private DiscordNotifyMain instance;
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;

    @Listener
    public void onServerStart(GameStartedServerEvent e) {
    	Game game = Sponge.getGame();
		this.instance = new DiscordNotifyMain(this.configDir.toFile(), ServerType.VELOCITY);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(Player all : game.getServer().getOnlinePlayers()) {
			UUID uuid = all.getUniqueId();
			String name = all.getName();
			
			players.add(new UniversalPlayer(uuid, name));
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		SpongeEventsListener listener = new SpongeEventsListener(this.instance);
		Sponge.getEventManager().registerListeners(this, listener);
		
		//TODO LOAD COMMANDS
	}

    @Listener
    public void onDisable(GameStoppingServerEvent e) {
    	this.instance.onDisable();
    }

}
