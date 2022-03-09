package me.truemb.universal.minecraft.main;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.LoadedGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppedGameEvent;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataHandler;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import com.google.inject.Inject;

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

//@Plugin(id = "${project.artifactId}", name = "${project.name}", version = "${project.version}", authors = {"TrueMB"})
//@Plugin(id = "discordnotify", name = "DiscordNotify", version = "3.0.0", authors = {"TrueMB"})
//@Plugin(id = "discordnotify", name = "${project.name}", version = "${project.version}", authors = {"TrueMB"}, dependencies = { @Dependency(id = "spicord", optional = true)} )
//@Plugin(value = "${project.artifactId}")
@Plugin(value = "DiscordNotify")
public class SpongeMain implements IRelay {
	
	private DiscordNotifyMain instance;
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;
	
    @Inject
    private Game game;
    private IMessageChannel core;
    private RawDataChannel outgoing;
    
    @Listener
    public void onServerStart(LoadedGameEvent e) {
    	Game game = e.game();
		this.instance = new DiscordNotifyMain(this.configDir.toFile(), ServerType.SPONGE);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(ServerPlayer all : game.server().onlinePlayers())
			players.add(new SpongePlayer(all));
		this.instance.getUniversalServer().loadPlayers(players);
		
		//TODO LOAD COMMANDS
	}
    
    @Listener
    public void onRegisterCommand(RegisterCommandEvent<Command.Raw> e) {
    	
    }
    
    @Listener
    public void onConstructPlugin(ConstructPluginEvent e) {
    	Game game = e.game();

    	//PLUGIN CHANNEL
        this.core = new MessageChannelCore(this);

        try {
            MessageChannelAPI.setCore(core);
        } catch (MessageChannelException exception) {
            exception.printStackTrace();
        }
        
		//LOAD LISTENER
		SpongeEventsListener listener = new SpongeEventsListener(this.instance);
    	game.eventManager().registerListeners(e.plugin(), listener);
    }

    @Listener
    public void onDisable(StoppedGameEvent e) {
    	if(this.instance != null)
    		this.instance.onDisable();
    }

    @Listener
    public void onRegisterChannel(RegisterChannelEvent e) {

    	this.outgoing = e.register(ResourceKey.of("messagechannel", "proxy"), RawDataChannel.class);
    	e.register(ResourceKey.of("messagechannel", "server"), RawDataChannel.class).play().addHandler(new RawPlayDataHandler<EngineConnection>() {

			@Override
			public void handlePayload(ChannelBuf data, EngineConnection connection) {
				try {
                    core.getPipelineRegistry().receive(data.readBytes(data.available()));
                } catch (UnsupportedOperationException exception) {
                    exception.printStackTrace();
                }
			}
		});
    }
    	//OLD
    	/*
        this.outgoing = game.getChannelRegistrar().createRawChannel(this, "messagechannel:proxy");
        this.game.getChannelRegistrar().createRawChannel(this, "messagechannel:server").addListener(Platform.Type.SERVER, (buffer, connection, side) -> {
                    try {
                        core.getPipelineRegistry().receive(buffer.readBytes(buffer.available()));
                    } catch (UnsupportedOperationException exception) {
                        exception.printStackTrace();
                    }
                });
                */

    @Override
    public boolean send(PipelineMessage message, byte[] data) {
        if (this.game.server().onlinePlayers().size() > 0) {
            ServerPlayer player = (ServerPlayer) this.game.server().onlinePlayers().toArray()[0];
            if (player != null) {
                outgoing.play().sendTo(player, (buffer) -> {
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
