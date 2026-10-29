package com.ycj0614.fluxnetworks.client;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.gui.EnumFeedbackInfo;
import com.ycj0614.fluxnetworks.api.network.AccessLevel;
import com.ycj0614.fluxnetworks.common.CommonProxy;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;
import com.ycj0614.fluxnetworks.common.network.PacketColorCache;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy {

    /** 텍스트로 보여 줄 피드백 */
    public EnumFeedbackInfo feedbackInfo = EnumFeedbackInfo.NONE;
    /** 특수 동작용 피드백 (SUCCESS, SUCCESS_2, PASSWORD_REQUIRE) */
    public EnumFeedbackInfo feedbackInfoSuccess = EnumFeedbackInfo.NONE;
    public int feedbackTimer = 0;

    /** PacketGUIPermission으로 받은, 열린 GUI의 접근 권한 */
    private AccessLevel guiAccessPermission = AccessLevel.NONE;

    /** 네티 스레드 등에서 들어온 작업을 클라이언트 메인 스레드에서 실행하기 위한 큐. ClientTickEvent START에서 처리 */
    private final Queue<Runnable> clientTasks = new ConcurrentLinkedQueue<>();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // super.preInit()이 this(= ClientProxy 인스턴스)를 FML 버스와 Forge 버스에 등록하므로
        // 이 클래스의 @SubscribeEvent 메서드도 이미 등록된다. 여기서 다시 등록하지 않는다.
        super.preInit(event);
        // TODO 3단계: 원작의 ClientRegistry.bindTileEntitySpecialRenderer(TileFluxStorage.class, new
        // TileFluxStorageRenderer()) 호출 — 플럭스 스토리지 에너지 표시 렌더러 등록
    }

    @Override
    public void onServerStopped() {
        super.onServerStopped();
        FluxColorHandler.reset();
        FluxNetworkCache.instance.clearClientCache();
    }

    @SubscribeEvent
    public void clientFlux(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // FMLServerStoppedEvent is not fired on the client side when disconnecting from a server
        // 이 이벤트는 네티 스레드에서 올 수 있으므로 클라이언트 메인 스레드로 넘긴다
        scheduleClientTask(this::onServerStopped);
    }

    // TODO 3단계: 원작의 registerBlockColor / registerItemColor(ColorHandlerEvent) — FLUX_CONTROLLER, FLUX_POINT, FLUX_PLUG,
    // FLUX_STORAGE_1~3 블록·아이템과
    // FLUX_CONFIGURATOR에 FluxColorHandler 색상 핸들러 등록 (1.7.10에는 ColorHandlerEvent가 없음. 블록 colorMultiplier 등으로 대체)

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            runClientTasks();
        }
        if (event.phase == TickEvent.Phase.END) {
            // 원작과 다름: 원작은 RenderTickEvent마다 호출했다 (프레임 수에 따라 요청 주기가 달라짐)
            FluxColorHandler.sendRequests();
            if (feedbackInfo.hasFeedback()) {
                feedbackTimer++;
                if (feedbackTimer >= 60) {
                    feedbackTimer = 0;
                    setFeedback(EnumFeedbackInfo.NONE, false);
                }
            }
        }
    }

    @Override
    public EnumFeedbackInfo getFeedback(boolean operation) {
        return operation ? feedbackInfoSuccess : feedbackInfo;
    }

    @Override
    public void setFeedback(EnumFeedbackInfo info, boolean operation) {
        if (operation) {
            this.feedbackInfoSuccess = info;
        } else {
            this.feedbackInfo = info;
        }
        this.feedbackTimer = 0;
    }

    @Override
    public void setGuiAccessPermission(AccessLevel accessPermission) {
        this.guiAccessPermission = accessPermission;
        // TODO 배치 K: 열린 GuiFluxCore.accessPermission에 반영하고 onSuperAdminChanged() 호출
        // (원작: Minecraft.getMinecraft().currentScreen이 GuiFluxCore면 guiFluxCore.accessPermission = 값;
        // guiFluxCore.onSuperAdminChanged())
    }

    @Override
    public AccessLevel getGuiAccessPermission() {
        return guiAccessPermission;
    }

    @Override
    public void onSuperAdminChanged() {
        // TODO 배치 K: 열린 GuiFluxCore.onSuperAdminChanged() 호출
        // (원작: Minecraft.getMinecraft().currentScreen이 GuiFluxCore면 guiFluxCore.onSuperAdminChanged())
    }

    @Override
    public void receiveColorCache(Map<Integer, PacketColorCache.Entry> cache) {
        FluxColorHandler.receiveCache(cache);
    }

    @Override
    public EntityPlayer getPlayer(MessageContext ctx) {
        return ctx.side.isServer() ? super.getPlayer(ctx) : Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public void scheduleClientTask(Runnable task) {
        clientTasks.add(task);
    }

    private void runClientTasks() {
        Runnable task;
        while ((task = clientTasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                FluxNetworks.logger.error("Error while running scheduled client task", e);
            }
        }
    }
}
