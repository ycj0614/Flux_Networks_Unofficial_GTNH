package com.ycj0614.fluxnetworks.common.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.utils.Coord4D;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkServer;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.data.FluxChunkManager;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 연결 탭의 일괄 편집 (클라이언트 → 서버, ID 12).
 *
 * editions: 0 이름, 1 우선순위, 2 한도, 3 서지 모드, 4 한도 해제, 5 청크 로딩, 6 연결 해제,
 * 7 티어, 8 출력 암페어 (7·8은 원작에 없음)
 * tag: TileFluxCore.NBT_CUSTOM_NAME / NBT_PRIORITY / NBT_LIMIT / NBT_SURGE_MODE / NBT_DISABLE_LIMIT, "chunkLoad",
 * "tier", TileFluxPoint.NBT_OUTPUT_AMPERAGE
 */
public class PacketBatchEditing implements IMessageHandler<PacketBatchEditing.BatchEditingMessage, IMessage> {

    public static final int EDITION_COUNT = 9;
    /** 한 패킷으로 편집할 수 있는 최대 장치 수 */
    public static final int MAX_COORDS = 1024;
    public static final String CHUNK_LOAD = "chunkLoad";
    /** 플러그 최대 입력 티어·포인트 출력 티어에 함께 쓰는 값 */
    public static final String TIER = "tier";

    @Override
    public IMessage onMessage(BatchEditingMessage message, MessageContext ctx) {
        // 보안: 좌표가 1024개를 넘는 패킷은 무시 (fromBytes에서 rejected로 표시)
        if (message.rejected || message.tag == null) {
            return null;
        }
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        final int networkID = message.networkID;
        final boolean[] editions = message.editions.clone();
        final NBTTagCompound tag = message.tag;
        final List<Coord4D> coords = new ArrayList<>(message.coord4DS);
        // 원작은 권한 확인·연결 목록 조회를 네트워크 스레드에서 하고 SUCCESS를 먼저 반환한다. 여기서는 전부 작업 안에서 한다
        PacketHandler.handlePacket(() -> process(player, networkID, editions, tag, coords), ctx);
        return null;
    }

    private static void process(EntityPlayerMP player, int networkID, boolean[] editions, NBTTagCompound tag,
        List<Coord4D> coords) {
        IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
        if (network.isInvalid()) {
            return;
        }
        if (!network.getMemberPermission(player)
            .canEdit()) {
            PacketHandler.network.sendTo(new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.NO_ADMIN), player);
            return;
        }

        boolean editName = editions[0];
        boolean editPriority = editions[1];
        boolean editLimit = editions[2];
        boolean editSurge = editions[3];
        boolean editUnlimited = editions[4];
        boolean editChunkLoad = editions[5];
        boolean disconnect = editions[6];
        boolean editTier = editions[7];
        boolean editAmperage = editions[8];

        // 보안: 장치 이름은 최대 24자로 자른다
        String name = TileFluxCore.limitCustomName(tag.getString(TileFluxCore.NBT_CUSTOM_NAME));
        int priority = tag.getInteger(TileFluxCore.NBT_PRIORITY);
        long limit = tag.getLong(TileFluxCore.NBT_LIMIT);
        boolean surge = tag.getBoolean(TileFluxCore.NBT_SURGE_MODE);
        boolean unlimited = tag.getBoolean(TileFluxCore.NBT_DISABLE_LIMIT);
        boolean load = tag.getBoolean(CHUNK_LOAD);
        int tier = tag.getInteger(TIER); // 범위 보정은 setter가 한다
        long amperage = tag.getLong(TileFluxPoint.NBT_OUTPUT_AMPERAGE);
        boolean reject = false;

        // 연결 해제가 큐로 처리되므로 원본 목록을 직접 돌지 않고 복사본을 쓴다
        List<IFluxConnector> online = new ArrayList<>(network.<IFluxConnector>getConnections(FluxLogicType.ANY));
        boolean needSort = false;

        for (Coord4D c : coords) {
            IFluxConnector found = null;
            for (IFluxConnector f : online) {
                if (f.getCoords()
                    .equals(c)) {
                    found = f;
                    break;
                }
            }
            if (!(found instanceof TileFluxCore)) {
                continue;
            }
            TileFluxCore f = (TileFluxCore) found;
            if (disconnect) {
                FluxUtils.removeConnection(f, false);
                f.disconnect(network);
                continue;
            }
            if (editName) {
                f.customName = name;
            }
            if (editPriority) {
                f.priority = priority;
                needSort = true;
            }
            if (editLimit) {
                // 포인트는 출력 한도를 쓰지 않음 (배치 E·F). 원작대로 값만 저장한다
                // 원작과 다름: 음수는 0 (readPacket ID 3과 같게)
                f.limit = Math.min(Math.max(0L, limit), f.getMaxTransferLimit());
            }
            if (editSurge) {
                f.surgeMode = surge;
                needSort = true;
            }
            if (editUnlimited) {
                f.disableLimit = unlimited;
            }
            if (editChunkLoad) {
                if (FluxConfig.enableChunkLoading) {
                    if (load) {
                        if (f.getConnectionType()
                            .isStorage()) {
                            reject = true;
                        } else if (!f.chunkLoading && !FluxChunkManager.setChunkLoading(f, true)) {
                            reject = true;
                        }
                    } else {
                        FluxChunkManager.setChunkLoading(f, false);
                    }
                } else {
                    f.chunkLoading = false;
                }
            }
            // 원작에 없음: 티어는 플러그의 최대 입력 티어, 포인트의 출력 티어. 암페어는 포인트만. 반드시 setter로 바꾼다
            if (editTier) {
                if (f instanceof TileFluxPlug) {
                    ((TileFluxPlug) f).setMaxInputTier(tier);
                } else if (f instanceof TileFluxPoint) {
                    ((TileFluxPoint) f).setOutputTier(tier);
                }
            }
            if (editAmperage && f instanceof TileFluxPoint) {
                ((TileFluxPoint) f).setOutputAmperage(amperage);
            }
            f.sendPackets();
        }

        // 원작과 다름: 우선순위·서지 모드를 바꿨으면 개별 편집(readPacket)처럼 연결 정렬을 다시 한다
        if (needSort && network instanceof FluxNetworkServer) {
            ((FluxNetworkServer) network).sortConnections = true;
        }

        PacketHandler.network.sendTo(
            new PacketFeedback.FeedbackMessage(disconnect ? EnumFeedbackInfo.SUCCESS_2 : EnumFeedbackInfo.SUCCESS),
            player);
        // 원작은 청크 로딩을 못 건 장치가 있으면 SUCCESS 뒤에 REJECT_SOME을 보낸다
        if (reject) {
            PacketHandler.network.sendTo(new PacketFeedback.FeedbackMessage(EnumFeedbackInfo.REJECT_SOME), player);
        }
    }

    public static class BatchEditingMessage implements IMessage {

        public int networkID;
        public List<Coord4D> coord4DS = new ArrayList<>();
        public NBTTagCompound tag;
        public boolean[] editions = new boolean[EDITION_COUNT];
        /** 받은 패킷이 형식 검사를 통과하지 못함 (받는 쪽 전용, 직렬화하지 않음) */
        public boolean rejected = false;

        public BatchEditingMessage() {}

        /** coord4DS는 MAX_COORDS(1024)개 이하로 보낸다. 넘으면 서버가 패킷 전체를 무시한다 */
        public BatchEditingMessage(int networkID, List<Coord4D> coord4DS, NBTTagCompound tag, boolean[] editions) {
            this.networkID = networkID;
            this.coord4DS = coord4DS;
            this.tag = tag;
            this.editions = editions;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            networkID = buf.readInt();
            for (int i = 0; i < EDITION_COUNT; i++) {
                editions[i] = buf.readBoolean();
            }
            tag = ByteBufUtils.readTag(buf);
            int size = buf.readInt();
            // 보안: 좌표가 1024개를 넘거나(음수 포함) 남은 바이트가 모자라면 좌표를 읽지 않고 패킷 전체를 무시
            if (size < 0 || size > MAX_COORDS || buf.readableBytes() < size * 16) {
                rejected = true;
                return;
            }
            for (int i = 0; i < size; i++) {
                coord4DS.add(readCoord(buf));
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(networkID);
            for (int i = 0; i < EDITION_COUNT; i++) {
                buf.writeBoolean(editions != null && i < editions.length && editions[i]);
            }
            ByteBufUtils.writeTag(buf, tag);
            buf.writeInt(coord4DS.size());
            for (Coord4D c : coord4DS) {
                writeCoord(buf, c);
            }
        }

        /** 원작 Coord4D(ByteBuf)와 같은 순서: x, y, z, dimension */
        private static Coord4D readCoord(ByteBuf buf) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("x", buf.readInt());
            t.setInteger("y", buf.readInt());
            t.setInteger("z", buf.readInt());
            t.setInteger("dimension", buf.readInt());
            return new Coord4D(t);
        }

        /** 원작 Coord4D.write(ByteBuf)와 같은 순서 */
        private static void writeCoord(ByteBuf buf, Coord4D c) {
            buf.writeInt(c.getX());
            buf.writeInt(c.getY());
            buf.writeInt(c.getZ());
            buf.writeInt(c.getDimension());
        }
    }
}
