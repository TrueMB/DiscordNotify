package me.truemb.universal.minecraft.commands;

import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import me.truemb.discordnotify.commands.DN_DChatCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.kyori.adventure.text.Component;

public class VelocityCommandExecutor_DChat implements SimpleCommand {

	private DiscordNotifyMain instance;
	private DN_DChatCommand dchatCommand;

	public VelocityCommandExecutor_DChat(DiscordNotifyMain plugin) {
		this.instance = plugin;
		this.dchatCommand = new DN_DChatCommand(plugin);
	}

	@Override
	public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();
        
        if (!(sender instanceof Player)) {
			sender.sendMessage(Component.text(this.instance.getConfigManager().getMinecraftMessage("console", false)));
			return;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		
		this.dchatCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return;
	}
	
}
