package me.truemb.universal.messenger;

public class SyncPipeline extends AbstractPipeline {

    public SyncPipeline(MessageChannelCore core, PipelineRegistryImpl registry, String channel) {
        super(core, registry, channel);
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
