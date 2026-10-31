package com.ycj0614.fluxnetworks.common.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.Constants;

import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.client.FluxColorHandler;
import com.ycj0614.fluxnetworks.common.block.BlockFluxStorage;
import com.ycj0614.fluxnetworks.common.connection.transfer.FluxStorageHandler;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.core.GTEnergy;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPlug;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxPoint;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 커넥터(플러그·포인트·스토리지) 전용 ItemBlock. 부순 장치의 데이터({@link FluxUtils#FLUX_DATA}, TILE_DROP)로 이름과 툴팁을 보여준다.
 * GameRegistry.registerBlock(block, ItemFluxConnector.class, name)으로 등록하며, 생성자는 (Block) 하나여야 한다.
 */
public class ItemFluxConnector extends ItemBlock {

    public ItemFluxConnector(Block block) {
        super(block);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        NBTTagCompound tag = getFluxData(stack);
        if (tag != null) {
            String name = tag.getString(TileFluxCore.NBT_CUSTOM_NAME);
            if (!name.isEmpty()) {
                return name;
            }
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        Block block = Block.getBlockFromItem(stack.getItem());
        BlockFluxStorage storage = block instanceof BlockFluxStorage ? (BlockFluxStorage) block : null;
        if (storage != null) {
            tooltip.add(FluxTranslate.FLUX_STORAGE_TOOLTIP.t());
        }
        NBTTagCompound tag = getFluxData(stack);
        if (tag != null) {
            int networkID = tag.hasKey(FluxNetworkData.NETWORK_ID) ? tag.getInteger(FluxNetworkData.NETWORK_ID) : -1;
            tooltip.add(
                FluxTranslate.NETWORK_FULL_NAME.t() + ": "
                    + EnumChatFormatting.WHITE
                    + FluxColorHandler.getOrRequestNetworkName(networkID));
            tooltip.add(
                FluxTranslate.TRANSFER_LIMIT.t() + ": "
                    + EnumChatFormatting.WHITE
                    + FluxUtils.format(tag.getLong(TileFluxCore.NBT_LIMIT), FluxUtils.TypeNumberFormat.COMMAS, true));
            tooltip.add(
                FluxTranslate.PRIORITY.t() + ": "
                    + EnumChatFormatting.WHITE
                    + tag.getInteger(TileFluxCore.NBT_PRIORITY));
            // EU 설정 (원작에 없음)
            if (tag.hasKey(TileFluxPlug.NBT_MAX_INPUT_TIER)) {
                tooltip.add(
                    FluxTranslate.MAX_INPUT_TIER.t() + ": "
                        + EnumChatFormatting.WHITE
                        + formatTier(tag.getInteger(TileFluxPlug.NBT_MAX_INPUT_TIER)));
            }
            if (tag.hasKey(TileFluxPoint.NBT_OUTPUT_TIER)) {
                tooltip.add(
                    FluxTranslate.OUTPUT_TIER.t() + ": "
                        + EnumChatFormatting.WHITE
                        + formatTier(tag.getInteger(TileFluxPoint.NBT_OUTPUT_TIER)));
            }
            if (tag.hasKey(TileFluxPoint.NBT_OUTPUT_AMPERAGE)) {
                tooltip.add(
                    FluxTranslate.OUTPUT_AMPERAGE.t() + ": "
                        + EnumChatFormatting.WHITE
                        + FluxUtils.format(
                            Math.max(1L, tag.getLong(TileFluxPoint.NBT_OUTPUT_AMPERAGE)),
                            FluxUtils.TypeNumberFormat.COMMAS,
                            "A"));
            }
            if (storage != null) {
                // 원작은 저장량만 표시했다. 여기서는 용량도 함께 보여준다
                tooltip.add(
                    FluxTranslate.ENERGY_STORED.t() + ": "
                        + EnumChatFormatting.WHITE
                        + FluxUtils
                            .format(tag.getLong(FluxStorageHandler.NBT_ENERGY), FluxUtils.TypeNumberFormat.COMMAS, "")
                        + " / "
                        + FluxUtils.format(storage.getMaxStorage(), FluxUtils.TypeNumberFormat.COMMAS, false));
            } else {
                tooltip.add(
                    FluxTranslate.INTERNAL_BUFFER.t() + ": "
                        + EnumChatFormatting.WHITE
                        + FluxUtils.format(tag.getLong("buffer"), FluxUtils.TypeNumberFormat.COMMAS, false));
            }
        }
        super.addInformation(stack, player, tooltip, advanced);
    }

    /** "LV (32 EU/t)" 형식 */
    private static String formatTier(int tier) {
        return GTEnergy.tierName(tier) + " ("
            + FluxUtils.format(GTEnergy.voltage(tier), FluxUtils.TypeNumberFormat.COMMAS, true)
            + ")";
    }

    @Nullable
    private static NBTTagCompound getFluxData(ItemStack stack) {
        if (stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(FluxUtils.FLUX_DATA, Constants.NBT.TAG_COMPOUND)) {
            return stack.getTagCompound()
                .getCompoundTag(FluxUtils.FLUX_DATA);
        }
        return null;
    }
}
