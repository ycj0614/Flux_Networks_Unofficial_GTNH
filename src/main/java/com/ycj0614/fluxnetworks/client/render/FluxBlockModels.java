package com.ycj0614.fluxnetworks.client.render;

import static com.ycj0614.fluxnetworks.client.render.FluxModelRenderer.box;
import static com.ycj0614.fluxnetworks.client.render.FluxModelRenderer.uv;
import static com.ycj0614.fluxnetworks.common.block.BlockFluxCore.TEX_0;
import static com.ycj0614.fluxnetworks.common.block.BlockFluxCore.TEX_1;
import static com.ycj0614.fluxnetworks.common.block.BlockFluxCore.TEX_COLOUR;
import static com.ycj0614.fluxnetworks.common.block.BlockFluxCore.TEX_TOP;

import com.ycj0614.fluxnetworks.client.render.FluxModelRenderer.Element;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 models/block/*.json을 옮긴 것. 좌표·UV·텍스처 칸(#0, #1, #top, #colour)은 JSON 그대로다.
 * 텍스처 칸 이름은 BlockFluxCore.TEX_* (#colour 요소가 원작 tintindex 1)
 */
@SideOnly(Side.CLIENT)
public final class FluxBlockModels {

    private FluxBlockModels() {}

    /** fluxplug.json */
    public static final Element[] PLUG = { box(4, 4, 4, 12, 12, 12, TEX_0, uv(8, 2, 16, 10)),
        box(4, 4, 4, 12, 12, 12, TEX_COLOUR, uv(8, 2, 16, 10)), };

    /** fluxpoint.json */
    public static final Element[] POINT = { box(5.5F, 5.5F, 5.5F, 10.5F, 10.5F, 10.5F, TEX_0, uv(12, 6, 16, 10)),
        box(5.5F, 5.5F, 5.5F, 10.5F, 10.5F, 10.5F, TEX_COLOUR, uv(12, 6, 16, 10)), };

    /** fluxconnection.json (아래를 향한 연결 팔. 다른 방향은 FluxModelRenderer가 돌린다) */
    public static final Element[] CONNECTION = { box(7, 1, 7, 9, 6, 9, TEX_0, uv(0, 0, 2, 5), uv(0, 0, 2, 2)),
        box(5, 0, 5, 11, 1, 11, TEX_0, uv(0, 0, 6, 1), uv(10, 10, 16, 16)),
        box(7, 1, 7, 9, 6, 9, TEX_COLOUR, uv(0, 0, 2, 5), uv(0, 0, 2, 2)),
        box(5, 0, 5, 11, 1, 11, TEX_COLOUR, uv(0, 0, 6, 1), uv(10, 10, 16, 16)), };

    private static final float[] LEG_END = uv(2, 2, 4, 4);
    private static final float[] DISC = uv(0, 0, 6, 6);
    private static final float[] DISC_SIDE = uv(0, 0, 6, 1);
    private static final float[] PLATE = uv(0, 0, 16, 16);
    private static final float[] PLATE_SIDE = uv(0, 0, 16, 1);
    private static final float[] INNER_END = uv(1, 1, 2, 2);
    private static final float[] INNER_NS = uv(1, 2, 2, 14);
    private static final float[] INNER_WE = uv(14, 2, 15, 14);

    /** fluxcontroller.json (원작 경계 상자는 0.0625~0.9375지만 모델은 블록 전체를 쓴다) */
    public static final Element[] CONTROLLER = {
        // Base1
        box(0, 1, 0, 16, 2, 16, TEX_0, PLATE_SIDE, PLATE),
        // Top1, Top1Colour
        box(0, 14, 0, 16, 15, 16, TEX_0, PLATE, null, PLATE_SIDE, PLATE_SIDE, PLATE_SIDE, PLATE_SIDE),
        box(0, 14, 0, 16, 15, 16, TEX_TOP, null, PLATE, null, null, null, null),
        box(0, 14, 0, 16, 15, 16, TEX_COLOUR, PLATE_SIDE, PLATE),
        // Leg1~4
        leg(1, 1), leg(13, 1), leg(13, 13), leg(1, 13),
        // Base2, Base3, Top2, Top3
        box(5, 2, 5, 11, 3, 11, TEX_1, DISC_SIDE, DISC).rotY45(),
        box(5, 0, 5, 11, 1, 11, TEX_1, DISC_SIDE, DISC).rotY45(),
        box(5, 13, 5, 11, 14, 11, TEX_1, DISC_SIDE, DISC).rotY45(),
        box(5, 15, 5, 11, 16, 11, TEX_1, DISC_SIDE, DISC).rotY45(),
        // LegInside1~4
        innerLeg(7.5F, 0.5F, 8.5F, 1.5F), innerLeg(7.5F, 14.5F, 8.5F, 15.5F), innerLeg(14.5F, 7.5F, 15.5F, 8.5F),
        innerLeg(0.5F, 7.5F, 1.5F, 8.5F), };

    private static Element leg(float x, float z) {
        return box(
            x,
            0,
            z,
            x + 2,
            16,
            z + 2,
            TEX_1,
            LEG_END,
            LEG_END,
            uv(8, 0, 10, 16),
            uv(10, 0, 12, 16),
            uv(12, 0, 14, 16),
            uv(8, 0, 10, 16));
    }

    private static Element innerLeg(float x0, float z0, float x1, float z1) {
        return box(x0, 2, z0, x1, 14, z1, TEX_0, INNER_END, INNER_END, INNER_NS, INNER_NS, INNER_WE, INNER_WE).rotY45();
    }

    private static final float[] PILLAR = uv(0, 0, 1, 14);
    private static final float[] PILLAR_END = uv(0, 0, 1, 1);
    private static final float[] RIM_LONG = uv(0, 0, 16, 1);
    private static final float[] RIM_END = uv(0, 0, 1, 1);
    private static final float[] RIM_SIDE = uv(0, 0, 14, 1);
    private static final float[] RIM_TOP = uv(0, 0, 1, 14);
    private static final float[] BASE_NS = uv(1, 0, 15, 1);
    private static final float[] BASE_WE = uv(1, 15, 15, 16);
    private static final float[] BASE_TOP = uv(1, 1, 15, 15);

    /** fluxstorage.json (#0 회색 기둥, #1 티어 텍스처, #colour). 에너지 게이지는 TileFluxStorageRenderer */
    public static final Element[] STORAGE = {
        // Base5, Base_Colour
        box(1, 1, 1, 15, 2, 15, TEX_1, BASE_TOP, BASE_TOP, BASE_NS, BASE_NS, BASE_WE, BASE_WE),
        box(1, 1, 1, 15, 2, 15, TEX_COLOUR, BASE_TOP, BASE_TOP, BASE_NS, BASE_NS, BASE_WE, BASE_WE),
        // Base1~4 (아래 테두리)
        box(0, 0, 0, 16, 1, 1, TEX_1, RIM_LONG, RIM_LONG, RIM_LONG, RIM_LONG, RIM_END, RIM_END),
        box(0, 0, 15, 16, 1, 16, TEX_1, RIM_LONG, RIM_LONG, RIM_LONG, RIM_LONG, RIM_END, RIM_END),
        box(0, 0, 1, 1, 1, 15, TEX_1, RIM_TOP, RIM_TOP, RIM_END, RIM_END, RIM_SIDE, RIM_SIDE),
        box(15, 0, 1, 16, 1, 15, TEX_1, RIM_TOP, RIM_TOP, RIM_END, RIM_END, RIM_SIDE, RIM_SIDE),
        // Top, Top_Colour
        box(0, 15, 0, 16, 16, 16, TEX_1, RIM_LONG, PLATE), box(0, 15, 0, 16, 16, 16, TEX_COLOUR, RIM_LONG, PLATE),
        // Section2~8, Cube (모서리 기둥)
        pillar(0, 1), pillar(0, 14), pillar(1, 15), pillar(14, 15), pillar(15, 14), pillar(15, 1), pillar(14, 0),
        pillar(1, 0), };

    private static Element pillar(float x, float z) {
        return box(x, 1, z, x + 1, 15, z + 1, TEX_0, PILLAR, PILLAR_END);
    }
}
