package me.truemb.discordnotify.runnable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.spicord.bot.DiscordBot.BotStatus;

import me.truemb.discordnotify.main.DiscordNotifyMain;

public class DN_DiscordBotConnector implements Runnable {
	
	private DiscordNotifyMain instance;
	
	private ScheduledFuture<?> task;
	
	public DN_DiscordBotConnector(DiscordNotifyMain plugin) {
		this.instance = plugin;
		
		this.task = plugin.getExecutor().scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		
		//TRY CONNECTING
		this.instance.getDiscordManager().prepareDiscordBot();
		
		//IF FAILED, THEN RETRY IN ONE SECOND
		if(this.instance.getDiscordManager().getDiscordBot() == null)
			return;
		
		if(this.instance.getDiscordManager().isDiscordBotHooked()) //TASK SUCCESSFUL DONE
			this.task.cancel(true);
		else if(this.instance.getDiscordManager().getDiscordBot().getStatus() == BotStatus.OFFLINE) { //BOT OFFLINE
			this.task.cancel(true);
			this.instance.getUniversalServer().getLogger().warning("Couldn't connect to the Bot. Is the Bot Offline?");
		}
	}

}
