package com.ycj0614.fluxnetworks.common.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.CommonProxy;
import com.ycj0614.fluxnetworks.common.core.FluxUtils;
import com.ycj0614.fluxnetworks.common.handler.GuiHandler;
import com.ycj0614.fluxnetworks.common.item.ItemConfigurator;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxConnector;
import com.ycj0614.fluxnetworks.common.tileentity.TileFluxCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 BlockCore + BlockFluxCore + BlockSidedConnection 통합.
 * 1.7.10에는 블록 상태가 없으므로 연결 여부는 타일의 connected / connections[]로만 판단한다.
 * <p>
 * TODO 3단계: 원작 모양 렌더링 (ISBRH, 경계 상자 플러그 0.25~0.75, 포인트 0.375~0.625).
 * 원작의 getActualState, createBlockState, getStateFromMeta, getMetaFromState, getBoundingBox, getRenderLayer,
 * isOpaqueCube/isNormalCube/isFullCube 오버라이드는 1단계에서 삭제했다 (일반 불투명 정육면체로 렌더링).
 * <p>
 * 아이템 이름·툴팁은 ItemFluxConnector가 맡는다.
 */
public abstract class BlockFluxCore extends Block {

    /** 원작 FluxTranslate.REMOVAL_DENIED_KEY 와 같은 값 (lang에 이미 있음) */
    public static final String REMOVAL_DENIED_KEY = "info.fluxnetworks.denied.removal";

    private final String iconName;

    @SideOnly(Side.CLIENT)
    protected IIcon iconOn;

    @SideOnly(Side.CLIENT)
    protected IIcon iconOff;

    /**
     * @param name     번역 키용 이름 (tile.fluxnetworks.&lt;name&gt;.name)
     * @param iconName 아이콘 이름 (textures/blocks/&lt;iconName&gt;_on.png, _off.png)
     */
    protected BlockFluxCore(String name, String iconName) {
        // 원작은 FluxUtils.MACHINE. 1단계 결정대로 Material.iron 사용
        super(Material.iron);
        this.iconName = iconName;
        setHardness(0.3F);
        setResistance(1000000.0F);
        setBlockName(FluxNetworks.MODID + "." + name);
        setCreativeTab(CommonProxy.creativeTabs);
    }

    // ---- 타일 엔티티 ----

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public abstract TileEntity createTileEntity(World world, int metadata);

    // ---- 아이콘 ----

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        iconOn = reg.registerIcon(FluxNetworks.MODID + ":" + iconName + "_on");
        iconOff = reg.registerIcon(FluxNetworks.MODID + ":" + iconName + "_off");
        blockIcon = iconOn;
    }

    /** 인벤토리·손에 든 모습, 파괴 파티클 */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return iconOn;
    }

    /** 월드 안: 네트워크에 연결되어 있으면 on, 아니면 off */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileFluxCore && ((TileFluxCore) tile).connected) {
            return iconOn;
        }
        return iconOff;
    }

    // ---- 우클릭 ----

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        // 구성기를 들고 있으면 false → ItemConfigurator.onItemUse가 붙여넣기를 한다 (원작과 같음)
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() instanceof ItemConfigurator) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileFluxCore) {
            TileFluxCore fluxCore = (TileFluxCore) tile;
            if (!fluxCore.playerUsing.isEmpty()) {
                sendDeniedMessage(player, FluxTranslate.ACCESS_OCCUPY_KEY);
                return true;
            } else if (fluxCore.canAccess(player)) {
                player.openGui(FluxNetworks.instance, GuiHandler.GUI_TILE, world, x, y, z);
                return true;
            }
        }
        sendDeniedMessage(player, FluxTranslate.ACCESS_DENIED_KEY);
        return true;
    }

    /** 원작은 액션바(sendStatusMessage)에 띄웠다. 1.7.10에는 없으므로 채팅으로 보낸다 */
    private static void sendDeniedMessage(EntityPlayer player, String key) {
        ChatComponentTranslation message = new ChatComponentTranslation(key);
        message.getChatStyle()
            .setBold(true)
            .setColor(EnumChatFormatting.DARK_RED);
        player.addChatMessage(message);
    }

    // ---- 설치 ----

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileFluxCore) {
            TileFluxCore fluxCore = (TileFluxCore) tile;
            readDataFromStack(stack, fluxCore);
            if (placer instanceof EntityPlayer) {
                fluxCore.playerUUID = ((EntityPlayer) placer).getGameProfile()
                    .getId();
            }
            fluxCore.markDirty();
        }
    }

    // ---- 파괴 ----

    // TODO 확인: Forge 10.13.4.1614의 Block.removedByPlayer(World, EntityPlayer, int, int, int, boolean willHarvest) 시그니처
    // (5인자 버전은 @Deprecated). ItemInWorldManager.removeBlock이 이 6인자 버전을 부르는지 확인
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (world.isRemote) {
            return false;
        }

        TileEntity tile = world.getTileEntity(x, y, z);

        if (tile instanceof TileFluxCore) {
            TileFluxCore fluxCore = (TileFluxCore) tile;
            if (fluxCore.canAccess(player)) {
                ItemStack stack = new ItemStack(this, 1, damageDropped(world.getBlockMetadata(x, y, z)));
                // 타일이 제거되기 전에 TILE_DROP 데이터를 받아 둔다
                writeDataToStack(stack, fluxCore);

                float motion = 0.7F;
                double motionX = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
                double motionY = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;
                double motionZ = (world.rand.nextFloat() * motion) + (1.0F - motion) * 0.5D;

                EntityItem entityItem = new EntityItem(world, x + motionX, y + motionY, z + motionZ, stack);

                world.setBlockToAir(x, y, z);
                world.spawnEntityInWorld(entityItem);
                return true;
            }
        }

        // 원작은 액션바(sendStatusMessage). 1.7.10에는 없으므로 채팅으로 보낸다
        ChatComponentTranslation text = new ChatComponentTranslation(REMOVAL_DENIED_KEY);
        text.getChatStyle()
            .setBold(true)
            .setColor(EnumChatFormatting.DARK_RED);
        player.addChatMessage(text);
        return false;
    }

    /** 드롭은 removedByPlayer가 처리한다 (원작과 같음) */
    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        // 1.7.10 Forge 기본 구현이 hasTileEntity면 타일을 제거한다 (→ TileFluxCore.invalidate → 네트워크에서 제거)
        super.breakBlock(world, x, y, z, block, meta);
    }

    protected void writeDataToStack(ItemStack stack, TileFluxCore tile) {
        NBTTagCompound root = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagCompound sub = root.hasKey(FluxUtils.FLUX_DATA, Constants.NBT.TAG_COMPOUND)
            ? root.getCompoundTag(FluxUtils.FLUX_DATA)
            : new NBTTagCompound();
        tile.writeCustomNBT(sub, NBTType.TILE_DROP);
        root.setTag(FluxUtils.FLUX_DATA, sub);
        stack.setTagCompound(root);
    }

    protected void readDataFromStack(ItemStack stack, TileFluxCore tile) {
        if (stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(FluxUtils.FLUX_DATA, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound sub = stack.getTagCompound()
                .getCompoundTag(FluxUtils.FLUX_DATA);
            tile.readCustomNBT(sub, NBTType.TILE_DROP);
        }
    }

    // ---- 이웃 변경 → 전송 연결 갱신 (원작 BlockSidedConnection.observedNeighborChange) ----

    /** 1.7.10에서는 바뀐 이웃 위치를 알 수 없으므로 6방향 모두 다시 확인한다 */
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileFluxConnector) {
            ((TileFluxConnector) tile).updateTransfers(ForgeDirection.VALID_DIRECTIONS);
        }
    }

    /** Forge: 이웃 타일이 바뀌었을 때 (TileEntity.markDirty 등). 그 방향만 다시 확인한다 */
    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) {
        if (!(world instanceof World) || ((World) world).isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileFluxConnector) {
            ForgeDirection dir = FluxUtils.getBlockDirection(x, y, z, tileX, tileY, tileZ);
            if (dir != null) {
                ((TileFluxConnector) tile).updateTransfers(dir);
            }
        }
    }
}
