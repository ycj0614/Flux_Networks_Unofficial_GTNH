package com.ycj0614.fluxnetworks.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.lwjgl.opengl.GL11;

import com.ycj0614.fluxnetworks.common.core.FluxUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * OpenGL로 그리는 꺾은선 그래프 (통계 탭의 에너지 변화량).
 * 원작과 다름: 데이터는 NetworkStatistics.energyChange(long[])를 그대로 받는다
 *
 * @author BloCamLimb
 */
@SideOnly(Side.CLIENT)
public class LineChart {

    public int x, y;
    public int height;

    public int linePoints;

    public String displayUnitX;
    public String displayUnitY;

    public long maxUnitY;
    public String suffixUnitY;

    public long[] data = new long[0];

    public double[] currentHeight;
    public double[] targetHeight;

    public LineChart(int x, int y, int height, int linePoints, String displayUnitX, String suffixUnitY) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.linePoints = linePoints;
        this.displayUnitX = displayUnitX;
        this.suffixUnitY = suffixUnitY;

        this.currentHeight = new double[linePoints];
        this.targetHeight = new double[linePoints];
        for (int i = 0; i < linePoints; i++) {
            currentHeight[i] = y + height;
            targetHeight[i] = y + height;
        }
    }

    public void drawChart(Minecraft mc) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_POINT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < currentHeight.length; i++) {
            GL11.glVertex3d(x + 20 * i, currentHeight[i], 1);
        }
        GL11.glEnd();

        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glPointSize(8.0f);
        GL11.glBegin(GL11.GL_POINTS);
        for (int i = 0; i < currentHeight.length; i++) {
            GL11.glVertex3d(x + 20 * i, currentHeight[i], 1);
        }
        GL11.glEnd();
        GL11.glPointSize(1.0f);
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        Gui.drawRect(x - 16, y + height, x + 116, y + height + 1, 0xffffffff);
        Gui.drawRect(x - 14, y - 6, x - 13, y + height + 3, 0xffffffff);

        FontRenderer font = mc.fontRenderer;
        GL11.glPushMatrix();
        GL11.glScalef(0.625f, 0.625f, 0.625f);
        font.drawString(
            suffixUnitY,
            (int) ((x - 15) * 1.6) - font.getStringWidth(suffixUnitY),
            (int) ((y - 7.5) * 1.6),
            0xffffff);
        if (displayUnitY != null) {
            font.drawString(
                displayUnitY,
                (int) ((x - 15) * 1.6) - font.getStringWidth(displayUnitY),
                (int) ((y - 2) * 1.6),
                0xffffff);
        }
        font.drawString(
            displayUnitX,
            (int) ((x + 118) * 1.6) - font.getStringWidth(displayUnitX),
            (int) ((y + height + 1.5) * 1.6),
            0xffffff);
        for (int i = 0; i < data.length && i < currentHeight.length; i++) {
            String d = FluxUtils.format(data[i], FluxUtils.TypeNumberFormat.COMPACT, "");
            font.drawString(
                d,
                (int) ((x + 20 * i) * 1.6F - font.getStringWidth(d) / 2F + 1.0f),
                (int) ((currentHeight[i] - 7) * 1.6),
                0xffffff);
            String c = String.valueOf((linePoints - 1 - i) * 5);
            font.drawString(
                c,
                (int) ((x + 20 * i) * 1.6F - font.getStringWidth(c) / 2F),
                (int) ((y + height + 2) * 1.6),
                0xffffff);
        }
        GL11.glPopMatrix();

        GL11.glPopAttrib();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void updateData(long[] newData) {
        this.data = newData.clone();
        calculateUnitY(this.data);
        calculateTargetHeight(this.data);
    }

    public void updateHeight(float partialTick) {
        for (int i = 0; i < currentHeight.length; i++) {
            double a = targetHeight[i] - currentHeight[i];
            if (a == 0) {
                continue;
            }
            double c;
            double p = partialTick / 16;
            if (Math.abs(a) <= p) {
                c = targetHeight[i];
            } else {
                if (a > 0) {
                    c = currentHeight[i] + Math.max(Math.min(a, a / 4 * partialTick), p);
                } else {
                    c = currentHeight[i] + Math.min(Math.max(a, a / 4 * partialTick), -p);
                }
            }
            currentHeight[i] = c;
        }
    }

    private void calculateUnitY(long[] data) {
        long maxValue = 0;
        for (long v : data) {
            maxValue = Math.max(maxValue, v);
        }
        if (maxValue == 0) {
            maxUnitY = 1;
            displayUnitY = FluxUtils.format(maxUnitY, FluxUtils.TypeNumberFormat.COMPACT, "");
            return;
        }
        int measureLevel = (int) Math.log10(maxValue); // 0 = 10, 3 = 10000
        switch (measureLevel) {
            case 0:
                maxUnitY = maxValue + 1;
                break;
            case 1:
                maxUnitY = ((maxValue / 5) + 1) * 5;
                break;
            case 2:
                maxUnitY = ((maxValue / 50) + 1) * 50;
                break;
            default:
                long p = (long) Math.pow(10, measureLevel - 1);
                maxUnitY = ((maxValue / p) + 1) * p;
                break;
        }
        displayUnitY = FluxUtils.format(maxUnitY, FluxUtils.TypeNumberFormat.COMPACT, "");
    }

    private void calculateTargetHeight(long[] data) {
        if (data.length != linePoints) {
            return;
        }
        for (int i = 0; i < data.length; i++) {
            // 원작과 다름: 음수(에너지 감소)는 0으로 그린다 (그래프 아래로 벗어나지 않게)
            targetHeight[i] = y + height * (1 - ((double) Math.max(0L, data[i]) / maxUnitY));
        }
    }
}
