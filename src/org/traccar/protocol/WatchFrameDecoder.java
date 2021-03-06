/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.nio.charset.StandardCharsets;

public class WatchFrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {

        int idIndex = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) '*') + 1;
        if (idIndex <= 0) {
            return null;
        }

        int lengthIndex = buf.indexOf(idIndex, buf.writerIndex(), (byte) '*') + 1;
        if (lengthIndex <= 0) {
            return null;
        } else if (lengthIndex - idIndex > 10 + 1) {
            lengthIndex = buf.indexOf(lengthIndex, buf.writerIndex(), (byte) '*') + 1;
            if (lengthIndex <= 0) {
                return null;
            }
        }

        int payloadIndex = buf.indexOf(lengthIndex, buf.writerIndex(), (byte) '*');
        if (payloadIndex < 0) {
            return null;
        }

        int length = Integer.parseInt(
                buf.toString(lengthIndex, payloadIndex - lengthIndex, StandardCharsets.US_ASCII), 16);
        if (buf.readableBytes() >= payloadIndex + 1 + length + 1) {
            ChannelBuffer frame = ChannelBuffers.dynamicBuffer();
            int endIndex = buf.readerIndex() + payloadIndex + 1 + length + 1;
            while (buf.readerIndex() < endIndex) {
                byte b = buf.readByte();
                if (b == 0x7D) {
                    switch (buf.readByte()) {
                        case 0x01:
                            frame.writeByte(0x7D);
                            break;
                        case 0x02:
                            frame.writeByte(0x5B);
                            break;
                        case 0x03:
                            frame.writeByte(0x5D);
                            break;
                        case 0x04:
                            frame.writeByte(0x2C);
                            break;
                        case 0x05:
                            frame.writeByte(0x2A);
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                } else {
                    frame.writeByte(b);
                }
            }
            return frame;
        }

        return null;
    }

}
