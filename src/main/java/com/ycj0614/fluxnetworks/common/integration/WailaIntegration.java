package com.ycj0614.fluxnetworks.common.integration;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPlug;
import com.ycj0614.fluxnetworks.api.tiles.IFluxPoint;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.common.block.BlockFluxCore;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

/**
 * WAILA 연동 (원작 TOPIntegration). WAILA 클래스는 이 클래스에서만 import한다.
 * CommonProxy가 WAILA가 있을 때만 IMC("Waila", "register")로 register를 알려준다.
 * 표시할 값은 서버 getNBTData에서 보내고 (멀티플레이 정확도), 클라이언트는 그 태그만 읽는다.
 */
public class WailaIntegration implements IWailaDataProvider {

    private static final String MOD_NAME = "Flux Networks";

    // 원작과 다름: 원작 enableOneProbeBasicInfo/AdvancedInfo/Sneaking 설정 대신 WAILA 자체 설정
    public static final String CONFIG_BASIC = "fluxnetworks.basic";
    public static final String CONFIG_ADVANCED = "fluxnetworks.advanced";
    public static final String CONFIG_SNEAKING = "fluxnetworks.sneaking";

    /** WAILA가 다른 데이터와 섞지 않도록 우리 값은 이 하위 태그에 넣는다 */
    private static final String NBT_ROOT = "fluxnetworks";
    private static final String NBT_NAME = "name";
    private static final String NBT_NETWORK = "network";
    private static final String NBT_TYPE = "type";
    private static final String NBT_CHANGE = "change";
    private static final String NBT_BUFFER = "buffer";
    private static final String NBT_LIMIT = "limit";
    private static final String NBT_DISABLE_LIMIT = "disableLimit";
    private static final String NBT_PRIORITY = "priority";
    private static final String NBT_SURGE = "surge";
    private static final String NBT_FORCED = "forced";
    private static final String NBT_TIER = "tier";
    private static final String NBT_AMPERAGE = "amperage";

    private static final WailaIntegration INSTANCE = new WailaIntegration();

    /** IMC 콜백. 이름은 CommonProxy의 IMC 문자열과 맞춘다 */
    public static void register(IWailaRegistrar registrar) {
        registrar.addConfig(MOD_NAME, CONFIG_BASIC, true);
        registrar.addConfig(MOD_NAME, CONFIG_ADVANCED, true);
        registrar.addConfig(MOD_NAME, CONFIG_SNEAKING, true);
        registrar.registerHeadProvider(INSTANCE, BlockFluxCore.class);
        registrar.registerBodyProvider(INSTANCE, BlockFluxCore.class);
        registrar.registerNBTProvider(INSTANCE, BlockFluxCore.class);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    /** 원작 TOP의 display override: 머리 줄을 장치 이름으로 */
    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        NBTTagCompound tag = getFluxTag(accessor);
        if (tag == null) {
            return currenttip;
        }
        String name = tag.getString(NBT_NAME);
        // 원작과 다름: 이름이 비어 있으면 WAILA 기본 줄(블록 이름)을 그대로 둔다
        if (!name.isEmpty()) {
            if (currenttip.isEmpty()) {
                currenttip.add(EnumChatFormatting.WHITE + name);
            } else {
                currenttip.set(0, EnumChatFormatting.WHITE + name);
            }
        }
        return currenttip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        NBTTagCompound tag = getFluxTag(accessor);
        if (tag == null) {
            return currenttip;
        }
        int typeIndex = tag.getInteger(NBT_TYPE);
        if (typeIndex < 0 || typeIndex >= ConnectionType.values().length) {
            return currenttip;
        }
        ConnectionType type = ConnectionType.values()[typeIndex];
        boolean sneaking = accessor.getPlayer() != null && accessor.getPlayer()
            .isSneaking();

        if (config.getConfig(CONFIG_BASIC, true)) {
            String network = tag.hasKey(NBT_NETWORK, Constants.NBT.TAG_STRING) ? tag.getString(NBT_NETWORK)
                : FluxTranslate.ERROR_NO_SELECTED.t();
            currenttip.add(EnumChatFormatting.AQUA + network);
            currenttip.add(FluxUtils.getTransferInfo(type, tag.getLong(NBT_CHANGE)));
            String buffer = FluxUtils.format(
                tag.getLong(NBT_BUFFER),
                sneaking ? FluxUtils.TypeNumberFormat.COMMAS : FluxUtils.TypeNumberFormat.COMPACT,
                false);
            currenttip.add(
                (type.isStorage() ? FluxTranslate.ENERGY_STORED : FluxTranslate.INTERNAL_BUFFER).t() + ": "
                    + EnumChatFormatting.GREEN
                    + buffer);
        }
        if (config.getConfig(CONFIG_ADVANCED, true) && (!config.getConfig(CONFIG_SNEAKING, true) || sneaking)) {
            currenttip.add(
                FluxTranslate.TRANSFER_LIMIT.t() + ": "
                    + EnumChatFormatting.GREEN
                    + (tag.getBoolean(NBT_DISABLE_LIMIT) ? FluxTranslate.UNLIMITED.t()
                        : FluxUtils.format(tag.getLong(NBT_LIMIT), FluxUtils.TypeNumberFormat.COMMAS, true)));
            currenttip.add(
                FluxTranslate.PRIORITY.t() + ": "
                    + EnumChatFormatting.GREEN
                    + (tag.getBoolean(NBT_SURGE) ? FluxTranslate.SURGE.t() : tag.getInteger(NBT_PRIORITY)));
            if (tag.getBoolean(NBT_FORCED)) {
                currenttip.add(EnumChatFormatting.GOLD + FluxTranslate.FORCED_LOADING.t());
            }
            // 원작과 다름: GT EU 설정 (플러그 최대 입력 티어, 포인트 출력 티어·암페어)
            if (type.isPlug()) {
                currenttip.add(
                    FluxTranslate.MAX_INPUT_TIER.t() + ": "
                        + EnumChatFormatting.AQUA
                        + formatTier(tag.getInteger(NBT_TIER)));
            } else if (type.isPoint()) {
                currenttip.add(
                    FluxTranslate.OUTPUT_TIER.t() + ": "
                        + EnumChatFormatting.AQUA
                        + formatTier(tag.getInteger(NBT_TIER)));
                currenttip.add(
                    FluxTranslate.OUTPUT_AMPERAGE.t() + ": "
                        + EnumChatFormatting.AQUA
                        + tag.getLong(NBT_AMPERAGE)
                        + "A");
            }
        }
        return currenttip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        return currenttip;
    }

    /** 서버에서 실행된다 */
    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x,
        int y, int z) {
        if (!(te instanceof IFluxConnector)) {
            return tag;
        }
        IFluxConnector flux = (IFluxConnector) te;
        ConnectionType type = flux.getConnectionType();
        NBTTagCompound data = new NBTTagCompound();
        data.setString(NBT_NAME, flux.getCustomName());
        IFluxNetwork network = flux.getNetwork();
        if (!network.isInvalid()) {
            data.setString(NBT_NETWORK, network.getNetworkName());
        }
        data.setInteger(NBT_TYPE, type.ordinal());
        data.setLong(NBT_CHANGE, flux.getTransferChange());
        data.setLong(NBT_BUFFER, flux.getTransferBuffer());
        data.setLong(NBT_LIMIT, flux.getRawLimit());
        data.setBoolean(NBT_DISABLE_LIMIT, flux.getDisableLimit());
        data.setInteger(NBT_PRIORITY, flux.getRawPriority());
        data.setBoolean(NBT_SURGE, flux.getSurgeMode());
        data.setBoolean(NBT_FORCED, flux.isForcedLoading());
        // 스토리지는 IFluxPlug·IFluxPoint를 둘 다 구현하므로 instanceof가 아니라 연결 종류로 고른다
        if (type.isPlug() && te instanceof IFluxPlug) {
            data.setInteger(NBT_TIER, ((IFluxPlug) te).getMaxInputTier());
        } else if (type.isPoint() && te instanceof IFluxPoint) {
            data.setInteger(NBT_TIER, ((IFluxPoint) te).getOutputTier());
            data.setLong(NBT_AMPERAGE, ((IFluxPoint) te).getOutputAmperage());
        }
        tag.setTag(NBT_ROOT, data);
        return tag;
    }

    /** 서버 데이터가 아직 오지 않았으면 null */
    private static NBTTagCompound getFluxTag(IWailaDataAccessor accessor) {
        NBTTagCompound tag = accessor.getNBTData();
        if (tag == null || !tag.hasKey(NBT_ROOT, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompoundTag(NBT_ROOT);
    }

    /** GuiDraw.formatTier와 같은 "LV (32 EU/t)" 형식. GuiDraw는 클라이언트 전용이라 여기서 따로 만든다 */
    private static String formatTier(int tier) {
        return GTEnergy.tierName(tier) + " ("
            + FluxUtils.format(GTEnergy.voltage(tier), FluxUtils.TypeNumberFormat.COMMAS, true)
            + ")";
    }
}
