package me.truemb.universal.minecraft.main;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import lombok.Getter;
import me.truemb.discordnotify.api.PlaceholderAPI;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import me.truemb.discordnotify.main.PluginDescription;
import me.truemb.universal.enums.ServerType;
import me.truemb.universal.messenger.IMessageChannel;
import me.truemb.universal.messenger.IRelay;
import me.truemb.universal.messenger.MessageChannelAPI;
import me.truemb.universal.messenger.MessageChannelCore;
import me.truemb.universal.messenger.MessageChannelException;
import me.truemb.universal.messenger.PipelineMessage;
import me.truemb.universal.minecraft.commands.BukkitCommandExecutor_DChat;
import me.truemb.universal.minecraft.commands.BukkitCommandExecutor_Staff;
import me.truemb.universal.minecraft.commands.BukkitCommandExecutor_Verify;
import me.truemb.universal.minecraft.events.BukkitEventsListener;
import me.truemb.universal.player.BukkitPlayer;
import me.truemb.universal.player.UniversalPlayer;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

public class BukkitMain extends JavaPlugin implements IRelay {
	
	private DiscordNotifyMain instance;
    private IMessageChannel core;

    @Getter
	private BukkitAudiences adventure;

	@Override
	public void onEnable() {
		this.instance = new DiscordNotifyMain(this.getDataFolder(), ServerType.BUKKIT, new PluginDescription(this.getDescription().getName(), this.getDescription().getAuthors().get(0), this.getDescription().getVersion()));
	    this.adventure = BukkitAudiences.create(this);
	    
	    this.setupPlaceholderAPI();
		
		//MESSAGING CHANNEL
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "messagechannel:proxy");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "messagechannel:server", new PluginMessageListener() {
            @Override
            public void onPluginMessageReceived(String channel, Player player, byte[] data) {
                core.getPipelineRegistry().receive(data);
            }
        });
		
		//LOAD PLAYERS
		Collection<UniversalPlayer> players = new ArrayList<>();
		for(Player all : Bukkit.getOnlinePlayers()) {
			players.add(new BukkitPlayer(all, this.getAdventure()));
		}
		this.instance.getUniversalServer().loadPlayers(players);
		
		//LOAD LISTENER
		BukkitEventsListener listener = new BukkitEventsListener(this.instance, this.getAdventure());
		this.getServer().getPluginManager().registerEvents(listener, this);
		
		//LOAD COMMANDS
		if(!this.instance.getUniversalServer().isProxySubServer()) {
			try{
			    Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			    commandMapField.setAccessible(true);
			    CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

				BukkitCommandExecutor_Staff staffCommand = new BukkitCommandExecutor_Staff(this.instance);
				commandMap.register("staff", staffCommand);
				List<String> staffAliases = new ArrayList<>();
				staffAliases.add("s");
				staffCommand.setAliases(staffAliases);
			    
				BukkitCommandExecutor_Verify verifyCommand = new BukkitCommandExecutor_Verify(this.instance);
				commandMap.register("verify", verifyCommand);
				
				if(this.instance.getConfigManager().getConfig().getBoolean("Options.Chat.enableSplittedChat")) {
					BukkitCommandExecutor_DChat dchatCommand = new BukkitCommandExecutor_DChat(this.instance);
					commandMap.register("dchat", dchatCommand);
				}
			    
			}catch(Exception exception){
			    exception.printStackTrace();
			}

			//METRICS ANALYTICS
			if(this.instance.getConfigManager().getConfig().getBoolean("Options.useMetrics"))
				new Metrics(this, DiscordNotifyMain.BSTATS_PLUGIN_ID);
			
		}else if(this.getServer().getPluginManager().getPlugin("Spicord") != null){
			this.instance.getUniversalServer().getLogger().severe("Network Sub-Server detected, but Spicord as well. Please install Spicord on your Bungeecord/Velocity Server.");
			return;
		}else {
			this.instance.getUniversalServer().getLogger().info("Network Sub-Server detected.");
		}
	}
	
	@Override
	public void onDisable() {
		if(this.instance != null)
			this.instance.onDisable();
		
	    if(this.adventure != null)
	        this.adventure.close();
	}

    @Override
    public void onLoad() {
        this.core = new MessageChannelCore(this);

        try {
            MessageChannelAPI.setCore(core);
        } catch (MessageChannelException exception) {
            exception.printStackTrace();
        }
    }
    
	private void setupPlaceholderAPI() {
		
		//PLUGIN WAS FOUND
	    if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
	        new PlaceholderAPI(this.instance).register();
			this.getLogger().info("PlacerHolderAPI was found and registered!");
	    }else {
			this.getLogger().info("PlacerHolderAPI was not found. (It's not needed, but supported)");
	    }
		
	}

    @Override
    public boolean send(PipelineMessage message, byte[] data) {
        if (getServer().getOnlinePlayers().size() > 0) {
            Player player = message.getTarget() != null ? getServer().getPlayer(message.getTarget()) : (Player) getServer().getOnlinePlayers().toArray()[0];
            if (player != null) {
                player.sendPluginMessage(this, "messagechannel:proxy", data);
                return true;
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
