package com.ycj0614.fluxnetworks.common.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.common.network.simpleimpl.IMessage;

public class PacketTileHandler {

    private static final String CHUNK_LOADING = "c";

    public static NBTTagCompound getSetNetworkPacket(int id, String password) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(FluxNetworkData.NETWORK_ID, id);
        tag.setString(FluxNetworkData.NETWORK_PASSWORD, password);
        return tag;
    }

    public static IMessage handleSetNetworkPacket(TileFluxCore tile, EntityPlayer player, NBTTagCompound tag) {
        int id = tag.getInteger(FluxNetworkData.NETWORK_ID);
        String pass = tag.getString(FluxNetworkData.NETWORK_PASSWORD);
        if (tile.getNetworkID() == id) {
            return null;
        }
        IFluxNetwork network = FluxNetworkCache.instance.getNetwork(id);
        if (!network.isInvalid()) {
            if (tile.getConnectionType()
                .isController()
                && network.getConnections(FluxLogicType.CONTROLLER)
                    .size() > 0) {
                return new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.HAS_CONTROLLER);
            }
            if (!network.getMemberPermission(player)
                .canAccess()) {
                if (pass.isEmpty()) {
                    return new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.PASSWORD_REQUIRE);
                }
                // 보안: 비밀번호는 최대 16자. 저장된 비밀번호도 16자 이하이므로 더 긴 입력은 비교 없이 거부
                if (pass.length() > PacketGeneralHandler.MAX_PASSWORD_LENGTH
                    || !pass.equals(network.getSetting(NetworkSettings.NETWORK_PASSWORD))) {
                    return new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.REJECT);
                }
            }
            if (tile.getNetwork() != null && !tile.getNetwork()
                .isInvalid()) {
                tile.getNetwork()
                    .queueConnectionRemoval(tile, false);
            }
            tile.playerUUID = player.getGameProfile()
                .getId();
            network.queueConnectionAddition(tile);
            return new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.SUCCESS);
        }
        return null;
    }

    public static NBTTagCompound getChunkLoadPacket(boolean chunkLoading) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(CHUNK_LOADING, chunkLoading);
        return tag;
    }

    public static IMessage handleChunkLoadPacket(TileFluxCore tile, EntityPlayer player, NBTTagCompound tag) {
        // TODO 3단계: FluxChunkManager.forceChunk/releaseChunk (원작 로직)
        // boolean load = tag.getBoolean(CHUNK_LOADING);
        // FluxConfig.enableChunkLoading이면
        // load: tile.chunkLoading = forceChunk(world, 타일 청크); 실패면 HAS_LOADER 반환, 성공이면 null
        // !load: releaseChunk(world, 타일 청크); tile.chunkLoading = false; null 반환
        // 아니면 아래와 같이 chunkLoading = false, BANNED_LOADING
        tile.chunkLoading = false;
        return new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.BANNED_LOADING);
    }
}
