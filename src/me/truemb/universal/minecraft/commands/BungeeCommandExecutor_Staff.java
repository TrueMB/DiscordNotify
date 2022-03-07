package me.truemb.universal.minecraft.commands;

import java.util.UUID;

import me.truemb.discordnotify.commands.DN_StaffCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BungeeCommandExecutor_Staff extends Command {

	private DiscordNotifyMain instance;
	private DN_StaffCommand staffCommand;

	public BungeeCommandExecutor_Staff(DiscordNotifyMain plugin) {
		super("staff", plugin.getConfigManager().getConfig().getString("Permissions.StaffChat"), "s");
		this.instance = plugin;
		this.staffCommand = new DN_StaffCommand(plugin);
	}
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		
		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(new TextComponent(this.instance.getConfigManager().getMinecraftMessage("console", false)));
			return;
		}

		ProxiedPlayer p = (ProxiedPlayer) sender;
		UUID uuid = p.getUniqueId();
		
		this.staffCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return;
	}
	
}
