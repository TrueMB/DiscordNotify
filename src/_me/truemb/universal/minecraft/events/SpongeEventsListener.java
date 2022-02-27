package _me.truemb.universal.minecraft.events;

import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import _me.truemb.universal.player.SpongePlayer;
import _me.truemb.universal.player.UniversalPlayer;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class SpongeEventsListener {
	
	//Sponge.getEventManager().registerListeners(this, new ExampleListener());
	//https://docs.spongepowered.org/stable/en/plugin/event/listeners.html
	
	private DiscordNotifyMain plugin;
	
	public SpongeEventsListener(DiscordNotifyMain plugin) {
		this.plugin = plugin;
	}

	@Listener
	public void onChat(MessageChannelEvent.Chat e, @First Player p) {
		
		if(e.isCancelled())
			return;
		
		if(e.isMessageCancelled())
			return;

		UUID uuid = p.getUniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String message = e.getMessage().toPlain();

		if(message.startsWith("/"))
			return;
		
		this.plugin.getListener().onPlayerMessage(up, message);
	}

	@Listener
	public void onConnect(ClientConnectionEvent.Join e) {

		Player p = e.getTargetEntity();
		
		UniversalPlayer up = new SpongePlayer(p);
		this.plugin.getUniversalServer().addPlayer(up);
		
		this.plugin.getListener().onPlayerJoin(up, null);
	}
	
	@Listener
	public void onDisconnect(ClientConnectionEvent.Disconnect e) {

		Player p = e.getTargetEntity();
		UUID uuid = p.getUniqueId();
		
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		this.plugin.getUniversalServer().removePlayer(up);
		
		this.plugin.getListener().onPlayerQuit(up, null);
	}

	@Listener
	public void onDeath(DestructEntityEvent.Death e) {

		if(!(e.getTargetEntity() instanceof Player))
			return;
		
		Player p = (Player) e.getTargetEntity();
		UUID uuid = p.getUniqueId();
		UniversalPlayer up = this.plugin.getUniversalServer().getPlayer(uuid);
		String deathMessage = e.getMessage().toPlain();
		
		this.plugin.getListener().onPlayerDeath(up, deathMessage);
	}
	
}
