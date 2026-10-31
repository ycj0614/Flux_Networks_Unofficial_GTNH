package com.ycj0614.fluxnetworks.common.registry;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.connection.transfer.FluxStorageHandler;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

/**
 * 원작 RegistryRecipes. 1.7.10에서는 postInit에서 register()를 부른다 (GT 재료·ItemList가 준비된 뒤).
 * 원작과 다름: FluxConfig.enableGTRecipes가 켜져 있으면 장치·아이템은 GT 조립기 레시피로, 스토리지는 GT 부품을 쓰는 제작대 레시피로 등록한다.
 * 스토리지는 에너지·네트워크를 이어받아야 하므로 두 모드 모두 제작대(FluxStorageRecipe)다.
 */
public class RegistryRecipes {

    public static void register() {
        RecipeSorter.register(
            FluxNetworks.MODID + ":shaped",
            FluxShapedRecipe.class,
            RecipeSorter.Category.SHAPED,
            "after:forge:shapedore");
        RecipeSorter.register(
            FluxNetworks.MODID + ":storage",
            FluxStorageRecipe.class,
            RecipeSorter.Category.SHAPED,
            "after:forge:shapedore");
        RecipeSorter.register(
            FluxNetworks.MODID + ":shapeless",
            FluxShapelessRecipe.class,
            RecipeSorter.Category.SHAPELESS,
            "after:forge:shapelessore");

        if (FluxConfig.enableGTRecipes) {
            registerGTRecipes();
        } else {
            registerCraftingRecipes();
        }

        // 장치 하나만 다시 조합하면 아이템 NBT(네트워크·설정·에너지)를 지운다
        addReset(RegistryBlocks.FLUX_CONTROLLER);
        addReset(RegistryBlocks.FLUX_POINT);
        addReset(RegistryBlocks.FLUX_PLUG);
        addReset(RegistryBlocks.FLUX_STORAGE_1);
        addReset(RegistryBlocks.FLUX_STORAGE_2);
        addReset(RegistryBlocks.FLUX_STORAGE_3);
    }

    /** 원작 레시피 그대로 */
    private static void registerCraftingRecipes() {
        GameRegistry.addRecipe(
            new FluxShapedRecipe(
                new ItemStack(RegistryItems.FLUX_CORE, 4),
                "GCG",
                "CAC",
                "GCG",
                'C',
                Blocks.obsidian,
                'G',
                RegistryItems.FLUX,
                'A',
                Items.ender_eye));
        GameRegistry.addRecipe(
            new FluxShapedRecipe(
                new ItemStack(RegistryItems.FLUX_CONFIGURATOR, 1),
                " CP",
                " OC",
                "O  ",
                'C',
                RegistryItems.FLUX,
                'P',
                Items.ender_pearl,
                'O',
                Blocks.obsidian));
        GameRegistry.addRecipe(
            new FluxShapedRecipe(
                new ItemStack(RegistryBlocks.FLUX_BLOCK, 1),
                "ACA",
                "CAC",
                "ACA",
                'A',
                RegistryItems.FLUX,
                'C',
                RegistryItems.FLUX_CORE));
        GameRegistry.addRecipe(
            new FluxShapedRecipe(
                new ItemStack(RegistryBlocks.FLUX_CONTROLLER, 1),
                "BCB",
                "R R",
                "BBB",
                'C',
                RegistryItems.FLUX_CORE,
                'B',
                RegistryBlocks.FLUX_BLOCK,
                'R',
                RegistryItems.FLUX));
        GameRegistry.addRecipe(
            new FluxShapedRecipe(
                new ItemStack(RegistryBlocks.FLUX_POINT, 1),
                " C ",
                "CBC",
                " C ",
                'C',
                RegistryItems.FLUX_CORE,
                'B',
                Blocks.redstone_block));
        GameRegistry.addRecipe(
            new FluxShapedRecipe(
                new ItemStack(RegistryBlocks.FLUX_PLUG, 1),
                " C ",
                "CBC",
                " C ",
                'C',
                RegistryItems.FLUX_CORE,
                'B',
                RegistryBlocks.FLUX_BLOCK));
        addStorageRecipes("paneGlassColorless", "paneGlassColorless", "paneGlassColorless");
    }

    /** 원작과 다름: GT 조립기 레시피. 승인된 재료·티어 표대로 */
    private static void registerGTRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(RegistryItems.FLUX, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Obsidian, 4),
                new ItemStack(Items.ender_eye, 1))
            .itemOutputs(new ItemStack(RegistryItems.FLUX_CORE, 4))
            .duration(10 * 20)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(RegistryItems.FLUX, 4), new ItemStack(RegistryItems.FLUX_CORE, 4))
            .itemOutputs(new ItemStack(RegistryBlocks.FLUX_BLOCK, 1))
            .duration(5 * 20)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                new ItemStack(RegistryItems.FLUX, 2),
                GTOreDictUnificator.get(OrePrefixes.stick, Materials.Obsidian, 2),
                new ItemStack(Items.ender_pearl, 1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.MV, 1))
            .itemOutputs(new ItemStack(RegistryItems.FLUX_CONFIGURATOR, 1))
            .duration(10 * 20)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_HV.get(1),
                new ItemStack(RegistryItems.FLUX_CORE, 4),
                ItemList.Sensor_HV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.HV, 2))
            .itemOutputs(new ItemStack(RegistryBlocks.FLUX_PLUG, 1))
            .duration(20 * 20)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_HV.get(1),
                new ItemStack(RegistryItems.FLUX_CORE, 4),
                ItemList.Emitter_HV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.HV, 2))
            .itemOutputs(new ItemStack(RegistryBlocks.FLUX_POINT, 1))
            .duration(20 * 20)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_EV.get(1),
                new ItemStack(RegistryBlocks.FLUX_BLOCK, 2),
                new ItemStack(RegistryItems.FLUX_CORE, 1),
                ItemList.Field_Generator_HV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 2))
            .itemOutputs(new ItemStack(RegistryBlocks.FLUX_CONTROLLER, 1))
            .duration(30 * 20)
            .eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.assemblerRecipes);
        // GTNH 회로 광물 사전 이름 (HV, EV, IV)
        addStorageRecipes("circuitAdvanced", "circuitData", "circuitElite");
    }

    /** 원작 모양 "BBB / G G / BBB". 단계마다 가운데 줄 재료가 다를 수 있다 */
    private static void addStorageRecipes(Object glass1, Object glass2, Object glass3) {
        GameRegistry.addRecipe(
            new FluxStorageRecipe(
                new ItemStack(RegistryBlocks.FLUX_STORAGE_1, 1),
                "BBB",
                "G G",
                "BBB",
                'B',
                RegistryBlocks.FLUX_BLOCK,
                'G',
                glass1));
        GameRegistry.addRecipe(
            new FluxStorageRecipe(
                new ItemStack(RegistryBlocks.FLUX_STORAGE_2, 1),
                "BBB",
                "G G",
                "BBB",
                'B',
                RegistryBlocks.FLUX_STORAGE_1,
                'G',
                glass2));
        GameRegistry.addRecipe(
            new FluxStorageRecipe(
                new ItemStack(RegistryBlocks.FLUX_STORAGE_3, 1),
                "BBB",
                "G G",
                "BBB",
                'B',
                RegistryBlocks.FLUX_STORAGE_2,
                'G',
                glass3));
    }

    private static void addReset(Block block) {
        GameRegistry.addRecipe(new FluxShapelessRecipe(new ItemStack(block, 1), block));
    }

    public static class FluxShapedRecipe extends ShapedOreRecipe {

        public FluxShapedRecipe(@Nonnull ItemStack result, Object... recipe) {
            super(result, recipe);
        }
    }

    public static class FluxShapelessRecipe extends ShapelessOreRecipe {

        public FluxShapelessRecipe(@Nonnull ItemStack result, Object... recipe) {
            super(result, recipe);
        }
    }

    /** 원작: 재료 스토리지들의 에너지를 합치고 첫 네트워크 ID를 결과에 넣는다 */
    public static class FluxStorageRecipe extends FluxShapedRecipe {

        public FluxStorageRecipe(@Nonnull ItemStack result, Object... recipe) {
            super(result, recipe);
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting crafting) {
            long energyTotal = 0;
            int networkID = 0;
            boolean firstFound = false;
            for (int i = 0; i < crafting.getSizeInventory(); i++) {
                ItemStack stack = crafting.getStackInSlot(i);
                if (stack == null || !stack.hasTagCompound()) {
                    continue;
                }
                NBTTagCompound root = stack.getTagCompound();
                if (!root.hasKey(FluxUtils.FLUX_DATA)) {
                    continue;
                }
                NBTTagCompound subTag = root.getCompoundTag(FluxUtils.FLUX_DATA);
                if (!firstFound) {
                    networkID = subTag.getInteger(FluxNetworkData.NETWORK_ID);
                    firstFound = true;
                }
                energyTotal += subTag.getLong(FluxStorageHandler.NBT_ENERGY);
            }
            if (firstFound) {
                ItemStack stack = getRecipeOutput().copy();
                NBTTagCompound subTag = new NBTTagCompound();
                subTag.setInteger(FluxNetworkData.NETWORK_ID, networkID);
                subTag.setLong(FluxStorageHandler.NBT_ENERGY, energyTotal);
                NBTTagCompound root = new NBTTagCompound();
                root.setTag(FluxUtils.FLUX_DATA, subTag);
                stack.setTagCompound(root);
                return stack;
            }
            return super.getCraftingResult(crafting);
        }
    }
}
