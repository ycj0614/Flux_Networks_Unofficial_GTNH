package com.ycj0614.fluxnetworks.common.handler;

import net.minecraft.entity.player.EntityPlayer;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.network.PacketActivateSuperAdmin;
import com.ycj0614.fluxnetworks.common.network.PacketBatchEditing;
import com.ycj0614.fluxnetworks.common.network.PacketByteBuf;
import com.ycj0614.fluxnetworks.common.network.PacketColorCache;
import com.ycj0614.fluxnetworks.common.network.PacketColorRequest;
import com.ycj0614.fluxnetworks.common.network.PacketConfiguratorSettings;
import com.ycj0614.fluxnetworks.common.network.PacketConnectionUpdate;
import com.ycj0614.fluxnetworks.common.network.PacketConnectionUpdateRequest;
import com.ycj0614.fluxnetworks.common.network.PacketFeedback;
import com.ycj0614.fluxnetworks.common.network.PacketGUIPermission;
import com.ycj0614.fluxnetworks.common.network.PacketGeneral;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdate;
import com.ycj0614.fluxnetworks.common.network.PacketNetworkUpdateRequest;
import com.ycj0614.fluxnetworks.common.network.PacketPermissionRequest;
import com.ycj0614.fluxnetworks.common.network.PacketSetConfiguratorNetwork;
import com.ycj0614.fluxnetworks.common.network.PacketSuperAdmin;
import com.ycj0614.fluxnetworks.common.network.PacketTile;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class PacketHandler {

    public static SimpleNetworkWrapper network = NetworkRegistry.INSTANCE.newSimpleChannel(FluxNetworks.MODID);

    /**
     * 패킷 ID(discriminator)는 원작 번호를 그대로 고정한다.
     */
    public static void registerMessages() {
        network.registerMessage(PacketGeneral.class, PacketGeneral.GeneralMessage.class, 1, Side.SERVER);
        network
            .registerMessage(PacketNetworkUpdate.class, PacketNetworkUpdate.NetworkUpdateMessage.class, 2, Side.CLIENT);
        network.registerMessage(PacketByteBuf.class, PacketByteBuf.ByteBufMessage.class, 3, Side.SERVER);
        network.registerMessage(PacketByteBuf.class, PacketByteBuf.ByteBufMessage.class, 4, Side.CLIENT);
        network.registerMessage(PacketTile.class, PacketTile.TileMessage.class, 5, Side.SERVER);
        network.registerMessage(PacketFeedback.class, PacketFeedback.FeedbackMessage.class, 6, Side.CLIENT);
        network.registerMessage(PacketColorRequest.class, PacketColorRequest.ColorRequestMessage.class, 7, Side.SERVER);
        network.registerMessage(PacketColorCache.class, PacketColorCache.ColorCacheMessage.class, 8, Side.CLIENT);
        network.registerMessage(
            PacketNetworkUpdateRequest.class,
            PacketNetworkUpdateRequest.UpdateRequestMessage.class,
            9,
            Side.SERVER);
        network.registerMessage(
            PacketConnectionUpdate.class,
            PacketConnectionUpdate.NetworkConnectionMessage.class,
            10,
            Side.CLIENT);
        network.registerMessage(
            PacketConnectionUpdateRequest.class,
            PacketConnectionUpdateRequest.ConnectionRequestMessage.class,
            11,
            Side.SERVER);
        network
            .registerMessage(PacketBatchEditing.class, PacketBatchEditing.BatchEditingMessage.class, 12, Side.SERVER);
        network.registerMessage(
            PacketPermissionRequest.class,
            PacketPermissionRequest.PermissionRequestMessage.class,
            13,
            Side.SERVER);
        network.registerMessage(
            PacketGUIPermission.class,
            PacketGUIPermission.GUIPermissionMessage.class,
            14,
            Side.CLIENT);
        network.registerMessage(PacketSuperAdmin.class, PacketSuperAdmin.SuperAdminMessage.class, 15, Side.CLIENT);
        network.registerMessage(
            PacketActivateSuperAdmin.class,
            PacketActivateSuperAdmin.ActivateSuperAdminMessage.class,
            16,
            Side.SERVER);
        network.registerMessage(
            PacketConfiguratorSettings.class,
            PacketConfiguratorSettings.ConfiguratorSettingsMessage.class,
            17,
            Side.SERVER);
        network.registerMessage(
            PacketSetConfiguratorNetwork.class,
            PacketSetConfiguratorNetwork.SetConfiguratorNetworkMessage.class,
            18,
            Side.SERVER);
    }

    public static EntityPlayer getPlayer(MessageContext ctx) {
        return FluxNetworks.proxy.getPlayer(ctx);
    }

    /**
     * 1.7.10에는 addScheduledTask가 없다. onMessage(네티 스레드)에서 받은 작업을 해당 쪽 메인 스레드의 작업 큐로 넘긴다.
     * 서버 작업은 ServerTickEvent START, 클라이언트 작업은 ClientTickEvent START에서 실행된다.
     */
    public static void handlePacket(Runnable runnable, MessageContext ctx) {
        if (ctx.side.isServer()) {
            FluxNetworks.proxy.scheduleServerTask(runnable);
        } else {
            FluxNetworks.proxy.scheduleClientTask(runnable);
        }
    }
}
