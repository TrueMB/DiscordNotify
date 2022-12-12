package me.truemb.universal.messenger;

import me.truemb.discordnotify.main.DiscordNotifyMain;

public class AsyncPipeline extends AbstractPipeline {

	private DiscordNotifyMain instance;
	
    public AsyncPipeline(DiscordNotifyMain plugin, MessageChannelCore core, PipelineRegistryImpl registry, String channel) {
        super(core, registry, channel);
        this.instance = plugin;
    }

    @Override
    public void send(PipelineMessage message) {
    	this.instance.getExecutor().submit(() -> super.send(message));
    }

    @Override
    public final void post(PipelineMessage message) {
    	this.instance.getExecutor().submit(() -> super.post(message));
    }

    @Override
    public void broadcast(PipelineMessage message) {
    	this.instance.getExecutor().submit(() -> super.broadcast(message));
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
