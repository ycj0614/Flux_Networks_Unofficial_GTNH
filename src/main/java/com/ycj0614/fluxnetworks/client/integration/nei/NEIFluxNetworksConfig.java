package com.ycj0614.fluxnetworks.client.integration.nei;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.registry.RegistryItems;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 JEIIntegration. NEI가 이름이 "NEI…Config"이고 IConfigureNEI를 구현한 클래스를 찾아 불러오므로 따로 등록하지 않는다.
 * NEI가 없으면 이 클래스는 불러오지 않는다. 이 클래스를 직접 부를 때는 Loader.isModLoaded("NotEnoughItems")를 먼저 확인한다.
 * 제작대·GT 레시피(배치 W)는 NEI 기본 핸들러와 GT5U 핸들러가 보여주므로 여기서는 흑요석 좌클릭 제작만 등록한다.
 */
@SideOnly(Side.CLIENT)
public class NEIFluxNetworksConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        if (!FluxConfig.enableFluxRecipe) {
            return;
        }
        FluxCraftingHandler handler = new FluxCraftingHandler();
        API.registerRecipeHandler(handler);
        API.registerUsageHandler(handler);
        API.addRecipeCatalyst(new ItemStack(RegistryItems.FLUX), FluxCraftingHandler.HANDLER_ID);
    }

    @Override
    public String getName() {
        return FluxNetworks.NAME;
    }

    @Override
    public String getVersion() {
        return FluxNetworks.VERSION;
    }

    /**
     * 탭 아이콘과 핸들러 크기(원작 JEI 배경 128×80)를 NEI에 알린다. 플러그인 로딩보다 먼저 처리되도록 init에서 IMC로 보낸다.
     */
    public static void sendHandlerInfo() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("handler", FluxCraftingHandler.HANDLER_ID);
        tag.setString("modName", FluxNetworks.NAME);
        tag.setString("modId", FluxNetworks.MODID);
        tag.setBoolean("modRequired", true);
        tag.setString("itemName", FluxNetworks.MODID + ":flux");
        tag.setInteger("handlerWidth", FluxCraftingHandler.WIDTH);
        tag.setInteger("handlerHeight", FluxCraftingHandler.HEIGHT);
        tag.setInteger("maxRecipesPerPage", 1);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerHandlerInfo", tag);
    }
}
