package com.ycj0614.fluxnetworks.client.integration.nei;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.ycj0614.fluxnetworks.FluxNetworks;
import com.ycj0614.fluxnetworks.common.registry.RegistryItems;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 JEI FluxCraftingCategory + FluxRecipeWrapper. 흑요석 위 레드스톤을 좌클릭하면 플럭스가 되는 월드 안 제작(CommonProxy.onPlayerInteract).
 * 좌표는 원작 JEI 배경(128×80, 텍스처를 20px 내려 그림) 기준이고, NEI 기본 너비 166 안에서 가운데로 X만큼 민다.
 * 원작과 다름: 1.7.10에는 ItemCameraTransforms·쿼터니언이 없으므로 블록은 RenderBlocks.renderBlockAsItem, 아이템은
 * ItemRenderer.renderItemIn2D와 GL11 회전으로 같은 모양을 낸다. 애니메이션 타이머는 JEI 틱 타이머(60틱에 0~320) 대신 시스템 시간을 쓴다.
 */
@SideOnly(Side.CLIENT)
public class FluxCraftingHandler extends TemplateRecipeHandler {

    /** 원작 JEI 카테고리 UID */
    public static final String HANDLER_ID = "flux";
    public static final String TEXTURE = FluxNetworks.MODID + ":textures/gui/gui_creating_flux_recipe.png";

    public static final int WIDTH = 166;
    public static final int HEIGHT = 80;
    /** 원작 배경(너비 128)을 가운데로 미는 양 */
    private static final int X = (WIDTH - 128) / 2;

    /** 원작 ITickTimer(60, 320): 60틱(3초) 동안 0에서 320까지 */
    private static final long CYCLE_MILLIS = 3000L;
    private static final int MAX_VALUE = 320;

    private static final RenderBlocks renderBlocks = new RenderBlocks();

    public class CachedFluxRecipe extends CachedRecipe {

        private final PositionedStack input = new PositionedStack(new ItemStack(Items.redstone), X + 8, 24, false);
        private final PositionedStack output = new PositionedStack(
            new ItemStack(RegistryItems.FLUX),
            X + 102,
            24,
            false);

        @Override
        public PositionedStack getIngredient() {
            return input;
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }
    }

    @Override
    public String getHandlerId() {
        return HANDLER_ID;
    }

    @Override
    public String getRecipeName() {
        return I18n.format("info.fluxnetworks.jei.creatingfluxrecipe");
    }

    @Override
    public String getGuiTexture() {
        return TEXTURE;
    }

    @Override
    public void loadTransferRects() {
        // 애니메이션을 누르면 이 핸들러의 모든 레시피. 카탈리스트(플럭스)의 사용처 조회도 이 영역의 outputId로 불러온다
        transferRects.add(new RecipeTransferRect(new Rectangle(X + 40, 8, 40, 52), HANDLER_ID));
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (HANDLER_ID.equals(outputId)) {
            arecipes.add(new CachedFluxRecipe());
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (result != null && result.getItem() == RegistryItems.FLUX) {
            arecipes.add(new CachedFluxRecipe());
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (ingredient != null && ingredient.getItem() == Items.redstone) {
            arecipes.add(new CachedFluxRecipe());
        }
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(TEXTURE);
        // 원작 createDrawable(TEXTURES, 0, -20, 128, 80): 텍스처 위쪽 20px는 비어 있다
        GuiDraw.drawTexturedModalRect(X, 20, 0, 0, 128, 60);
    }

    @Override
    public void drawExtras(int recipe) {
        int value = (int) (Minecraft.getSystemTime() % CYCLE_MILLIS * MAX_VALUE / CYCLE_MILLIS);
        float offset = (value > 160 ? 160 - (value - 160) : value) / 10F;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1, 1, 1, 1);

        // 위에서 내려오는 흑요석과 바닥의 기반암
        RenderHelper.enableGUIStandardItemLighting();
        drawBlock(Blocks.obsidian, X + 63, 19 + offset, 128);
        drawBlock(Blocks.bedrock, X + 63, 49, 96);
        RenderHelper.disableStandardItemLighting();

        // 가운데에서 돌다가 절반이 지나면 결과물로 바뀌는 아이템
        ItemStack toDisplay = new ItemStack(value > 160 ? RegistryItems.FLUX : Items.redstone);
        drawItem(toDisplay, X + 63, 36, 112, -90 + 180 * ((float) value / MAX_VALUE));

        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String help = I18n.format("info.fluxnetworks.jei.leftclickhelp", Blocks.obsidian.getLocalizedName());
        fontRenderer.drawString(help, (int) (X + 64 - fontRenderer.getStringWidth(help) / 2f), 68, 0x404040);
    }

    /** 원작 FIXED 변환(크기 0.5) × 32 = 한 변 16px, X 30° · Y 45° 기울임 */
    private static void drawBlock(Block block, float x, float y, float z) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        GL11.glScalef(16, 16, 16);
        // RenderItem.renderItemIntoGUI와 같은 방식: 화면 y가 아래로 가므로 뒤집어서 30° 기울인다
        GL11.glScalef(1, 1, -1);
        GL11.glRotatef(180 + 30, 1, 0, 0);
        GL11.glRotatef(45, 0, 1, 0);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        renderBlocks.renderBlockAsItem(block, 0, 1.0F);
        GL11.glPopMatrix();
    }

    /** 원작 scale(16, -16, 16) × FIXED 0.5 = 한 변 8px, Y축 회전 */
    private static void drawItem(ItemStack stack, float x, float y, float z, float yaw) {
        IIcon icon = stack.getIconIndex();
        if (icon == null) {
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, z);
        GL11.glScalef(8, -8, 8);
        GL11.glRotatef(yaw, 0, 1, 0);
        GL11.glTranslatef(-0.5F, -0.5F, 0.03125F);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(
                stack.getItemSpriteNumber() == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);
        ItemRenderer.renderItemIn2D(
            Tessellator.instance,
            icon.getMaxU(),
            icon.getMinV(),
            icon.getMinU(),
            icon.getMaxV(),
            icon.getIconWidth(),
            icon.getIconHeight(),
            0.0625F);
        GL11.glPopMatrix();
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
        Point mouse = gui.getRecipeMousePosition(recipe);
        if (mouse.x > X + 40 && mouse.x < X + 80 && mouse.y > 8 && mouse.y < 60) {
            currenttip.add("Y+2 = " + Blocks.obsidian.getLocalizedName());
            currenttip.add("Y+1 = " + new ItemStack(Items.redstone).getDisplayName());
            currenttip.add("Y+0 = " + Blocks.bedrock.getLocalizedName());
        }
        // 원작 툴팁이 있으면 전송 영역 툴팁은 붙지 않는다 (비어 있을 때만 붙음)
        return super.handleTooltip(gui, currenttip, recipe);
    }
}
