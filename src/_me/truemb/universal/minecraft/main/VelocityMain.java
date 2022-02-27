package _me.truemb.universal.minecraft.main;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import _me.truemb.universal.enums.ServerType;
import _me.truemb.universal.messenger.IMessageChannel;
import _me.truemb.universal.messenger.IRelay;
import _me.truemb.universal.messenger.MessageChannelAPI;
import _me.truemb.universal.messenger.MessageChannelCore;
import _me.truemb.universal.messenger.MessageChannelException;
import _me.truemb.universal.messenger.PipelineMessage;
import _me.truemb.universal.minecraft.commands.VelocityCommandExecutor_DChat;
import _me.truemb.universal.minecraft.commands.VelocityCommandExecutor_Staff;
import _me.truemb.universal.minecraft.commands.VelocityCommandExecutor_Verify;
import _me.truemb.universal.minecraft.events.VelocityEventsListener;
import _me.truemb.universal.player.UniversalPlayer;
import _me.truemb.universal.player.VelocityPlayer;
import _me.truemb.universal.server.VelocityServer;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;

@Plugin(id = "${project.artifactId}", name = "${project.name}", version = "${project.version}", authors = {"TrueMB"})
public class VelocityMain implements IRelay {
	
	private DiscordNotifyMain instance;
	
    private IMessageChannel core;
    private ProxyServer proxy;

    private MinecraftChannelIdentifier INCOMING;
    private MinecraftChannelIdentifier OUTGOING;

    @Inject
    public VelocityMain(ProxyServer server, Logger logger) {

        this.core = new MessageChannelCore(this);

        try {
            MessageChannelAPI.setCore(core);
        } catch (MessageChannelException exception) {
            exception.printStackTrace();
        }
	}
	
    @Subscribe
    public void onDisable(ProxyShutdownEvent e) {
    	if(this.instance != null)
    		this.instance.onDisable();
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent e, @DataDirectory Path dataDirectory) {
		this.instance = new DiscordNotifyMain(dataDirectory.toFile(), ServerType.VELOCITY);

		//SET VELOCITY INSTANCE
		VelocityServer velocityServer = (VelocityServer) this.instance.getUniversalServer();
		velocityServer.setInstance(this.proxy);
		
		//MESSAGING CHANNEL
        this.proxy.getChannelRegistrar().register(INCOMING = MinecraftChannelIdentifier.create("messagechannel", "proxy"));
        this.proxy.getChannelRegistrar().register(OUTGOING = MinecraftChannelIdentifier.create("messagechannel", "server"));

        this.proxy.getEventManager().register(this, this);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(Player all : this.proxy.getAllPlayers()) {
			UniversalPlayer up = new VelocityPlayer(all);
			players.add(up);

			up.setServer(all.getCurrentServer().get().getServerInfo().getName());
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		VelocityEventsListener listener = new VelocityEventsListener(this.instance);
		this.proxy.getEventManager().register(this, listener);
		this.proxy.getEventManager().register(this, this);
		
		//LOAD COMMANDS
		if(this.instance.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat")) {
			VelocityCommandExecutor_DChat dchatCommand = new VelocityCommandExecutor_DChat(this.instance);
			CommandMeta dchatMeta = this.proxy.getCommandManager().metaBuilder("dchat").build();
			
			this.proxy.getCommandManager().register(dchatMeta, dchatCommand);
		}
		
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			VelocityCommandExecutor_Staff staffCommand = new VelocityCommandExecutor_Staff(this.instance);
			CommandMeta staffMeta = this.proxy.getCommandManager().metaBuilder("staff").aliases("s").build();
			
			this.proxy.getCommandManager().register(staffMeta, staffCommand);
		}
		
		VelocityCommandExecutor_Verify verifyCommand = new VelocityCommandExecutor_Verify(this.instance);
		CommandMeta verifyMeta = this.proxy.getCommandManager().metaBuilder("dchat").build();
		
		this.proxy.getCommandManager().register(verifyMeta, verifyCommand);
    }

    @Override
    public boolean send(PipelineMessage message, byte[] data) {
        Optional<Player> player = this.proxy.getPlayer(message.getTarget());
        if (player.isPresent()) {
            Optional<ServerConnection> server = player.get().getCurrentServer();
            if(message.getTargetServer() != null){
            	this.proxy.getServer(message.getTargetServer()).get().sendPluginMessage(this.OUTGOING, data);
            }else if (server.isPresent()) {
                return server.get().sendPluginMessage(this.OUTGOING, data);
            } 
        }
        return false;
    }

    @Override
    public boolean broadcast(PipelineMessage message, byte[] data) {
        for (RegisteredServer subserver : this.proxy.getAllServers()) {
            subserver.sendPluginMessage(OUTGOING, data);
        }
        return true;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier().equals(this.INCOMING)) {
            this.core.getPipelineRegistry().receive(event.getData());

            event.setResult(ForwardResult.handled());
        }
    }

    @Override
    public boolean isProxy() {
        return true;
    }

}
