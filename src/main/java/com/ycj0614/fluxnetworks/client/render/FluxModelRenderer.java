package com.ycj0614.fluxnetworks.client.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 원작 JSON 블록 모델(elements)을 Tessellator로 그린다. 1.12의 모델 굽기(FaceBakery) 중 쓰는 부분만 옮겼다.
 * <ul>
 * <li>좌표와 UV는 JSON과 같은 0~16 단위</li>
 * <li>요소 회전은 원작 모델에 있는 Y축 45°(중심 8,8,8)만 지원</li>
 * <li>blockstate의 submodel 회전(x, y)은 {@link #transform}의 방향으로 대신한다 (연결 팔 모델)</li>
 * </ul>
 * 월드에서는 면 방향에 따라 밝기를 곱하고(바닐라 면 음영, AO 없음), 아이템에서는 법선을 넣어 GL 조명에 맡긴다.
 */
@SideOnly(Side.CLIENT)
public final class FluxModelRenderer {

    /**
     * 모델 요소 하나 (JSON elements 항목).
     * uv는 ForgeDirection 순서(DOWN, UP, NORTH, SOUTH, WEST, EAST)이고 null이면 그 면을 그리지 않는다
     */
    public static final class Element {

        final float x0, y0, z0, x1, y1, z1;
        /** 텍스처 칸 (BlockFluxCore.TEX_*) */
        final int texture;
        final float[][] uv;
        boolean rotY45;

        Element(float x0, float y0, float z0, float x1, float y1, float z1, int texture, float[][] uv) {
            this.x0 = x0 / 16F;
            this.y0 = y0 / 16F;
            this.z0 = z0 / 16F;
            this.x1 = x1 / 16F;
            this.y1 = y1 / 16F;
            this.z1 = z1 / 16F;
            this.texture = texture;
            this.uv = uv;
        }

        /** "rotation": { "origin": [8, 8, 8], "axis": "y", "angle": 45 } */
        Element rotY45() {
            this.rotY45 = true;
            return this;
        }
    }

    /** 면마다 네 꼭짓점이 상자의 최소(0)·최대(1) 중 어느 쪽인지 (x, y, z). RenderBlocks.renderFace*와 같은 순서 */
    private static final int[][][] FACE_VERTICES = { { { 0, 0, 1 }, { 0, 0, 0 }, { 1, 0, 0 }, { 1, 0, 1 } }, // DOWN
        { { 1, 1, 1 }, { 1, 1, 0 }, { 0, 1, 0 }, { 0, 1, 1 } }, // UP
        { { 0, 1, 0 }, { 1, 1, 0 }, { 1, 0, 0 }, { 0, 0, 0 } }, // NORTH
        { { 0, 1, 1 }, { 0, 0, 1 }, { 1, 0, 1 }, { 1, 1, 1 } }, // SOUTH
        { { 0, 1, 1 }, { 0, 1, 0 }, { 0, 0, 0 }, { 0, 0, 1 } }, // WEST
        { { 1, 0, 1 }, { 1, 0, 0 }, { 1, 1, 0 }, { 1, 1, 1 } }, // EAST
    };

    private static final double SIN45 = Math.sqrt(0.5);

    private FluxModelRenderer() {}

    /**
     * 모델을 그린다. Tessellator는 호출한 쪽에서 startDrawingQuads 해 둔다.
     *
     * @param icons     텍스처 칸별 아이콘 (BlockFluxCore.TEX_*)
     * @param override  null이 아니면 모든 면에 이 아이콘 (블록 부수는 금 표시)
     * @param transform 연결 팔처럼 아래(DOWN)를 향한 모델을 돌릴 방향. null이면 그대로
     * @param tint      TEX_COLOUR 요소에 곱할 색 (RGB)
     * @param tintSlot  색을 입힐 텍스처 칸
     * @param world     true면 면 음영을 색에 곱하고, false면 법선을 넣는다 (아이템)
     */
    public static void render(Tessellator t, Element[] model, IIcon[] icons, IIcon override, ForgeDirection transform,
        int tint, int tintSlot, boolean world, double ox, double oy, double oz) {
        double[] pos = new double[3];
        double[] normal = new double[3];
        for (Element e : model) {
            IIcon icon = override != null ? override : icons[e.texture];
            if (icon == null) {
                continue;
            }
            boolean tinted = override == null && e.texture == tintSlot;
            float r = tinted ? (tint >> 16 & 255) / 255F : 1F;
            float g = tinted ? (tint >> 8 & 255) / 255F : 1F;
            float b = tinted ? (tint & 255) / 255F : 1F;
            for (int f = 0; f < 6; f++) {
                float[] uv = e.uv[f];
                if (uv == null) {
                    continue;
                }
                ForgeDirection face = ForgeDirection.getOrientation(f);
                normal[0] = face.offsetX;
                normal[1] = face.offsetY;
                normal[2] = face.offsetZ;
                transformVector(normal, e.rotY45, transform);
                if (world) {
                    float shade = shade(normal);
                    t.setColorOpaque_F(r * shade, g * shade, b * shade);
                } else {
                    t.setColorOpaque_F(r, g, b);
                    t.setNormal((float) normal[0], (float) normal[1], (float) normal[2]);
                }
                for (int[] corner : FACE_VERTICES[f]) {
                    pos[0] = (corner[0] == 0 ? e.x0 : e.x1) - 0.5;
                    pos[1] = (corner[1] == 0 ? e.y0 : e.y1) - 0.5;
                    pos[2] = (corner[2] == 0 ? e.z0 : e.z1) - 0.5;
                    double tu = faceU(f, corner);
                    double tv = faceV(f, corner);
                    double u = uv[0] + tu * (uv[2] - uv[0]);
                    double v = uv[1] + tv * (uv[3] - uv[1]);
                    transformVector(pos, e.rotY45, transform);
                    t.addVertexWithUV(
                        ox + pos[0] + 0.5,
                        oy + pos[1] + 0.5,
                        oz + pos[2] + 0.5,
                        icon.getInterpolatedU(u),
                        icon.getInterpolatedV(v));
                }
            }
        }
    }

    /** 1.12 기본 UV와 같은 방향: 면을 바깥에서 볼 때 u는 왼쪽→오른쪽, v는 위→아래 (위·아래 면은 북쪽이 v=0) */
    private static double faceU(int face, int[] c) {
        switch (face) {
            case 2: // NORTH
                return 1 - c[0];
            case 4: // WEST
                return c[2];
            case 5: // EAST
                return 1 - c[2];
            default: // DOWN, UP, SOUTH
                return c[0];
        }
    }

    private static double faceV(int face, int[] c) {
        switch (face) {
            case 0: // DOWN
                return 1 - c[2];
            case 1: // UP
                return c[2];
            default:
                return 1 - c[1];
        }
    }

    /**
     * 블록 중심 기준 좌표(또는 법선)를 돌린다. 먼저 요소의 Y축 45°, 다음에 방향 변환.
     * 방향 변환은 원작 blockstate의 submodel 회전과 같다:
     * north x90 y180, east x90 y270, south x90, west x90 y90, up x180, down 그대로
     */
    private static void transformVector(double[] v, boolean rotY45, ForgeDirection dir) {
        double x = v[0], y = v[1], z = v[2];
        if (rotY45) {
            double nx = (x - z) * SIN45;
            double nz = (x + z) * SIN45;
            x = nx;
            z = nz;
        }
        if (dir != null) {
            double nx = x, ny = y, nz = z;
            switch (dir) {
                case UP:
                    ny = -y;
                    nz = -z;
                    break;
                case SOUTH:
                    ny = z;
                    nz = -y;
                    break;
                case NORTH:
                    nx = -x;
                    ny = z;
                    nz = y;
                    break;
                case WEST:
                    nx = y;
                    ny = z;
                    nz = x;
                    break;
                case EAST:
                    nx = -y;
                    ny = z;
                    nz = -x;
                    break;
                default:
                    break;
            }
            x = nx;
            y = ny;
            z = nz;
        }
        v[0] = x;
        v[1] = y;
        v[2] = z;
    }

    /** 바닐라 면 음영 (아래 0.5, 위 1.0, 남북 0.8, 동서 0.6). 45° 면은 성분 제곱으로 섞는다 */
    private static float shade(double[] n) {
        double s = n[0] * n[0] * 0.6 + n[2] * n[2] * 0.8 + n[1] * n[1] * (n[1] > 0 ? 1.0 : 0.5);
        return (float) s;
    }

    // ---- 모델 정의용 ----

    static float[] uv(float u0, float v0, float u1, float v1) {
        return new float[] { u0, v0, u1, v1 };
    }

    /** 면 UV 순서: down, up, north, south, west, east */
    static Element box(float x0, float y0, float z0, float x1, float y1, float z1, int texture, float[] down,
        float[] up, float[] north, float[] south, float[] west, float[] east) {
        return new Element(x0, y0, z0, x1, y1, z1, texture, new float[][] { down, up, north, south, west, east });
    }

    /** 여섯 면이 모두 같은 UV인 상자 */
    static Element box(float x0, float y0, float z0, float x1, float y1, float z1, int texture, float[] all) {
        return box(x0, y0, z0, x1, y1, z1, texture, all, all, all, all, all, all);
    }

    /** 옆 네 면과 위아래 면의 UV가 다른 상자 */
    static Element box(float x0, float y0, float z0, float x1, float y1, float z1, int texture, float[] sides,
        float[] topBottom) {
        return box(x0, y0, z0, x1, y1, z1, texture, topBottom, topBottom, sides, sides, sides, sides);
    }
}
