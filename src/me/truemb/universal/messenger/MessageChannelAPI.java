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

import lombok.Getter;

public class MessageChannelAPI {

    @Getter
    private static IMessageChannel core;
    private static final Object LOCK = new Object();
    
    public static void setCore(IMessageChannel core) throws MessageChannelException {
        if (MessageChannelAPI.core == null) {
            synchronized (LOCK) {
                if (MessageChannelAPI.core == null) {
                    MessageChannelAPI.core = core;
                    return;
                }
            }
        }
        throw new MessageChannelException(
                "The MessageChannelCore singleton instance has already been defined by another plugin!");
    }

    public static IPipelineRegistry getPipelineRegistry() {
        return core.getPipelineRegistry();
    }
}