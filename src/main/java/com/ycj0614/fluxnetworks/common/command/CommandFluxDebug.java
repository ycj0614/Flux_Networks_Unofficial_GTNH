package com.ycj0614.fluxnetworks.common.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.network.SecurityType;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

/**
 * 1단계 임시 디버그 명령. GUI 없이 네트워크를 만들고 장치를 연결·설정한다.
 * <p>
 * // TODO 2단계 완료 후 삭제 (BlockFluxCore.onBlockActivated의 sendInfo 호출도 함께 GUI 열기로 교체)
 */
public class CommandFluxDebug extends CommandBase {

    private static final String NAME = "fluxdebug";

    /** 1단계 고정 색 (2단계 GUI에서 선택) */
    private static final int DEFAULT_NETWORK_COLOR = 0x4CA6FF;

    /** 바라보는 블록을 찾는 최대 거리 */
    private static final double REACH = 6.0D;

    private static final String[] SUB_COMMANDS = { "create", "list", "delete", "connect", "disconnect", "tier", "amp",
        "info" };

    /** ForgeDirection 순서 (DOWN, UP, NORTH, SOUTH, WEST, EAST) = connections[] 인덱스 */
    private static final String[] SIDE_NAMES = { "D", "U", "N", "S", "W", "E" };

    private static final String USAGE = "/fluxdebug <create|list|delete|connect|disconnect|tier|amp|info> ..."
        + " ([x y z] = target block, default: the block you are looking at)";
    private static final String USAGE_CREATE = "/fluxdebug create <name>";
    private static final String USAGE_DELETE = "/fluxdebug delete <id>";
    private static final String USAGE_CONNECT = "/fluxdebug connect <id> [x y z]";
    private static final String USAGE_DISCONNECT = "/fluxdebug disconnect [x y z]";
    private static final String USAGE_TIER = "/fluxdebug tier <0-14 or ULV..MAX> [x y z]";
    private static final String USAGE_AMP = "/fluxdebug amp <amperage> [x y z]";
    private static final String USAGE_INFO = "/fluxdebug info [x y z]";

    @Override
    public String getCommandName() {
        return NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("/fluxdebug can only be used by a player.");
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        if (args.length == 0) {
            throw new WrongUsageException(USAGE);
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":
                create(player, args);
                break;
            case "list":
                list(player, args);
                break;
            case "delete":
                delete(player, args);
                break;
            case "connect":
                connect(player, args);
                break;
            case "disconnect":
                disconnect(player, args);
                break;
            case "tier":
                tier(player, args);
                break;
            case "amp":
                amp(player, args);
                break;
            case "info":
                info(player, args);
                break;
            default:
                throw new WrongUsageException(USAGE);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUB_COMMANDS);
        }
        if (args.length == 2 && "tier".equalsIgnoreCase(args[0])) {
            String[] names = new String[GTEnergy.MAX_TIER - GTEnergy.MIN_TIER + 1];
            for (int t = GTEnergy.MIN_TIER; t <= GTEnergy.MAX_TIER; t++) {
                names[t - GTEnergy.MIN_TIER] = GTEnergy.tierName(t);
            }
            return getListOfStringsMatchingLastWord(args, names);
        }
        return null;
    }

    // ---- 하위 명령 ----

    private void create(EntityPlayerMP player, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE_CREATE);
        }
        String name = joinArgs(args, 1);
        if (name.isEmpty()) {
            throw new WrongUsageException(USAGE_CREATE);
        }
        if (!FluxNetworkCache.instance.hasSpaceLeft(player)) {
            throw new CommandException(
                "You have reached the maximum number of networks per player (%s).",
                FluxConfig.maximumPerPlayer);
        }
        IFluxNetwork network = FluxNetworkCache.instance
            .createdNetwork(player, name, DEFAULT_NETWORK_COLOR, SecurityType.PUBLIC, "");
        send(
            player,
            EnumChatFormatting.GREEN + "Created public network "
                + EnumChatFormatting.AQUA
                + "#"
                + network.getNetworkID()
                + EnumChatFormatting.GREEN
                + " \""
                + name
                + "\".");
        send(
            player,
            EnumChatFormatting.GRAY + "Look at a flux plug/point and run /fluxdebug connect " + network.getNetworkID());
    }

    private void list(EntityPlayerMP player, String[] args) {
        if (args.length != 1) {
            throw new WrongUsageException("/fluxdebug list");
        }
        List<IFluxNetwork> networks = new ArrayList<>(FluxNetworkCache.instance.getAllNetworks());
        if (networks.isEmpty()) {
            send(player, EnumChatFormatting.YELLOW + "There are no flux networks.");
            return;
        }
        networks.sort(Comparator.comparingInt(IFluxNetwork::getNetworkID));
        send(player, EnumChatFormatting.GOLD + "Flux networks (" + networks.size() + "):");
        for (IFluxNetwork network : networks) {
            UUID owner = network.getSetting(NetworkSettings.NETWORK_OWNER);
            int plugs = network.getConnections(FluxLogicType.PLUG)
                .size();
            int points = network.getConnections(FluxLogicType.POINT)
                .size();
            send(
                player,
                EnumChatFormatting.AQUA + "#"
                    + network.getNetworkID()
                    + " "
                    + EnumChatFormatting.WHITE
                    + networkName(network)
                    + EnumChatFormatting.GRAY
                    + " | owner "
                    + shortUuid(owner)
                    + " | plugs "
                    + plugs
                    + ", points "
                    + points
                    + " | in "
                    + EnumChatFormatting.GREEN
                    + euT(statInput(network))
                    + EnumChatFormatting.GRAY
                    + ", out "
                    + EnumChatFormatting.RED
                    + euT(statOutput(network)));
        }
    }

    private void delete(EntityPlayerMP player, String[] args) {
        if (args.length != 2) {
            throw new WrongUsageException(USAGE_DELETE);
        }
        int id = parseInt(player, args[1]);
        IFluxNetwork network = FluxNetworkCache.instance.getNetwork(id);
        if (network.isInvalid()) {
            throw new CommandException("Network #%s does not exist.", id);
        }
        UUID owner = network.getSetting(NetworkSettings.NETWORK_OWNER);
        boolean isOwner = owner != null && owner.equals(
            player.getGameProfile()
                .getId());
        // 이 명령은 권한 레벨 2(OP)라 지금은 항상 통과한다. 권한 레벨을 낮출 경우를 위해 남겨 둔다
        if (!isOwner && !player.canCommandSenderUseCommand(2, NAME)) {
            throw new CommandException("Only the network owner or an operator can delete network #%s.", id);
        }

        String name = networkName(network);
        List<IFluxConnector> devices = new ArrayList<>();
        for (IFluxConnector c : network.getConnections(FluxLogicType.PLUG)) {
            devices.add(c);
        }
        for (IFluxConnector c : network.getConnections(FluxLogicType.POINT)) {
            devices.add(c);
        }

        FluxNetworkData.get()
            .removeNetwork(network);

        int stillLinked = 0;
        for (IFluxConnector device : devices) {
            if (device.getNetworkID() == id) {
                stillLinked++;
            }
        }
        send(
            player,
            EnumChatFormatting.GREEN + "Deleted network "
                + EnumChatFormatting.AQUA
                + "#"
                + id
                + EnumChatFormatting.GREEN
                + " \""
                + name
                + "\".");
        send(
            player,
            EnumChatFormatting.GRAY + "Connected devices: "
                + devices.size()
                + ", disconnected now: "
                + (devices.size() - stillLinked)
                + ", still linked: "
                + stillLinked);
        if (stillLinked > 0) {
            send(
                player,
                EnumChatFormatting.YELLOW
                    + "Some devices still point to the deleted network (check FluxNetworkServer.onDeleted).");
        }
    }

    private void connect(EntityPlayerMP player, String[] args) {
        if (args.length != 2 && args.length != 5) {
            throw new WrongUsageException(USAGE_CONNECT);
        }
        int id = parseInt(player, args[1]);
        final TileFluxCore tile = getTarget(player, args, 2);
        checkAccess(player, tile);

        IFluxNetwork target = FluxNetworkCache.instance.getNetwork(id);
        if (target.isInvalid()) {
            throw new CommandException("Network #%s does not exist.", id);
        }
        if (!target.getMemberPermission(player)
            .canAccess()) {
            throw new CommandException("You don't have access to network #%s.", id);
        }

        IFluxNetwork current = tile.getNetwork();
        if (!current.isInvalid() && current.getNetworkID() == id) {
            send(player, EnumChatFormatting.YELLOW + "This device is already connected to network #" + id + ".");
            return;
        }

        // 원작 FluxConfigurationType.pasteNetwork와 같은 방식
        if (!current.isInvalid()) {
            current.queueConnectionRemoval(tile, false);
        }
        target.queueConnectionAddition(tile);

        // 실제 connect()는 서버 틱 끝의 FluxConnectionEvent에서 일어난다. 그 다음 틱 시작에 저장 표시를 한다
        // (TileFluxCore.connect()가 markDirty를 부르지 않으므로 바뀐 networkID가 청크 저장에서 빠지지 않게 한다)
        FluxNetworks.proxy.scheduleServerTask(() -> {
            if (!tile.isInvalid()) {
                tile.markDirty();
            }
        });

        send(
            player,
            EnumChatFormatting.GREEN + "Queued "
                + deviceName(tile)
                + " at "
                + coords(tile)
                + " for network "
                + EnumChatFormatting.AQUA
                + "#"
                + id
                + EnumChatFormatting.GREEN
                + " \""
                + networkName(target)
                + "\".");
        send(
            player,
            EnumChatFormatting.GRAY
                + "The connection is made at the end of this server tick (FluxConnectionEvent). Check with /fluxdebug info.");
    }

    private void disconnect(EntityPlayerMP player, String[] args) {
        if (args.length != 1 && args.length != 4) {
            throw new WrongUsageException(USAGE_DISCONNECT);
        }
        TileFluxCore tile = getTarget(player, args, 1);
        checkAccess(player, tile);

        IFluxNetwork network = tile.getNetwork();
        if (network.isInvalid()) {
            send(player, EnumChatFormatting.YELLOW + "This device is not connected to any network.");
            return;
        }
        int id = network.getNetworkID();
        network.queueConnectionRemoval(tile, false);
        tile.disconnect(network);
        tile.markDirty();
        send(
            player,
            EnumChatFormatting.GREEN + "Disconnected "
                + deviceName(tile)
                + " at "
                + coords(tile)
                + " from network "
                + EnumChatFormatting.AQUA
                + "#"
                + id
                + EnumChatFormatting.GREEN
                + ".");
    }

    private void tier(EntityPlayerMP player, String[] args) {
        if (args.length != 2 && args.length != 5) {
            throw new WrongUsageException(USAGE_TIER);
        }
        int tier = parseTier(player, args[1]);
        TileFluxCore tile = getTarget(player, args, 2);
        checkAccess(player, tile);

        if (tile instanceof TileFluxPlug) {
            TileFluxPlug plug = (TileFluxPlug) tile;
            plug.setMaxInputTier(tier);
            send(
                player,
                EnumChatFormatting.GREEN + "Flux Plug max input tier set to "
                    + EnumChatFormatting.WHITE
                    + tierText(plug.getMaxInputTier())
                    + EnumChatFormatting.GREEN
                    + ". Higher voltages are rejected.");
        } else if (tile instanceof TileFluxPoint) {
            TileFluxPoint point = (TileFluxPoint) tile;
            point.setOutputTier(tier);
            send(
                player,
                EnumChatFormatting.GREEN + "Flux Point output tier set to "
                    + EnumChatFormatting.WHITE
                    + tierText(point.getOutputTier())
                    + EnumChatFormatting.GREEN
                    + ", max output "
                    + euT(point.getLogicLimit())
                    + " ("
                    + point.getOutputAmperage()
                    + " A).");
        } else {
            throw new CommandException("This device has no voltage tier.");
        }
    }

    private void amp(EntityPlayerMP player, String[] args) {
        if (args.length != 2 && args.length != 5) {
            throw new WrongUsageException(USAGE_AMP);
        }
        long amperage = parseLongWithMin(args[1], 1L);
        TileFluxCore tile = getTarget(player, args, 2);
        if (!(tile instanceof TileFluxPoint)) {
            throw new CommandException("Amperage can only be set on a Flux Point.");
        }
        checkAccess(player, tile);

        TileFluxPoint point = (TileFluxPoint) tile;
        point.setOutputAmperage(amperage);
        send(
            player,
            EnumChatFormatting.GREEN + "Flux Point output amperage set to "
                + EnumChatFormatting.WHITE
                + point.getOutputAmperage()
                + " A"
                + EnumChatFormatting.GREEN
                + " at "
                + tierText(point.getOutputTier())
                + ", max output "
                + euT(point.getLogicLimit())
                + ".");
    }

    private void info(EntityPlayerMP player, String[] args) {
        if (args.length != 1 && args.length != 4) {
            throw new WrongUsageException(USAGE_INFO);
        }
        sendInfo(player, getTarget(player, args, 1));
    }

    // ---- 공용: 장치 정보 (블록 우클릭에서도 사용) ----

    public static void sendInfo(EntityPlayer player, TileFluxCore tile) {
        World world = tile.getWorldObj();
        String dim = world != null ? String.valueOf(world.provider.dimensionId) : "?";
        send(
            player,
            EnumChatFormatting.GOLD + "== "
                + deviceName(tile)
                + " "
                + EnumChatFormatting.GRAY
                + "at "
                + coords(tile)
                + " (dim "
                + dim
                + ")");

        IFluxNetwork network = tile.getNetwork();
        if (network.isInvalid()) {
            String saved = tile.getNetworkID() != -1
                ? EnumChatFormatting.GRAY + " (saved ID " + tile.getNetworkID() + ")"
                : "";
            send(player, "Network: " + EnumChatFormatting.RED + "not connected" + saved);
        } else {
            send(
                player,
                "Network: " + EnumChatFormatting.AQUA
                    + "#"
                    + network.getNetworkID()
                    + " "
                    + EnumChatFormatting.WHITE
                    + networkName(network));
        }
        send(
            player,
            "Connected: " + (tile.connected ? EnumChatFormatting.GREEN + "true" : EnumChatFormatting.RED + "false")
                + EnumChatFormatting.RESET
                + ", owner: "
                + shortUuid(tile.getConnectionOwner()));

        if (tile instanceof TileFluxPlug) {
            TileFluxPlug plug = (TileFluxPlug) tile;
            send(
                player,
                "Max input tier: " + EnumChatFormatting.WHITE
                    + tierText(plug.getMaxInputTier())
                    + EnumChatFormatting.RESET
                    + ", transfer limit: "
                    + euT(plug.getLogicLimit()));
        } else if (tile instanceof TileFluxPoint) {
            TileFluxPoint point = (TileFluxPoint) tile;
            send(
                player,
                "Output tier: " + EnumChatFormatting.WHITE
                    + tierText(point.getOutputTier())
                    + EnumChatFormatting.RESET
                    + ", amperage: "
                    + point.getOutputAmperage()
                    + " A, max output: "
                    + euT(point.getLogicLimit()));
        }

        send(
            player,
            "Buffer: " + eu(tile.getTransferBuffer()) + ", last tick change: " + euT(tile.getTransferChange()));

        StringBuilder sides = new StringBuilder();
        sides.append(EnumChatFormatting.GRAY)
            .append("Sides (green = GT device): ");
        for (int i = 0; i < SIDE_NAMES.length && i < tile.connections.length; i++) {
            sides.append(tile.connections[i] != 0 ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_GRAY)
                .append(SIDE_NAMES[i])
                .append(' ');
        }
        send(player, sides.toString());
    }

    // ---- 대상 블록 ----

    /**
     * args[coordIndex..coordIndex+2]가 있으면 그 좌표, 없으면 바라보는 블록 (최대 REACH).
     * 인자 개수는 호출하는 쪽에서 미리 검사한다.
     */
    private static TileFluxCore getTarget(EntityPlayerMP player, String[] args, int coordIndex) {
        World world = player.worldObj;
        int x, y, z;
        if (args.length >= coordIndex + 3) {
            x = parseInt(player, args[coordIndex]);
            y = parseInt(player, args[coordIndex + 1]);
            z = parseInt(player, args[coordIndex + 2]);
        } else {
            MovingObjectPosition hit = rayTrace(player);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                throw new CommandException(
                    "Look at a flux device within %s blocks, or give its coordinates.",
                    (int) REACH);
            }
            x = hit.blockX;
            y = hit.blockY;
            z = hit.blockZ;
        }
        // TODO 확인: World.blockExists(int, int, int) (배치 F에서도 사용)
        if (!world.blockExists(x, y, z)) {
            throw new CommandException("The block at %s, %s, %s is not loaded.", x, y, z);
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileFluxCore)) {
            throw new CommandException("There is no flux device at %s, %s, %s.", x, y, z);
        }
        return (TileFluxCore) tile;
    }

    private static MovingObjectPosition rayTrace(EntityPlayerMP player) {
        // TODO 확인: 1.7.10 서버(EntityPlayerMP)에서 posY + getEyeHeight()가 눈 높이인지.
        // Forge 1.7.10 Item.getMovingObjectPositionFromPlayer가 서버(!isRemote)에서 같은 식을 쓴다
        Vec3 start = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH);
        // 1.7.10 rayTraceBlocks는 start 벡터를 변경하므로 end를 먼저 계산해 둔다
        return player.worldObj.rayTraceBlocks(start, end);
    }

    private static void checkAccess(EntityPlayer player, TileFluxCore tile) {
        if (!tile.canAccess(player)) {
            throw new CommandException("You don't have permission to configure this device.");
        }
    }

    // ---- 파싱 ----

    /** 숫자(0~14) 또는 티어 이름(ULV, LV, MV ...) */
    private static int parseTier(ICommandSender sender, String s) {
        for (int t = GTEnergy.MIN_TIER; t <= GTEnergy.MAX_TIER; t++) {
            if (GTEnergy.tierName(t)
                .equalsIgnoreCase(s)) {
                return t;
            }
        }
        return parseIntBounded(sender, s, GTEnergy.MIN_TIER, GTEnergy.MAX_TIER);
    }

    private static long parseLongWithMin(String s, long min) {
        long value;
        try {
            value = Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new NumberInvalidException("commands.generic.num.invalid", s);
        }
        if (value < min) {
            throw new NumberInvalidException("commands.generic.num.tooSmall", value, min);
        }
        return value;
    }

    private static String joinArgs(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString()
            .trim();
    }

    // ---- 표시 ----

    // TODO 확인: NetworkSettings.NETWORK_NAME이 String 값 설정인지 (원작 IFluxNetwork.getNetworkName()이 남아 있다면 그쪽을 써도 된다)
    private static String networkName(IFluxNetwork network) {
        if (network.isInvalid()) {
            return "(none)";
        }
        String name = network.getSetting(NetworkSettings.NETWORK_NAME);
        return name != null ? name : "";
    }

    // TODO 확인: NetworkSettings.NETWORK_STATISTICS의 값(원작 NetworkStatistics)에 public long 필드 energyInput, energyOutput가
    // 있는지
    private static long statInput(IFluxNetwork network) {
        return network.getSetting(NetworkSettings.NETWORK_STATISTICS).energyInput;
    }

    private static long statOutput(IFluxNetwork network) {
        return network.getSetting(NetworkSettings.NETWORK_STATISTICS).energyOutput;
    }

    private static String deviceName(TileFluxCore tile) {
        if (tile instanceof TileFluxPlug) {
            return "Flux Plug";
        }
        if (tile instanceof TileFluxPoint) {
            return "Flux Point";
        }
        return tile.getClass()
            .getSimpleName();
    }

    private static String coords(TileFluxCore tile) {
        return tile.xCoord + ", " + tile.yCoord + ", " + tile.zCoord;
    }

    private static String shortUuid(UUID uuid) {
        if (uuid == null) {
            return "none";
        }
        return uuid.toString()
            .substring(0, 8);
    }

    private static String tierText(int tier) {
        return GTEnergy.tierName(tier) + " (" + euT(GTEnergy.voltage(tier)) + ")";
    }

    private static String eu(long value) {
        return FluxUtils.format(value, FluxUtils.TypeNumberFormat.COMMAS, false);
    }

    private static String euT(long value) {
        if (value == Long.MAX_VALUE) {
            return "unlimited";
        }
        return FluxUtils.format(value, FluxUtils.TypeNumberFormat.COMMAS, true);
    }

    private static void send(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
