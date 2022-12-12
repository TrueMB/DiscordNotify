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
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import com.google.inject.Inject;

import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.main.PluginDescription;
import me.truemb.universal.enums.ServerType;
import me.truemb.universal.messenger.IMessageChannel;
import me.truemb.universal.messenger.IRelay;
import me.truemb.universal.messenger.MessageChannelAPI;
import me.truemb.universal.messenger.MessageChannelCore;
import me.truemb.universal.messenger.MessageChannelException;
import me.truemb.universal.messenger.PipelineMessage;
import me.truemb.universal.minecraft.commands.SpongeCommandExecutor_DChat;
import me.truemb.universal.minecraft.commands.SpongeCommandExecutor_Staff;
import me.truemb.universal.minecraft.commands.SpongeCommandExecutor_Verify;
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
    
    @Inject
    private PluginContainer pluginContainer;
    private IMessageChannel core;
    private RawDataChannel outgoing;
    
    //COMMANDS
    private SpongeCommandExecutor_DChat dchatCommand = new SpongeCommandExecutor_DChat();
    private SpongeCommandExecutor_Verify verifyCommand = new SpongeCommandExecutor_Verify();
    private SpongeCommandExecutor_Staff staffCommand = new SpongeCommandExecutor_Staff();
    
    @Listener
    public void onConstructPlugin(ConstructPluginEvent e) {
    	//PLUGIN CHANNEL
        this.core = new MessageChannelCore(this);
        try {
            MessageChannelAPI.setCore(core);
        } catch (MessageChannelException exception) {
            exception.printStackTrace();
        }
    }
    
    @Listener
    public void onServerStart(LoadedGameEvent e) {
    	Game game = e.game();
		this.instance = new DiscordNotifyMain(this.configDir.toFile(), ServerType.SPONGE, new PluginDescription(null, null, null));
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(ServerPlayer all : game.server().onlinePlayers())
			players.add(new SpongePlayer(all));
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		SpongeEventsListener listener = new SpongeEventsListener(this.instance);
    	game.eventManager().registerListeners(this.pluginContainer, listener);
    	
    	//FINISH COMMAND LOADING
		if(!this.instance.getUniversalServer().isProxySubServer()) {
	    	this.dchatCommand.setup(this.instance);
	    	this.verifyCommand.setup(this.instance);
	    	this.staffCommand.setup(this.instance);
		}
	}

    @Listener
    public void onDisable(StoppedGameEvent e) {
    	if(this.instance != null)
    		this.instance.onDisable();
    }

    @Listener
    public void onRegisterCommand(RegisterCommandEvent<Command.Raw> e) {
    	
		//LOAD COMMANDS
		if(!this.instance.getUniversalServer().isProxySubServer()) {
	    	e.register(this.pluginContainer, this.dchatCommand, "dchat");
	    	e.register(this.pluginContainer, this.verifyCommand, "verify");
	    	e.register(this.pluginContainer, this.staffCommand, "staff", "s");
		}
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
