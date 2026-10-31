package com.ycj0614.fluxnetworks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class FluxConfig {

    public static Configuration config;

    public static final String GENERAL = "general";
    public static final String CLIENT = "client";
    public static final String ENERGY = "energy";
    public static final String NETWORKS = "networks";
    public static final String BLACKLIST = "blacklists";

    public static boolean enableButtonSound;
    public static boolean enableFluxRecipe, enableOldRecipe, enableGTRecipes, enableChunkLoading, enableSuperAdmin;
    public static int defaultLimit, defaultPlugMaxInputTier, defaultPointTier, defaultPointAmperage, pointBufferTicks;
    public static int basicCapacity, basicTransfer, herculeanCapacity, herculeanTransfer, gargantuanCapacity,
        gargantuanTransfer;
    public static int maximumPerPlayer, superAdminRequiredPermission;
    public static String[] blockBlacklistStrings, itemBlackListStrings;

    /** "modid:name" -> meta (-1 = 모든 meta). 원작의 TileEntityHandler.blockBlacklist를 대체 */
    public static final Map<String, Integer> blockBlacklist = new HashMap<>();
    /** "modid:name" -> meta (-1 = 모든 meta). 원작의 ItemEnergyHandler.itemBlackList를 대체 */
    public static final Map<String, Integer> itemBlacklist = new HashMap<>();

    public static void init(File file) {
        config = new Configuration(new File(file.getPath(), "flux_networks.cfg"));
        config.load();
        read();
        verifyAndReadBlacklist();
        config.save();
        generateFluxChunkConfig();
    }

    public static void verifyAndReadBlacklist() {
        parseBlacklist(blockBlacklistStrings, blockBlacklist);
        parseBlacklist(itemBlackListStrings, itemBlacklist);
    }

    private static void parseBlacklist(String[] entries, Map<String, Integer> target) {
        target.clear();
        for (String str : entries) {
            if (!str.contains(":")) {
                FluxNetworks.logger
                    .error("BLACKLIST ERROR: " + str + " has incorrect formatting, please use 'modid:name@meta'");
            }
            String root = str;
            int meta = -1;
            if (str.contains("@")) {
                String[] split = str.split("@");
                root = split[0];
                try {
                    meta = Integer.parseInt(split[1]);
                    target.put(root, meta);
                } catch (Exception e) {
                    FluxNetworks.logger
                        .error("BLACKLIST ERROR: " + str + " has incorrect formatting, meta must be positive integer'");
                }
            } else {
                target.put(root, meta);
            }
        }
    }

    public static void generateFluxChunkConfig() {
        if (!ForgeChunkManager.getConfig()
            .hasCategory(FluxNetworks.MODID)) {
            ForgeChunkManager.getConfig()
                .get(FluxNetworks.MODID, "maximumChunksPerTicket", 1000000)
                .setMinValue(0);
            ForgeChunkManager.getConfig()
                .get(FluxNetworks.MODID, "maximumTicketCount", 1000000)
                .setMinValue(0);
            ForgeChunkManager.getConfig()
                .save();
        }
    }

    public static void read() {
        defaultLimit = config.getInt(
            "Default Plug Transfer Limit",
            ENERGY,
            2097152,
            0,
            Integer.MAX_VALUE,
            "Maximum EU per tick a single Flux Plug can send into the network");
        defaultPlugMaxInputTier = config.getInt(
            "Default Plug Max Input Tier",
            ENERGY,
            14,
            0,
            14,
            "Maximum voltage tier a Flux Plug accepts (0 = ULV, 14 = MAX). Higher voltages are rejected without exploding");
        defaultPointTier = config.getInt(
            "Default Point Output Tier",
            ENERGY,
            1,
            0,
            14,
            "Default output voltage tier of a Flux Point (1 = LV)");
        defaultPointAmperage = config.getInt(
            "Default Point Output Amperage",
            ENERGY,
            1,
            1,
            Integer.MAX_VALUE,
            "Default output amperage of a Flux Point");
        pointBufferTicks = config
            .getInt("Point Buffer Ticks", ENERGY, 2, 1, 20, "Flux Point buffer size = voltage x amperage x this value");

        basicCapacity = config.getInt("Basic Storage Capacity", ENERGY, 250000, 0, Integer.MAX_VALUE, "In EU");
        basicTransfer = config.getInt("Basic Storage Transfer", ENERGY, 5000, 0, Integer.MAX_VALUE, "In EU per tick");
        herculeanCapacity = config.getInt("Herculean Storage Capacity", ENERGY, 2000000, 0, Integer.MAX_VALUE, "In EU");
        herculeanTransfer = config
            .getInt("Herculean Storage Transfer", ENERGY, 30000, 0, Integer.MAX_VALUE, "In EU per tick");
        gargantuanCapacity = config
            .getInt("Gargantuan Storage Capacity", ENERGY, 32000000, 0, Integer.MAX_VALUE, "In EU");
        gargantuanTransfer = config
            .getInt("Gargantuan Storage Transfer", ENERGY, 360000, 0, Integer.MAX_VALUE, "In EU per tick");

        maximumPerPlayer = config.getInt(
            "Maximum Networks Per Player",
            NETWORKS,
            3,
            -1,
            Integer.MAX_VALUE,
            "Maximum networks each player can have. -1 = no limit");
        enableSuperAdmin = config.getBoolean(
            "Allow Network Super Admin",
            NETWORKS,
            true,
            "Allows someone to be a network super admin, otherwise, no one can access or dismantle your flux devices or delete your networks without permission");
        superAdminRequiredPermission = config.getInt(
            "Permission level required to activate Super Admin",
            NETWORKS,
            1,
            0,
            Integer.MAX_VALUE,
            "See ops.json. If the player has permission level equal or greater to the value set here they will be able to Activate Super Admin. Setting this to 0 will allow anyone to active Super Admin.");

        enableFluxRecipe = config.getBoolean(
            "Enable Flux Recipe",
            GENERAL,
            true,
            "Enables redstones being compressed with the bedrock and obsidian to get flux");
        enableOldRecipe = config.getBoolean(
            "Enable Old Recipe",
            GENERAL,
            false,
            "Enables redstone being turned into Flux when dropped in fire. (Need \"Enable Flux Recipe\" = true, so the default recipe can't be disabled if turns this on)");
        // 원작과 다름: GT 레시피 선택
        enableGTRecipes = config.getBoolean(
            "Enable GT Recipes",
            GENERAL,
            true,
            "Uses GregTech assembler recipes for flux devices and GT parts in storage recipes. If false, the original crafting table recipes are used");
        enableChunkLoading = config
            .getBoolean("Allow Flux Chunk Loading", GENERAL, true, "Allows flux tiles to work as chunk loaders");

        enableButtonSound = config
            .getBoolean("Enable GUI Button Sound", CLIENT, true, "Enable navigation buttons sound when pressing it");

        blockBlacklistStrings = getBlackList(
            "Block Connection Blacklist",
            BLACKLIST,
            new String[] {},
            "a blacklist for blocks which flux connections shouldn't connect to, use format 'modid:name@meta'");
        itemBlackListStrings = getBlackList(
            "Item Transfer Blacklist",
            BLACKLIST,
            new String[] {},
            "a blacklist for items which the Flux Controller shouldn't transfer to, use format 'modid:name@meta'");
    }

    public static String[] getBlackList(String name, String category, String[] defaultValue, String comment) {
        Property prop = config.get(category, name, defaultValue);
        prop.setLanguageKey(name);
        prop.setValidValues(null);
        prop.comment = comment;
        return prop.getStringList();
    }
}
