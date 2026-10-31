package com.ycj0614.fluxnetworks.common.connection.transfer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkMember;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.network.WirelessType;
import com.ycj0614.fluxnetworks.common.core.ItemEnergy;
import com.ycj0614.fluxnetworks.common.integration.BaublesIntegration;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxController;

/**
 * 컨트롤러 전송 핸들러 (원작 알고리즘). 네트워크 에너지로 멤버 플레이어의 전기 아이템을 무선 충전한다.
 * <p>
 * timer는 0~31을 돈다. timer == 0이면 충전 대상 플레이어를 다시 고르고, (timer & 3) == 2이면 다음 충전에 쓸
 * 요청량(demand)을 시뮬레이션으로 정하고, (timer & 3) == 0이면 버퍼에서 실제로 충전한다 (4틱마다).
 * 그래서 ItemEnergy는 한 번에 아이템 전송 한도 × 4까지 넣는다.
 * <p>
 * 슬롯 순서: 오른손 → 핫바(든 아이템 제외) → 갑옷 → 바우블(바우블 모드가 있을 때만).
 * 원작과 다름: 1.7.10에는 왼손(오프핸드)이 없으므로 LEFT_HAND는 무시한다.
 */
public class FluxControllerHandler extends BasicPointHandler<TileFluxController> {

    /**
     * 이미 어떤 컨트롤러가 충전하고 있는 플레이어. 한 플레이어는 한 네트워크만 충전한다.
     * 원작과 다름: 원작은 EntityPlayerMP를 담아 리스폰·로그아웃한 플레이어 참조가 남을 수 있어 UUID로 담는다.
     * 서버 종료 때 clearChargingPlayers로 비운다.
     */
    private static final Set<UUID> CHARGING_PLAYERS = new HashSet<>();

    /** 이 컨트롤러가 충전하는 플레이어. 32틱마다 다시 만든다 */
    private final List<EntityPlayerMP> players = new ArrayList<>();
    /** updatePlayers 때 읽은 NETWORK_WIRELESS 설정 */
    private int wireless;
    private int timer;

    public FluxControllerHandler(TileFluxController fluxController) {
        super(fluxController);
    }

    @Override
    public void onCycleStart() {
        if (!device.isActive() || !WirelessType.ENABLE_WIRELESS.isActivated(device.getNetwork())) {
            demand = 0;
            clearPlayers();
            return;
        }
        if (timer == 0) {
            updatePlayers();
        }
        if ((timer & 0x3) == 2) {
            // 다른 틱에는 demand를 그대로 둔다 (버퍼를 미리 채운다)
            demand = chargeAllItems(device.getLogicLimit(), true);
        }
    }

    @Override
    public void onCycleEnd() {
        super.onCycleEnd();
        timer = ++timer & 0x1f;
    }

    @Override
    protected long sendToConsumers(long energy) {
        if (!device.isActive()) {
            return 0;
        }
        if ((timer & 0x3) > 0) {
            return 0;
        }
        if (!WirelessType.ENABLE_WIRELESS.isActivated(device.getNetwork())) {
            return 0;
        }
        // 원작은 BasicPointHandler.onCycleEnd에서 min(buffer, logicLimit)로 자른다. 현재 구조는 버퍼 전체를 넘기므로 여기서 자른다
        return chargeAllItems(Math.min(energy, device.getLogicLimit()), false);
    }

    /** 컨트롤러는 인접 블록과 에너지를 주고받지 않는다 */
    @Override
    public void updateTransfers(@Nonnull ForgeDirection... faces) {}

    @Override
    public void reset() {
        super.reset();
        clearPlayers();
    }

    /** 서버 종료 때 부른다 (싱글플레이에서 월드를 다시 열 때 남은 UUID 제거) */
    public static void clearChargingPlayers() {
        CHARGING_PLAYERS.clear();
    }

    /**
     * @return 넣은 (simulate면 넣을 수 있는) EU
     */
    private long chargeAllItems(long energy, boolean simulate) {
        if (energy <= 0) {
            return 0;
        }
        long leftover = energy;
        for (EntityPlayerMP player : players) {
            // 죽었거나 접속을 끊음 (다음 updatePlayers에서 새 객체로 바뀐다)
            if (!player.isEntityAlive()) {
                continue;
            }
            leftover = chargePlayer(player, leftover, simulate);
            if (leftover <= 0) {
                return energy;
            }
        }
        return energy - leftover;
    }

    /**
     * @return 남은 EU
     */
    private long chargePlayer(EntityPlayerMP player, long leftover, boolean simulate) {
        InventoryPlayer inv = player.inventory;
        ItemStack heldItem = inv.getCurrentItem();
        if (WirelessType.RIGHT_HAND.isActivated(wireless)) {
            leftover = chargeStack(heldItem, leftover, simulate);
            if (leftover <= 0) {
                return 0;
            }
        }
        if (WirelessType.HOT_BAR.isActivated(wireless)) {
            for (int i = 0; i < 9 && i < inv.mainInventory.length; i++) {
                ItemStack stack = inv.mainInventory[i];
                if (stack == heldItem) {
                    continue;
                }
                leftover = chargeStack(stack, leftover, simulate);
                if (leftover <= 0) {
                    return 0;
                }
            }
        }
        if (WirelessType.ARMOR_SLOT.isActivated(wireless)) {
            for (ItemStack stack : inv.armorInventory) {
                leftover = chargeStack(stack, leftover, simulate);
                if (leftover <= 0) {
                    return 0;
                }
            }
        }
        if (WirelessType.BAUBLES.isActivated(wireless) && FluxNetworks.proxy.baublesLoaded) {
            IInventory baubles = BaublesIntegration.getBaubles(player);
            if (baubles != null) {
                // 슬롯 내용을 바꾸지 않고 NBT만 고친다 (setInventorySlotContents는 장착 해제·장착 이벤트를 부르므로 쓰지 않는다)
                for (int i = 0; i < baubles.getSizeInventory(); i++) {
                    leftover = chargeStack(baubles.getStackInSlot(i), leftover, simulate);
                    if (leftover <= 0) {
                        return 0;
                    }
                }
            }
        }
        return leftover;
    }

    private static long chargeStack(ItemStack stack, long leftover, boolean simulate) {
        if (stack != null && ItemEnergy.canCharge(stack)) {
            leftover -= ItemEnergy.charge(stack, leftover, simulate);
        }
        return leftover;
    }

    private void clearPlayers() {
        if (!players.isEmpty()) {
            for (EntityPlayerMP toRemove : players) {
                CHARGING_PLAYERS.remove(
                    toRemove.getGameProfile()
                        .getId());
            }
            players.clear();
        }
    }

    private void updatePlayers() {
        clearPlayers();
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }
        IFluxNetwork network = device.getNetwork();
        wireless = network.getSetting(NetworkSettings.NETWORK_WIRELESS);
        List<NetworkMember> members = network.getSetting(NetworkSettings.NETWORK_PLAYERS);
        if (members == null || members.isEmpty()) {
            return;
        }
        Set<UUID> memberIds = new HashSet<>();
        for (NetworkMember member : members) {
            memberIds.add(member.getPlayerUUID());
        }
        for (Object o : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) o;
            UUID id = player.getGameProfile()
                .getId();
            if (!memberIds.contains(id) || CHARGING_PLAYERS.contains(id)) {
                continue;
            }
            players.add(player);
            CHARGING_PLAYERS.add(id);
        }
    }
}
