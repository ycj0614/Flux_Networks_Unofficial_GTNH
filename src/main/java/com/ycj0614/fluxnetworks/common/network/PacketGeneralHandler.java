package com.ycj0614.fluxnetworks.common.network;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.google.common.collect.Lists;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkMember;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.network.SecurityType;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;

/**
 * PacketGeneral의 종류별 처리. handle* 메서드는 모두 서버 메인 스레드(작업 큐)에서 호출된다.
 */
public class PacketGeneralHandler {

    /** 네트워크 이름 최대 길이 (원작 GUI 입력칸 setMaxStringLength(24)) */
    public static final int MAX_NETWORK_NAME_LENGTH = 24;
    /** 비밀번호 최대 길이 (원작 GUI 입력칸 setMaxStringLength(16)) */
    public static final int MAX_PASSWORD_LENGTH = 16;

    /** CHANGE_PERMISSION의 type 값 */
    public static final int PERMISSION_ADD_USER = 0;
    public static final int PERMISSION_SET_ADMIN = 1;
    public static final int PERMISSION_SET_USER = 2;
    public static final int PERMISSION_REMOVE = 3;
    public static final int PERMISSION_TRANSFER_OWNERSHIP = 4;

    private static final String PLAYER_NAME = "playerName";
    private static final String PLAYER_CHANGED = "playerChanged";
    private static final String PLAYER_REMOVED = "playerRemoved";
    private static final String PERMISSION_TYPE = "t";

    // ---------------------------------------------------------------- 생성

    public static NBTTagCompound getCreateNetworkPacket(String name, int color, SecurityType security,
        String password) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(FluxNetworkData.NETWORK_NAME, name);
        tag.setInteger(FluxNetworkData.NETWORK_COLOR, color);
        tag.setInteger(FluxNetworkData.SECURITY_TYPE, security.ordinal());
        tag.setString(FluxNetworkData.NETWORK_PASSWORD, password);
        return tag;
    }

    public static IMessage handleCreateNetworkPacket(EntityPlayer player, NBTTagCompound nbtTag) {
        // 보안: 네트워크 이름은 최대 24자로 자른다
        String name = truncate(nbtTag.getString(FluxNetworkData.NETWORK_NAME), MAX_NETWORK_NAME_LENGTH);
        int color = nbtTag.getInteger(FluxNetworkData.NETWORK_COLOR);
        SecurityType security = readSecurityType(nbtTag);
        // 보안: 비밀번호는 최대 16자로 자른다
        String password = truncate(nbtTag.getString(FluxNetworkData.NETWORK_PASSWORD), MAX_PASSWORD_LENGTH);
        if (security == null) {
            return null; // 패킷 규칙 4
        }
        if (!FluxUtils.checkPassword(password)) {
            return feedback(EnumFeedbackInfo.ILLEGAL_PASSWORD);
        }
        if (FluxNetworkCache.instance.hasSpaceLeft(player)) {
            FluxNetworkCache.instance.createdNetwork(player, name, color, security, password);
            return feedback(EnumFeedbackInfo.SUCCESS);
        }
        return feedback(EnumFeedbackInfo.NO_SPACE);
    }

    // ---------------------------------------------------------------- 편집

    public static NBTTagCompound getNetworkEditPacket(int networkID, String name, int color, SecurityType security,
        String password) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
        tag.setString(FluxNetworkData.NETWORK_NAME, name);
        tag.setInteger(FluxNetworkData.NETWORK_COLOR, color);
        tag.setInteger(FluxNetworkData.SECURITY_TYPE, security.ordinal());
        tag.setString(FluxNetworkData.NETWORK_PASSWORD, password);
        return tag;
    }

    public static IMessage handleNetworkEditPacket(EntityPlayer player, NBTTagCompound tag) {
        int networkID = tag.getInteger(FluxNetworkData.NETWORK_ID);
        // 보안: 네트워크 이름은 최대 24자로 자른다
        String newName = truncate(tag.getString(FluxNetworkData.NETWORK_NAME), MAX_NETWORK_NAME_LENGTH);
        int color = tag.getInteger(FluxNetworkData.NETWORK_COLOR);
        SecurityType security = readSecurityType(tag);
        // 보안: 비밀번호는 최대 16자로 자른다
        String password = truncate(tag.getString(FluxNetworkData.NETWORK_PASSWORD), MAX_PASSWORD_LENGTH);
        if (security == null) {
            return null; // 패킷 규칙 4
        }
        if (!FluxUtils.checkPassword(password)) {
            return feedback(EnumFeedbackInfo.ILLEGAL_PASSWORD);
        }
        final IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
        if (network.isInvalid()) {
            return null;
        }
        if (!network.getMemberPermission(player)
            .canEdit()) {
            return feedback(EnumFeedbackInfo.NO_ADMIN);
        }
        boolean needPacket = false;
        if (!newName.equals(network.getSetting(NetworkSettings.NETWORK_NAME))) {
            network.setSetting(NetworkSettings.NETWORK_NAME, newName);
            needPacket = true;
        }
        int oldColor = network.getSetting(NetworkSettings.NETWORK_COLOR);
        if (oldColor != color) {
            network.setSetting(NetworkSettings.NETWORK_COLOR, color);
            needPacket = true;
            List<IFluxConnector> list = network.getConnections(FluxLogicType.ANY);
            list.forEach(fluxConnector -> fluxConnector.connect(network)); // 장치의 색 데이터 갱신
        }
        if (needPacket) {
            // 이름·색이 바뀌면 모든 클라이언트의 색상 캐시(아이템 툴팁 등)를 갱신한다
            PacketHandler.network.sendToAll(PacketColorCache.ColorCacheMessage.of(network));
        }
        network.setSetting(NetworkSettings.NETWORK_SECURITY, security);
        network.setSetting(NetworkSettings.NETWORK_PASSWORD, password);
        PacketHandler.network.sendToAll(
            new PacketNetworkUpdate.NetworkUpdateMessage(Lists.newArrayList(network), NBTType.NETWORK_GENERAL));
        return feedback(EnumFeedbackInfo.SUCCESS_2);
    }

    // ---------------------------------------------------------------- 삭제

    public static NBTTagCompound getDeleteNetworkPacket(int networkID) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
        return tag;
    }

    public static IMessage handleDeleteNetworkPacket(EntityPlayer player, NBTTagCompound nbtTag) {
        int id = nbtTag.getInteger(FluxNetworkData.NETWORK_ID);
        IFluxNetwork toDelete = FluxNetworkCache.instance.getNetwork(id);
        if (!toDelete.isInvalid()) {
            if (toDelete.getMemberPermission(player)
                .canDelete()) {
                FluxNetworkData.get()
                    .removeNetwork(toDelete);
                return feedback(EnumFeedbackInfo.SUCCESS);
            } else {
                return feedback(EnumFeedbackInfo.NO_OWNER);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 멤버 (원작에서도 미사용, enum 주석 처리)

    @Deprecated
    public static NBTTagCompound getAddMemberPacket(int networkID, String playerName) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
        tag.setString(PLAYER_NAME, playerName);
        return tag;
    }

    @Deprecated
    public static IMessage handleAddMemberPacket(EntityPlayer player, NBTTagCompound packetTag) {
        int networkID = packetTag.getInteger(FluxNetworkData.NETWORK_ID);
        String playerName = packetTag.getString(PLAYER_NAME);

        IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
        if (!network.isInvalid()) {
            if (network.getMemberPermission(player)
                .canEdit()) {
                network.addNewMember(playerName);
                return playersUpdate(network);
            } else {
                return feedback(EnumFeedbackInfo.NO_ADMIN);
            }
        }
        return null;
    }

    @Deprecated
    public static NBTTagCompound getRemoveMemberPacket(int networkID, UUID playerRemoved) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
        writeUUID(tag, PLAYER_REMOVED, playerRemoved);
        return tag;
    }

    @Deprecated
    public static IMessage handleRemoveMemberPacket(EntityPlayer player, NBTTagCompound packetTag) {
        int networkID = packetTag.getInteger(FluxNetworkData.NETWORK_ID);
        UUID playerRemoved = readUUID(packetTag, PLAYER_REMOVED);
        if (playerRemoved == null) {
            return null;
        }
        IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
        if (!network.isInvalid()) {
            if (network.getMemberPermission(player)
                .canEdit()) {
                network.removeMember(playerRemoved);
                return playersUpdate(network);
            } else {
                return feedback(EnumFeedbackInfo.NO_ADMIN);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 권한 변경

    /**
     * @param type 0 일반 사용자로 추가(비멤버), 1 관리자 지정, 2 일반 사용자로, 3 제거, 4 소유권 이전 (PERMISSION_* 상수)
     */
    public static NBTTagCompound getChangePermissionPacket(int networkID, UUID playerChanged, int type) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
        writeUUID(tag, PLAYER_CHANGED, playerChanged);
        tag.setInteger(PERMISSION_TYPE, type);
        return tag;
    }

    public static IMessage handleChangePermissionPacket(EntityPlayer player, NBTTagCompound packetTag) {
        int networkID = packetTag.getInteger(FluxNetworkData.NETWORK_ID);
        final UUID playerChanged = readUUID(packetTag, PLAYER_CHANGED);
        int type = packetTag.getInteger(PERMISSION_TYPE);
        if (playerChanged == null || type < PERMISSION_ADD_USER || type > PERMISSION_TRANSFER_OWNERSHIP) {
            return null; // 원작과 다름: 알 수 없는 type은 무시 (원작은 아무것도 바꾸지 않고 SUCCESS)
        }

        IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
        if (network.isInvalid()) {
            return null;
        }
        AccessLevel requester = network.getMemberPermission(player);
        if (!requester.canEdit()) {
            return feedback(EnumFeedbackInfo.NO_ADMIN);
        }
        // 보안: 소유권 이전은 소유자 또는 SuperAdmin(canDelete)만 가능. 원작은 canEdit이라 관리자가 소유권을 가져갈 수 있다
        if (type == PERMISSION_TRANSFER_OWNERSHIP && !requester.canDelete()) {
            return feedback(EnumFeedbackInfo.NO_OWNER);
        }

        List<NetworkMember> members = network.getSetting(NetworkSettings.NETWORK_PLAYERS);

        if (type == PERMISSION_ADD_USER) {
            // 원작과 다름: 이미 멤버면 중복 추가하지 않는다
            if (network.getValidMember(playerChanged)
                .isPresent()) {
                return feedback(EnumFeedbackInfo.INVALID_USER);
            }
            EntityPlayerMP target = getOnlinePlayer(playerChanged);
            if (target != null) {
                members.add(NetworkMember.createNetworkMember(target, AccessLevel.USER));
                sendFeedback(player, EnumFeedbackInfo.SUCCESS);
                return playersUpdate(network);
            }
            return feedback(EnumFeedbackInfo.INVALID_USER);
        }

        Optional<NetworkMember> settings = network.getValidMember(playerChanged);
        if (settings.isPresent()) {
            NetworkMember p = settings.get();
            // 보안: 현재 소유자는 관리자 지정·일반 사용자로·제거(type 1·2·3)의 대상이 될 수 없다
            if (type != PERMISSION_TRANSFER_OWNERSHIP && p.getAccessPermission() == AccessLevel.OWNER) {
                return feedback(EnumFeedbackInfo.NO_OWNER);
            }
            if (type == PERMISSION_SET_ADMIN) {
                p.setAccessPermission(AccessLevel.ADMIN);
            } else if (type == PERMISSION_SET_USER) {
                p.setAccessPermission(AccessLevel.USER);
            } else if (type == PERMISSION_REMOVE) {
                members.remove(p);
            } else {
                // 원작과 다름: 대상 자신은 지우지 않는다 (원작은 대상이 이미 소유자면 대상까지 목록에서 지운다)
                members.removeIf(
                    f -> f.getAccessPermission()
                        .canDelete()
                        && !f.getPlayerUUID()
                            .equals(playerChanged));
                network.setSetting(NetworkSettings.NETWORK_OWNER, playerChanged);
                p.setAccessPermission(AccessLevel.OWNER);
            }
            sendFeedback(player, EnumFeedbackInfo.SUCCESS);
            return playersUpdate(network);
        } else if (type == PERMISSION_TRANSFER_OWNERSHIP) {
            EntityPlayerMP target = getOnlinePlayer(playerChanged);
            if (target != null) {
                members.removeIf(
                    f -> f.getAccessPermission()
                        .canDelete());
                members.add(NetworkMember.createNetworkMember(target, AccessLevel.OWNER));
                network.setSetting(NetworkSettings.NETWORK_OWNER, playerChanged);
                sendFeedback(player, EnumFeedbackInfo.SUCCESS);
                return playersUpdate(network);
            }
            return feedback(EnumFeedbackInfo.INVALID_USER);
        }
        return feedback(EnumFeedbackInfo.INVALID_USER);
    }

    // ---------------------------------------------------------------- 무선 (4단계, GUI에서 숨김)

    public static NBTTagCompound getChangeWirelessPacket(int networkID, int wirelessMode) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
        tag.setInteger(FluxNetworkData.WIRELESS_MODE, wirelessMode);
        return tag;
    }

    public static IMessage handleChangeWirelessPacket(EntityPlayer player, NBTTagCompound packetTag) {
        int networkID = packetTag.getInteger(FluxNetworkData.NETWORK_ID);
        int wireless = packetTag.getInteger(FluxNetworkData.WIRELESS_MODE);
        IFluxNetwork network = FluxNetworkCache.instance.getNetwork(networkID);
        if (!network.isInvalid()) {
            if (network.getMemberPermission(player)
                .canEdit()) {
                network.setSetting(NetworkSettings.NETWORK_WIRELESS, wireless);
                send(
                    player,
                    new PacketNetworkUpdate.NetworkUpdateMessage(Lists.newArrayList(network), NBTType.NETWORK_GENERAL));
                return feedback(EnumFeedbackInfo.SUCCESS);
            } else {
                return feedback(EnumFeedbackInfo.NO_ADMIN);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 헬퍼

    /** null이면 빈 문자열, max자를 넘으면 앞 max자만 남긴다 */
    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** 범위를 벗어나면 null (패킷 규칙 4) */
    private static SecurityType readSecurityType(NBTTagCompound tag) {
        int i = tag.getInteger(FluxNetworkData.SECURITY_TYPE);
        SecurityType[] values = SecurityType.values();
        return i >= 0 && i < values.length ? values[i] : null;
    }

    private static void writeUUID(NBTTagCompound tag, String key, UUID uuid) {
        tag.setLong(key + "Most", uuid.getMostSignificantBits());
        tag.setLong(key + "Least", uuid.getLeastSignificantBits());
    }

    private static UUID readUUID(NBTTagCompound tag, String key) {
        if (!tag.hasKey(key + "Most") || !tag.hasKey(key + "Least")) {
            return null;
        }
        return new UUID(tag.getLong(key + "Most"), tag.getLong(key + "Least"));
    }

    /** 1.7.10에는 PlayerList.getPlayerByUUID가 없으므로 접속자 목록을 돈다 */
    private static EntityPlayerMP getOnlinePlayer(UUID uuid) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        // 1.7.10: playerEntityList는 raw List
        for (Object o : server.getConfigurationManager().playerEntityList) {
            if (o instanceof EntityPlayerMP) {
                EntityPlayerMP p = (EntityPlayerMP) o;
                if (uuid.equals(p.getUniqueID())) {
                    return p;
                }
            }
        }
        return null;
    }

    private static IMessage playersUpdate(IFluxNetwork network) {
        return new PacketNetworkUpdate.NetworkUpdateMessage(Lists.newArrayList(network), NBTType.NETWORK_PLAYERS);
    }

    private static IMessage feedback(EnumFeedbackInfo info) {
        return new PacketFeedback.FeedbackMessage(info);
    }

    private static void sendFeedback(EntityPlayer player, EnumFeedbackInfo info) {
        send(player, feedback(info));
    }

    private static void send(EntityPlayer player, IMessage message) {
        if (player instanceof EntityPlayerMP) {
            PacketHandler.network.sendTo(message, (EntityPlayerMP) player);
        }
    }
}
