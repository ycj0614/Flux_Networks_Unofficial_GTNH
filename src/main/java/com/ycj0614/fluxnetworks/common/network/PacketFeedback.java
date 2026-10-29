package com.ycj0614.fluxnetworks.common.network;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server → Client: GUI 피드백 (명령 결과)
 */
public class PacketFeedback implements IMessageHandler<PacketFeedback.FeedbackMessage, IMessage> {

    @Override
    public IMessage onMessage(FeedbackMessage message, MessageContext ctx) {
        final EnumFeedbackInfo info = message.info;
        if (info == null) {
            return null;
        }
        PacketHandler.handlePacket(() -> {
            if (info == EnumFeedbackInfo.SUCCESS || info == EnumFeedbackInfo.SUCCESS_2
                || info == EnumFeedbackInfo.PASSWORD_REQUIRE) {
                FluxNetworks.proxy.setFeedback(info, true);
            } else {
                FluxNetworks.proxy.setFeedback(info, false);
            }
        }, ctx);
        return null;
    }

    public static class FeedbackMessage implements IMessage {

        /** 잘못된 ordinal을 받으면 null (핸들러가 무시) */
        public EnumFeedbackInfo info;

        public FeedbackMessage() {}

        public FeedbackMessage(EnumFeedbackInfo info) {
            this.info = info;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            int index = buf.readInt();
            EnumFeedbackInfo[] values = EnumFeedbackInfo.values();
            info = index >= 0 && index < values.length ? values[index] : null;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(info.ordinal());
        }
    }
}
