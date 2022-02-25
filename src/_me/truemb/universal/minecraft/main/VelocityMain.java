package _me.truemb.universal.minecraft.main;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import _me.truemb.universal.enums.ServerType;
import _me.truemb.universal.minecraft.commands.VelocityCommandExecutor_DChat;
import _me.truemb.universal.minecraft.commands.VelocityCommandExecutor_Staff;
import _me.truemb.universal.minecraft.commands.VelocityCommandExecutor_Verify;
import _me.truemb.universal.minecraft.events.VelocityEventsListener;
import _me.truemb.universal.player.UniversalPlayer;
import _me.truemb.universal.server.VelocityServer;
import me.truemb.discordnotify.enums.FeatureType;
import me.truemb.discordnotify.main.DiscordNotifyMain;

@Plugin(id = "discordnotify", name = "DiscordNotify", version = "3.0.0", authors = {"TrueMB"})
public class VelocityMain {
	
	private DiscordNotifyMain instance;

    @Inject
    public VelocityMain(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		this.instance = new DiscordNotifyMain(dataDirectory.toFile(), ServerType.VELOCITY);
		
		//SET VELOCITY INSTANCE
		VelocityServer velocityServer = (VelocityServer) this.instance.getUniversalServer();
		velocityServer.setInstance(server);
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(Player all : server.getAllPlayers()) {
			UUID uuid = all.getUniqueId();
			String name = all.getUsername();
			
			UniversalPlayer up = new UniversalPlayer(uuid, name);
			players.add(up);

			up.setServer(all.getCurrentServer().get().getServerInfo().getName());
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		VelocityEventsListener listener = new VelocityEventsListener(this.instance);
		server.getEventManager().register(this, listener);
        server.getEventManager().register(this, this);
		
		//LOAD COMMANDS
		if(this.instance.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat")) {
			VelocityCommandExecutor_DChat dchatCommand = new VelocityCommandExecutor_DChat(this.instance);
			CommandMeta dchatMeta = server.getCommandManager().metaBuilder("dchat").build();
			
			server.getCommandManager().register(dchatMeta, dchatCommand);
		}
		
		if(this.instance.getConfigManager().isFeatureEnabled(FeatureType.Staff)) {
			VelocityCommandExecutor_Staff staffCommand = new VelocityCommandExecutor_Staff(this.instance);
			CommandMeta staffMeta = server.getCommandManager().metaBuilder("staff").aliases("s").build();
			
			server.getCommandManager().register(staffMeta, staffCommand);
		}
		
		VelocityCommandExecutor_Verify verifyCommand = new VelocityCommandExecutor_Verify(this.instance);
		CommandMeta verifyMeta = server.getCommandManager().metaBuilder("dchat").build();
		
		server.getCommandManager().register(verifyMeta, verifyCommand);
        
	}
	
    @Subscribe
    public void onDisable(ProxyShutdownEvent e) {
    	this.instance.onDisable();
    }

}
