package me.truemb.universal.minecraft.commands;

import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import me.truemb.discordnotify.commands.DN_StaffCommand;
import me.truemb.discordnotify.main.DiscordNotifyMain;
import net.kyori.adventure.text.Component;

public class VelocityCommandExecutor_Staff implements SimpleCommand {

	private DiscordNotifyMain instance;
	private DN_StaffCommand staffCommand;

	public VelocityCommandExecutor_Staff(DiscordNotifyMain plugin) {
		this.instance = plugin;
		this.staffCommand = new DN_StaffCommand(plugin);
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
		
		this.staffCommand.onCommand(this.instance.getUniversalServer().getPlayer(uuid), args);
		return;
	}
	
    @Override
    public boolean hasPermission(final Invocation invocation) {
    	return true;
       // return invocation.source().hasPermission(this.instance.getConfigManager().getConfig().getString("Permissions.StaffChat"));    
    }
	
}
