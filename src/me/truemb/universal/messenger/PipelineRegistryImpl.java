/*
 * This file is part of MessageChannel, licensed under the MIT License (MIT).
 *
 * Copyright (c) Crypnotic <https://www.crypnotic.me>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.truemb.universal.messenger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.SneakyThrows;
import me.truemb.discordnotify.main.DiscordNotifyMain;

public class PipelineRegistryImpl implements IPipelineRegistry {

    private final MessageChannelCore core;
    private final Map<String, IPipeline> pipelines;

    public PipelineRegistryImpl(MessageChannelCore core) {
        this.core = core;
        this.pipelines = new HashMap<String, IPipeline>();
    }

    @SneakyThrows(MessageChannelException.class)
    public IPipeline register(String channel) {
        if (!pipelines.containsKey(channel)) {
            synchronized (pipelines) {
                if (!pipelines.containsKey(channel)) {
                    IPipeline pipeline = new SyncPipeline(core, this, channel);
                    pipelines.put(channel, pipeline);

                    return pipeline;
                }
            }
        }
        throw new MessageChannelException("Channel (" + channel + ") already has a registered pipeline!");
    }

    @SneakyThrows(MessageChannelException.class)
    public IPipeline registerAsync(DiscordNotifyMain plugin, String channel) {
        if (!pipelines.containsKey(channel)) {
            synchronized (pipelines) {
                if (!pipelines.containsKey(channel)) {
                    IPipeline pipeline = new AsyncPipeline(plugin, core, this, channel);
                    pipelines.put(channel, pipeline);

                    return pipeline;
                }
            }
        }
        throw new MessageChannelException("Channel (" + channel + ") already has a registered pipeline!");
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows(MessageChannelException.class)
    public final void receive(byte[] data) {
        try {
            ByteArrayInputStream bytes = new ByteArrayInputStream(data);
            ObjectInputStream input = new ObjectInputStream(bytes);

            String channel = input.readUTF();
            UUID target = (UUID) input.readObject();
            List<Object> content = (List<Object>) input.readObject();
            
            input.close();
            bytes.close();

            PipelineMessage message = new PipelineMessage(target, content);

            synchronized (pipelines) {
                if (pipelines.containsKey(channel)) {
                    pipelines.get(channel).post(message);
                }
            }
        } catch (IOException | ClassNotFoundException exception) {
            throw new MessageChannelException("Failed to deserialize message: " + exception.getMessage());
        }
    }

    public IPipeline getRegisteredPipeline(String channel) {
        return isRegisteredPipeline(channel) ? pipelines.get(channel) : null;
    }

    public boolean isRegisteredPipeline(String channel) {
        return pipelines.containsKey(channel);
    }
}