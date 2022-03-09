package me.truemb.universal.minecraft.commands;

import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import me.truemb.discordnotify.commands.DN_DChatCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class SpongeCommandExecutor_DChat implements CommandExecutor {

	private DiscordNotifyMain instance;
	private DN_DChatCommand dchatCommand;

	public SpongeCommandExecutor_DChat(DiscordNotifyMain plugin) {
		this.instance = plugin;
		this.dchatCommand = new DN_DChatCommand(plugin);
	}
	
	@Override
	public CommandResult execute(CommandContext context) throws CommandException {
		
		/*
		if (!(context. instanceof Player)) {
			sender.sendMessage(Text.of(this.instance.getConfigManager().getMinecraftMessage("console", false)));
			return CommandResult.success();
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		*/
		//TODO this.dchatCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args.getAll(""));
		return CommandResult.success();
	}
	
	
	
}
