package me.truemb.universal.minecraft.main;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.network.ChannelBinding.RawDataChannel;
import org.spongepowered.api.plugin.Plugin;

import com.google.inject.Inject;

import lombok.Getter;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.universal.enums.ServerType;
import me.truemb.universal.messenger.IMessageChannel;
import me.truemb.universal.messenger.IRelay;
import me.truemb.universal.messenger.MessageChannelAPI;
import me.truemb.universal.messenger.MessageChannelCore;
import me.truemb.universal.messenger.MessageChannelException;
import me.truemb.universal.messenger.PipelineMessage;
import me.truemb.universal.minecraft.events.SpongeEventsListener;
import me.truemb.universal.player.SpongePlayer;
import me.truemb.universal.player.UniversalPlayer;
import net.kyori.adventure.platform.spongeapi.SpongeAudiences;

//@Plugin(id = "${project.artifactId}", name = "${project.name}", version = "${project.version}", authors = {"TrueMB"})
@Plugin(id = "discordnotify", name = "DiscordNotify", version = "3.0.0", authors = {"TrueMB"})
public class SpongeMain implements IRelay {
	
	private DiscordNotifyMain instance;
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;
	
    @Inject
    private Game game;
    private IMessageChannel core;
    private RawDataChannel outgoing;
    
    @Getter
    private final SpongeAudiences adventure;

    @Inject
    public SpongeMain(final SpongeAudiences adventure) {
        this.adventure = adventure;
	}

    @Listener
    public void onServerStart(GameStartedServerEvent e) {
    	Game game = Sponge.getGame();
		this.instance = new DiscordNotifyMain(this.configDir.toFile(), ServerType.VELOCITY);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(Player all : game.getServer().getOnlinePlayers()) {
			players.add(new SpongePlayer(all, this.getAdventure()));
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		SpongeEventsListener listener = new SpongeEventsListener(this.instance, this.adventure);
		Sponge.getEventManager().registerListeners(this, listener);
		
		//TODO LOAD COMMANDS
	}

    @Listener
    public void onDisable(GameStoppingServerEvent e) {
    	if(this.instance != null)
    		this.instance.onDisable();
    }
    
    @Listener
    public void onGameConstruction(GameConstructionEvent event) {
        this.core = new MessageChannelCore(this);

        try {
            MessageChannelAPI.setCore(core);
        } catch (MessageChannelException exception) {
            exception.printStackTrace();
        }
    }

    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        this.outgoing = game.getChannelRegistrar().createRawChannel(this, "messagechannel:proxy");
        game.getChannelRegistrar().createRawChannel(this, "messagechannel:server").addListener(Platform.Type.SERVER,
                (buffer, connection, side) -> {
                    try {
                        core.getPipelineRegistry().receive(buffer.readBytes(buffer.available()));
                    } catch (UnsupportedOperationException exception) {
                        exception.printStackTrace();
                    }
                });
    }

    @Override
    public boolean send(PipelineMessage message, byte[] data) {
        if (game.getServer().getOnlinePlayers().size() > 0) {
            Player player = (Player) game.getServer().getOnlinePlayers().toArray()[0];
            if (player != null) {
                outgoing.sendTo(player, (buffer) -> {
                    buffer.writeBytes(data);
                });
            }
        }
        return false;
    }

    @Override
    public boolean broadcast(PipelineMessage message, byte[] data) {
        return false;
    }

    @Override
    public boolean isProxy() {
        return false;
    }

}
