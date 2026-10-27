package com.ycj0614.fluxnetworks.common.core;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.ycj0614.fluxnetworks.api.network.ConnectionType;
import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;

public class FluxUtils {

    public static final String FLUX_DATA = "FluxData";
    public static final String GUI_COLOR = "GuiColor";
    public static final String CONFIGS_TAG = "Configs";

    private static final String EU_USAGE_SUFFIX = "EU/t";
    private static final String EU_STORAGE_SUFFIX = "EU";

    public static UUID UUID_DEFAULT = new UUID(-1, -1);

    // TODO 확인: 1.7.10 Material(MapColor) 생성자가 public인지 (protected라면 익명 서브클래스 new Material(...) {}로 변경)
    public static Material MACHINE = new Material(MapColor.blackColor);

    public static <E extends Enum> E incrementEnum(E enumObj, E[] values) {
        int ordinal = enumObj.ordinal() + 1;
        if (ordinal < values.length) {
            return values[ordinal];
        } else {
            return values[0];
        }
    }

    @Nullable
    public static ForgeDirection getBlockDirection(int x, int y, int z, int ox, int oy, int oz) {
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            if (x + d.offsetX == ox && y + d.offsetY == oy && z + d.offsetZ == oz) {
                return d;
            }
        }
        return null;
    }

    public static String getTransferInfo(ConnectionType type, long change) {
        if (type.isPlug()) {
            String b = FluxUtils.format(change, FluxUtils.TypeNumberFormat.COMMAS, true);
            if (change == 0) {
                return FluxTranslate.INPUT.t() + ": " + EnumChatFormatting.GOLD + b;
            } else {
                return FluxTranslate.INPUT.t() + ": " + EnumChatFormatting.GREEN + "+" + b;
            }
        }
        if (type.isPoint() || type.isController()) {
            String b = FluxUtils.format(-change, FluxUtils.TypeNumberFormat.COMMAS, true);
            if (change == 0) {
                return FluxTranslate.OUTPUT.t() + ": " + EnumChatFormatting.GOLD + b;
            } else {
                return FluxTranslate.OUTPUT.t() + ": " + EnumChatFormatting.RED + "-" + b;
            }
        }
        // Storage are inverted
        if (type == ConnectionType.STORAGE) {
            if (change == 0) {
                return FluxTranslate.CHANGE.t() + ": " + EnumChatFormatting.GOLD + change + EU_USAGE_SUFFIX;
            } else if (change > 0) {
                return FluxTranslate.CHANGE.t() + ": "
                    + EnumChatFormatting.RED
                    + "-"
                    + FluxUtils.format(change, FluxUtils.TypeNumberFormat.COMMAS, true);
            } else {
                return FluxTranslate.CHANGE.t() + ": "
                    + EnumChatFormatting.GREEN
                    + "+"
                    + FluxUtils.format(-change, FluxUtils.TypeNumberFormat.COMMAS, true);
            }
        }
        return "";
    }

    public static <T> boolean addWithCheck(Collection<T> list, T toAdd) {
        if (toAdd != null && !list.contains(toAdd)) {
            list.add(toAdd);
            return true;
        }
        return false;
    }

    @Nullable
    public static ItemStack getBlockItem(World world, int x, int y, int z) {
        Item item = Item.getItemFromBlock(world.getBlock(x, y, z));
        if (item == null) {
            return null;
        }
        return new ItemStack(item, 1, world.getBlockMetadata(x, y, z));
    }

    public static boolean addConnection(IFluxConnector fluxConnector) {
        if (fluxConnector.getNetworkID() != -1) {
            IFluxNetwork network = FluxNetworkCache.instance.getNetwork(fluxConnector.getNetworkID());
            if (!network.isInvalid()) {
                if (fluxConnector.getConnectionType()
                    .isController()
                    && network.getConnections(FluxLogicType.CONTROLLER)
                        .size() > 0) {
                    return false;
                }
                network.queueConnectionAddition(fluxConnector);
                return true;
            }
        }
        return false;
    }

    public static void removeConnection(IFluxConnector fluxConnector, boolean isChunkUnload) {
        if (fluxConnector.getNetworkID() != -1) {
            IFluxNetwork network = FluxNetworkCache.instance.getNetwork(fluxConnector.getNetworkID());
            if (!network.isInvalid()) {
                network.queueConnectionRemoval(fluxConnector, isChunkUnload);
            }
        }
    }

    public static int getIntFromColor(int red, int green, int blue) {
        red = red << 16 & 0x00FF0000;
        green = green << 8 & 0x0000FF00;
        blue = blue & 0x000000FF;

        return 0xFF000000 | red | green | blue;
    }

    public static int getBrighterColor(int color, double index) {
        int red = (color >> 16) & 0x000000FF;
        int green = (color >> 8) & 0x000000FF;
        int blue = (color) & 0x000000FF;
        return getIntFromColor(
            (int) Math.min(red * index, 255),
            (int) Math.min(green * index, 255),
            (int) Math.min(blue * index, 255));
    }

    public enum TypeNumberFormat {
        FULL, // Full format
        COMPACT, // Compact format (like 3.5M)
        COMMAS, // Language dependent comma separated format
        NONE // No output (empty string)
    }

    public static String format(long in, TypeNumberFormat style, String suffix) {
        switch (style) {
            case FULL:
                return in + suffix;
            case COMPACT: {
                int unit = 1000;
                if (in < unit) {
                    return in + " " + suffix;
                }
                int exp = (int) (Math.log(in) / Math.log(unit));
                char pre;
                if (suffix.startsWith("m")) {
                    suffix = suffix.substring(1);
                    if (exp - 2 >= 0) {
                        pre = "kMGTPE".charAt(exp - 2);
                        return String.format("%.1f%s", in / Math.pow(unit, exp), pre) + suffix;
                    } else {
                        return String.format("%.1f%s", in / Math.pow(unit, exp), suffix);
                    }
                } else {
                    pre = "kMGTPE".charAt(exp - 1);
                    return String.format("%.1f%s", in / Math.pow(unit, exp), pre) + suffix;
                }
            }
            case COMMAS:
                return NumberFormat.getInstance()
                    .format(in) + suffix;
            case NONE:
                return suffix;
        }
        return Long.toString(in);
    }

    /**
     * 내부 단위 = EU (1:1). usage가 true면 "EU/t", 아니면 "EU" 접미사
     */
    public static String format(long in, TypeNumberFormat style, boolean usage) {
        return format(in, style, usage ? EU_USAGE_SUFFIX : EU_STORAGE_SUFFIX);
    }

    public static boolean checkPassword(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isLetterOrDigit(str.charAt(i))) return false;
        }
        return true;
    }

    // TODO 배치 F: FluxConfigurationType, TileFluxCore 변환 후 추가
    // (원작 copyConfiguration(TileFluxCore, NBTTagCompound): FluxConfigurationType.VALUES를 돌며 타일 설정을 NBT에 복사해 반환)
    // (원작 pasteConfiguration(TileFluxCore, NBTTagCompound): NBT에 키가 있는 설정만 타일에 붙여넣음)

    // TODO 2단계: GUI 일괄 편집용 NBT 생성 (TextboxButton, SlidedSwitchButton 사용)
    // (원작 getBatchEditingTag: 이름·우선순위·한도·surge·disableLimit·chunkLoad를 ItemFluxConnector 키로 NBT에 담아 반환)
}
