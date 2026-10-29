package com.ycj0614.fluxnetworks.common;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.capabilities.SuperAdminData;
import com.ycj0614.fluxnetworks.common.capabilities.SuperAdminHandler;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkInvalid;
import com.ycj0614.fluxnetworks.common.event.FluxConnectionEvent;
import com.ycj0614.fluxnetworks.common.handler.GuiHandler;
import com.ycj0614.fluxnetworks.common.handler.PacketHandler;
import com.ycj0614.fluxnetworks.common.handler.TileEntityHandler;
import com.ycj0614.fluxnetworks.common.network.PacketColorCache;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdate;
import com.ycj0614.fluxnetworks.common.network.PacketSuperAdmin;
import com.ycj0614.fluxnetworks.common.registry.RegistryBlocks;
import com.ycj0614.fluxnetworks.common.registry.RegistryItems;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CommonProxy {

    /** 관리자 구성기 GUI에서 SuperAdmin이 열람 중인 네트워크 (클라이언트에서 쓴다. 이름은 원작 그대로) */
    public int admin_viewing_network_id = -1;
    public boolean detailed_network_view;
    public IFluxNetwork admin_viewing_network = FluxNetworkInvalid.instance;

    /** 1.7.10에는 addScheduledTask가 없으므로, 패킷 처리 등 메인 스레드 작업을 여기에 넣고 ServerTickEvent START에서 실행한다 */
    private final Queue<Runnable> serverTasks = new ConcurrentLinkedQueue<>();

    public void preInit(FMLPreInitializationEvent event) {
        // TickEvent, PlayerEvent 등 cpw.mods.fml.common.gameevent.* → FML 버스
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        // FluxConnectionEvent, EntityJoinWorldEvent 등 net.minecraftforge.event.* → Forge 버스
        MinecraftForge.EVENT_BUS.register(this);
        PacketHandler.registerMessages();
        FluxConfig.init(event.getModConfigurationDirectory());
        RegistryBlocks.register();
        RegistryItems.register();
        TileEntityHandler.registerTileEntity();
        // TODO 3단계: 원작의 EntityRegistry.registerModEntity(EntityFireItem.class, "Flux", 0, FluxNetworks.instance, 64,
        // 10, true) 호출 — 불 속 레드스톤 → 플럭스 변환용 아이템 엔티티 등록
    }

    public void init(FMLInitializationEvent event) {
        // 원작 DefaultSuperAdmin.register()(캐퍼빌리티 등록) 대체: 플레이어 생성 시 SuperAdminData를 붙인다 (Forge 버스)
        MinecraftForge.EVENT_BUS.register(new SuperAdminHandler());
        NetworkRegistry.INSTANCE.registerGuiHandler(FluxNetworks.instance, new GuiHandler());
    }

    public void postInit(FMLPostInitializationEvent event) {
        // TODO 3단계: 원작의 ForgeChunkManager.setForcedChunkLoadingCallback(FluxNetworks.instance,
        // FluxChunkManager::callback) 호출 — 플럭스 타일 청크 로딩 콜백 등록
    }

    public void onServerStarted() {

    }

    public void onServerStopped() {
        FluxNetworkCache.instance.clearNetworks();
        // TODO 3단계: 원작의 FluxChunkManager.clear() 호출 — 청크 로딩 티켓 캐시 초기화
        serverTasks.clear();
    }

    public static CreativeTabs creativeTabs = new CreativeTabs(FluxNetworks.MODID) {

        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return Item.getItemFromBlock(RegistryBlocks.FLUX_PLUG);
        }
    };

    // TODO 3단계: 원작의 onPlayerInteract(PlayerInteractEvent.LeftClickBlock) — 서버에서 enableFluxRecipe일 때, 좌클릭한 흑요석의 2칸 아래가
    // 기반암이면
    // 그 사이 칸의 레드스톤 EntityItem을 모두 제거하고 같은 개수의 Flux를 떨어뜨린다. 흑요석은 한 칸 아래로 옮기고 폭발음 재생 후 이벤트 취소 (receiveCanceled = true)

    // TODO 3단계: 원작의 onEntyAdded(EntityJoinWorldEvent, priority LOWEST) — enableFluxRecipe && enableOldRecipe일 때 서버에서
    // 월드에 들어오는 레드스톤 EntityItem을
    // EntityFireItem(불에 닿으면 Flux로 변하는 아이템 엔티티)으로 바꿔 직접 청크·월드에 추가하고, 원래 엔티티 스폰은 취소

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            runServerTasks();
        }
        /*
         * if(event.phase == TickEvent.Phase.START) {
         * for(IFluxNetwork network : FluxNetworkCache.instance.getAllNetworks()) {
         * network.onStartServerTick();
         * }
         * }
         */
        if (event.phase == TickEvent.Phase.END) {
            for (IFluxNetwork network : FluxNetworkCache.instance.getAllNetworks()) {
                network.onEndServerTick();
            }
        }
    }

    /** FML 버스. 서버 메인 스레드에서 호출된다 (플레이어 NBT, 즉 SuperAdminData는 이미 읽힌 뒤) */
    @SubscribeEvent
    public void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            PacketHandler.network.sendTo(
                new PacketNetworkUpdate.NetworkUpdateMessage(
                    new ArrayList<>(FluxNetworkCache.instance.getAllNetworks()),
                    NBTType.NETWORK_GENERAL),
                playerMP);
            PacketHandler.network
                .sendTo(new PacketSuperAdmin.SuperAdminMessage(SuperAdminData.isPlayerSuperAdmin(player)), playerMP);
        }
    }

    @SubscribeEvent
    public void onFluxConnected(FluxConnectionEvent.Connected event) {
        if (!event.flux.getFluxWorld().isRemote) {
            event.flux.connect(event.network);
        }
    }

    @SubscribeEvent
    public void onFluxDisconnect(FluxConnectionEvent.Disconnected event) {
        if (!event.flux.getFluxWorld().isRemote) {
            event.flux.disconnect(event.network);
        }
    }

    //// 클라이언트 위임 메서드: 패킷 클래스가 클라이언트 클래스를 import하지 않도록 프록시를 거친다. 서버 쪽은 빈 구현 \\\\

    public EnumFeedbackInfo getFeedback(boolean operation) {
        return null;
    }

    public void setFeedback(EnumFeedbackInfo info, boolean operation) {}

    public void setGuiAccessPermission(AccessLevel accessPermission) {}

    public AccessLevel getGuiAccessPermission() {
        return AccessLevel.NONE;
    }

    public void onSuperAdminChanged() {}

    /** PacketColorCache 수신 (클라이언트 메인 스레드) */
    public void receiveColorCache(Map<Integer, PacketColorCache.Entry> cache) {}

    public EntityPlayer getPlayer(MessageContext ctx) {
        return ctx.getServerHandler().playerEntity;
    }

    /** 서버 메인 스레드에서 실행할 작업을 등록한다. 어느 스레드에서 호출해도 된다 */
    public void scheduleServerTask(Runnable task) {
        serverTasks.add(task);
    }

    /** 클라이언트 메인 스레드에서 실행할 작업을 등록한다. 서버 전용 프록시에서는 실행하지 않는다 */
    public void scheduleClientTask(Runnable task) {
        FluxNetworks.logger.warn("scheduleClientTask called on a dedicated server; task ignored");
    }

    private void runServerTasks() {
        Runnable task;
        while ((task = serverTasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                FluxNetworks.logger.error("Error while running scheduled server task", e);
            }
        }
    }
}
