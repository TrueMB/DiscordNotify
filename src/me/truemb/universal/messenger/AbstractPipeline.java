package me.truemb.universal.messenger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractPipeline implements IPipeline {

    protected MessageChannelCore core;
    protected PipelineRegistryImpl registry;
    protected String channel;
    protected Set<Consumer<PipelineMessage>> listeners;

    public AbstractPipeline(MessageChannelCore core, PipelineRegistryImpl registry, String channel) {
        this.core = core;
        this.registry = registry;
        this.channel = channel;
        this.listeners = new HashSet<Consumer<PipelineMessage>>();
    }

    public void onReceive(Consumer<PipelineMessage> listener) {
        if (!this.listeners.contains(listener)) {
            synchronized (listeners) {
                if (!this.listeners.contains(listener)) {
                    this.listeners.add(listener);
                }
            }
        }
    }

    public void send(PipelineMessage message) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(bytes);

            output.writeUTF(channel);
            output.writeObject(message.getTarget());
            output.writeObject(message.getContents());

            output.close();
            bytes.close();

            core.getRelay().send(message, bytes.toByteArray());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void post(PipelineMessage message) {
        synchronized (listeners) {
            listeners.forEach(listener -> listener.accept(message));
        }
    }

    @Override
    public void broadcast(PipelineMessage message) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(bytes);

            output.writeUTF(channel);
            output.writeObject(message.getTarget());
            output.writeObject(message.getContents());

            output.close();
            bytes.close();

            core.getRelay().broadcast(message, bytes.toByteArray());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
