package com.ycj0614.fluxnetworks.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.client.gui.basic.GuiFluxCore;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.network.PacketColorCache;
import com.ycj0614.fluxnetworks.common.network.PacketColorRequest;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 FluxColorHandler. 네트워크 이름·색 캐시(아이템 툴팁에서 서버에 요청해 받은 값)와 블록·아이템 색 입히기.
 * <p>
 * 1.7.10에는 IBlockColor/IItemColor와 tintIndex가 없다. 원작의 tintIndex 1(colour 텍스처 레이어)에 입힐 색을
 * 렌더러(BlockFluxRenderer, ItemFluxDeviceRenderer)와 ItemConfigurator.getColorFromItemStack이 여기서 받아 간다.
 */
@SideOnly(Side.CLIENT)
public class FluxColorHandler {

    public static final int DEFAULT_COLOR = FluxUtils.getIntFromColor(41, 94, 138);
    public static final int NO_NETWORK_COLOR = FluxUtils.getIntFromColor(178, 178, 178);

    /** 원작은 40 렌더 틱마다 보냈다. 여기서는 클라이언트 틱(약 2초) 기준 */
    private static final int REQUEST_INTERVAL = 40;

    private static final Map<Integer, Integer> colorCache = new HashMap<>();
    private static final Map<Integer, String> nameCache = new HashMap<>();
    private static final Set<Integer> requests = new LinkedHashSet<>();
    private static final Set<Integer> sentRequests = new HashSet<>();

    private static int tickCount;

    public static void reset() {
        colorCache.clear();
        nameCache.clear();
        requests.clear();
        sentRequests.clear(); // 원작과 다름: 원작은 보낸 요청 목록을 비우지 않아 재접속 후 다시 요청하지 못했다
        tickCount = 0;
    }

    public static void loadColorCache(int id, int color) {
        if (id != -1) {
            colorCache.put(id, color);
        }
    }

    public static void loadNameCache(int id, String name) {
        if (id != -1) {
            nameCache.put(id, name);
        }
    }

    public static void placeRequest(int id) {
        if (id != -1 && !sentRequests.contains(id)) {
            requests.add(id);
        }
    }

    public static int getOrRequestNetworkColor(int id) {
        if (id == -1) {
            return NO_NETWORK_COLOR;
        }
        Integer cached = colorCache.get(id);
        if (cached != null) {
            return cached;
        }
        placeRequest(id);
        return NO_NETWORK_COLOR;
    }

    public static String getOrRequestNetworkName(int id) {
        if (id == -1) {
            return "NONE";
        }
        String cached = nameCache.get(id);
        if (cached != null) {
            return cached;
        }
        placeRequest(id);
        return "WAITING FOR SERVER";
    }

    /** ClientTickEvent END마다 호출. 모인 요청을 REQUEST_INTERVAL 틱마다 한 번에 보낸다 */
    public static void sendRequests() {
        if (requests.isEmpty()) {
            return;
        }
        if (++tickCount > REQUEST_INTERVAL) {
            tickCount = 0;
            List<Integer> toSend = new ArrayList<>();
            for (Integer id : requests) {
                if (toSend.size() >= PacketColorCache.MAX_ENTRIES) {
                    break; // 남은 요청은 다음 주기에 보낸다
                }
                toSend.add(id);
            }
            PacketHandler.network.sendToServer(new PacketColorRequest.ColorRequestMessage(toSend));
            sentRequests.addAll(toSend);
            requests.removeAll(toSend);
        }
    }

    // ---- 색 입히기 (원작 colorMultiplier, tintIndex 1) ----

    /** 월드의 장치 블록. 연결 안 됨이면 NO_NETWORK_COLOR, 연결됐으면 네트워크 색을 조금 밝게 */
    public static int getBlockColor(TileFluxCore tile) {
        if (!tile.connected) {
            return NO_NETWORK_COLOR;
        }
        return FluxUtils.getBrighterColor(tile.color, 1.2);
    }

    /** 장치 아이템. GUI_COLOR 태그가 있으면 열린 GUI의 네트워크 색, 아니면 FLUX_DATA의 네트워크 색 */
    public static int getItemColor(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound()
            .getBoolean(FluxUtils.GUI_COLOR)) {
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen instanceof GuiFluxCore) {
                GuiFluxCore guiFluxCore = (GuiFluxCore) screen;
                return guiFluxCore.network.isInvalid() ? NO_NETWORK_COLOR
                    : guiFluxCore.network.getSetting(NetworkSettings.NETWORK_COLOR) | 0xff000000;
            }
        }
        if (stack.hasTagCompound() && stack.getTagCompound()
            .hasKey(FluxUtils.FLUX_DATA, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound tag = stack.getTagCompound()
                .getCompoundTag(FluxUtils.FLUX_DATA);
            // 원작과 다름: 원작은 키가 없으면 getInteger의 0(네트워크 0번)을 요청했다
            int id = tag.hasKey(FluxNetworkData.NETWORK_ID) ? tag.getInteger(FluxNetworkData.NETWORK_ID) : -1;
            return getOrRequestNetworkColor(id);
        }
        return NO_NETWORK_COLOR;
    }

    /** 구성기 layer1. 구성기 GUI가 열려 있으면 그 네트워크 색, 아니면 복사해 둔 설정의 네트워크 색 */
    public static int getConfiguratorColor(ItemStack stack) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiFluxCore) {
            GuiFluxCore guiFluxCore = (GuiFluxCore) screen;
            if (guiFluxCore.connector instanceof ItemConfigurator.NetworkConnector) {
                return guiFluxCore.network.getSetting(NetworkSettings.NETWORK_COLOR);
            }
        }
        return getOrRequestNetworkColor(ItemConfigurator.getNetworkID(ItemConfigurator.getConfigs(stack)));
    }

    public static void receiveCache(Map<Integer, PacketColorCache.Entry> cache) {
        for (Map.Entry<Integer, PacketColorCache.Entry> e : cache.entrySet()) {
            int id = e.getKey();
            loadColorCache(id, e.getValue().color);
            loadNameCache(id, e.getValue().name);
            sentRequests.remove(id);
            requests.remove(id);
        }
    }
}
