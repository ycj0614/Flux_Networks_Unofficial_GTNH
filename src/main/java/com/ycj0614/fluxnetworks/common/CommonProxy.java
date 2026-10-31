package com.ycj0614.fluxnetworks.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

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
import com.ycj0614.fluxnetworks.common.core.EntityFireItem;
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
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.EntityRegistry;
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
        // 불 속 레드스톤 → 플럭스 변환용 아이템 엔티티. 이름 "Flux"·ID 0은 월드 저장에 기록되므로 바꾸지 않는다
        EntityRegistry.registerModEntity(EntityFireItem.class, "Flux", 0, FluxNetworks.instance, 64, 10, true);
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

    /**
     * 원작 onPlayerInteract(LeftClickBlock). 좌클릭한 흑요석의 2칸 아래가 기반암이면 그 사이 칸의 레드스톤 아이템을 모두 없애고
     * 같은 개수의 Flux를 떨어뜨린다. 흑요석은 한 칸 아래로 옮긴다 (Forge 버스)
     */
    @SubscribeEvent(receiveCanceled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            return;
        }
        World world = event.world;
        if (world.isRemote || !FluxConfig.enableFluxRecipe) {
            return;
        }
        int x = event.x, y = event.y, z = event.z;
        if (world.getBlock(x, y, z) != Blocks.obsidian || world.getBlock(x, y - 2, z) != Blocks.bedrock) {
            return;
        }
        List<?> entities = world
            .getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(x, y - 1, z, x + 1, y, z + 1));
        List<EntityItem> redstones = new ArrayList<>();
        int count = 0;
        for (Object o : entities) {
            EntityItem e = (EntityItem) o;
            ItemStack stack = e.getEntityItem();
            if (!e.isDead && stack != null && stack.getItem() == Items.redstone) {
                redstones.add(e);
                count += stack.stackSize;
            }
        }
        if (redstones.isEmpty()) {
            return;
        }
        for (EntityItem e : redstones) {
            e.setDead();
        }
        world.setBlockToAir(x, y, z);
        // 원작과 다름: 원작은 개수와 상관없이 한 스택으로 떨어뜨린다. 최대 스택 크기로 나눠 떨어뜨린다
        int maxStack = RegistryItems.FLUX.getItemStackLimit();
        while (count > 0) {
            int size = Math.min(count, maxStack);
            count -= size;
            world.spawnEntityInWorld(
                new EntityItem(world, x + 0.5, y, z + 0.5, new ItemStack(RegistryItems.FLUX, size)));
        }
        world.setBlock(x, y - 1, z, Blocks.obsidian);
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.explode", 1.0F, 1.0F);
        event.setCanceled(true);
    }

    /**
     * 원작 onEntyAdded. enableOldRecipe일 때 월드에 들어오는 레드스톤 아이템을 EntityFireItem으로 바꾼다 (Forge 버스).
     * <p>
     * 원작과 다름: 원작은 이벤트 안에서 새 엔티티를 청크·월드 목록에 직접 넣고 스폰을 취소한다. 1.7.10에서는 청크 로드 중에도
     * 이 이벤트가 불리는데, 그때 같은 청크의 엔티티 목록에 추가하면 ConcurrentModificationException이 날 수 있다.
     * 그래서 다음 서버 틱 작업으로 미뤄 교체한다 (1틱 늦지만 아이템 체력 5라 불 속 1틱은 버틴다)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityAdded(EntityJoinWorldEvent event) {
        if (!FluxConfig.enableFluxRecipe || !FluxConfig.enableOldRecipe || event.world.isRemote) {
            return;
        }
        if (!(event.entity instanceof EntityItem) || event.entity instanceof EntityFireItem) {
            return;
        }
        final EntityItem entityItem = (EntityItem) event.entity;
        ItemStack stack = entityItem.getEntityItem();
        if (stack == null || stack.getItem() != Items.redstone) {
            return;
        }
        scheduleServerTask(() -> {
            if (entityItem.isDead) {
                return;
            }
            ItemStack current = entityItem.getEntityItem();
            if (current == null || current.getItem() != Items.redstone) {
                return;
            }
            EntityFireItem newEntity = new EntityFireItem(entityItem);
            entityItem.setDead();
            entityItem.worldObj.spawnEntityInWorld(newEntity);
        });
    }

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

    /** GuiHandler.getClientGuiElement 위임. 서버에서는 호출되지 않는다 */
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

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
