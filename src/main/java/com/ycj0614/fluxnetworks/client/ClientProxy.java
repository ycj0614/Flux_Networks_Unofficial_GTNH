package com.ycj0614.fluxnetworks.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.CommonProxy;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ClientProxy extends CommonProxy {

    // TODO 2단계: 원작의 피드백 필드 — EnumFeedbackInfo feedbackInfo = NONE (텍스트 메시지), feedbackInfoSuccess = NONE (특수 동작), int
    // feedbackTimer = 0

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
        // TODO 3단계: 원작의 FluxColorHandler.reset() 호출 — 클라이언트 네트워크 색상 캐시 초기화
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

    // TODO 3단계: 원작의 onRenderTick(TickEvent.RenderTickEvent) — 매 렌더 틱마다 FluxColorHandler.sendRequests()로 색상 요청 패킷 전송

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            runClientTasks();
        }
        // TODO 2단계: 원작의 피드백 타이머 — Phase.END에서 feedbackInfo.hasFeedback()이면 feedbackTimer를 올리고 60틱이 되면
        // setFeedback(EnumFeedbackInfo.NONE, false)
    }

    // TODO 2단계: 원작의 getFeedback(boolean) / setFeedback(EnumFeedbackInfo, boolean) 오버라이드 — operation 여부에 따라
    // feedbackInfoSuccess / feedbackInfo 조회·설정, 설정 시 feedbackTimer = 0

    // TODO 3단계: 원작의 receiveColorCache(Map<Integer, Tuple<Integer, String>>) 오버라이드 —
    // FluxColorHandler.receiveCache(cache) 호출

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
