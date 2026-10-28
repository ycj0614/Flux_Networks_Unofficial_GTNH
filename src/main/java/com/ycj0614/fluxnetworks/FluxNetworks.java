package com.ycj0614.fluxnetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ycj0614.fluxnetworks.common.CommonProxy;
import com.ycj0614.fluxnetworks.common.command.CommandFluxDebug;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

@Mod(
    modid = FluxNetworks.MODID,
    name = FluxNetworks.NAME,
    version = FluxNetworks.VERSION,
    dependencies = "required-after:gregtech",
    acceptedMinecraftVersions = "[1.7.10]")
public class FluxNetworks {

    // 템플릿의 Tags에는 VERSION만 생성되므로 MODID와 NAME은 직접 정의한다.
    // MODID는 gradle.properties의 modId와 같아야 한다.
    public static final String MODID = "fluxnetworks";
    public static final String NAME = "Flux Networks";
    public static final String VERSION = Tags.VERSION;

    @Mod.Instance(MODID)
    public static FluxNetworks instance;

    public static Logger logger = LogManager.getLogger("FluxNetworks");

    @SidedProxy(
        clientSide = "com.ycj0614.fluxnetworks.client.ClientProxy",
        serverSide = "com.ycj0614.fluxnetworks.common.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // 1단계 임시 디버그 명령 (TODO 2단계 완료 후 삭제)
        event.registerServerCommand(new CommandFluxDebug());
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        proxy.onServerStarted();
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        proxy.onServerStopped();
    }
}
