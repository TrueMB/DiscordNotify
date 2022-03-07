package me.truemb.universal.minecraft.commands;

import java.util.UUID;

import me.truemb.discordnotify.commands.DN_DChatCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BungeeCommandExecutor_DChat extends Command {

	private DiscordNotifyMain instance;
	private DN_DChatCommand dchatCommand;

	public BungeeCommandExecutor_DChat(DiscordNotifyMain plugin) {
		super("dchat");
		this.instance = plugin;
		this.dchatCommand = new DN_DChatCommand(plugin);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		
		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new TextComponent(this.instance.getConfigManager().getMinecraftMessage("console", false)));
			return;
		}

		ProxiedPlayer p = (ProxiedPlayer) sender;
		UUID uuid = p.getUniqueId();
		
		this.dchatCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return;
	}
	
}
