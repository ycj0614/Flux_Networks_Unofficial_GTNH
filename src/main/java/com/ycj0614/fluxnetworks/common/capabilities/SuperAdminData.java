package com.ycj0614.fluxnetworks.common.capabilities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListEntry;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;

import com.ycj0614.fluxnetworks.FluxConfig;
import com.ycj0614.fluxnetworks.api.network.ISuperAdmin;
import com.ycj0614.fluxnetworks.common.connection.FluxNetworkCache;

/**
 * 원작 DefaultSuperAdmin + CapabilitySAProvider + DefaultSAStorage를 1.7.10 IExtendedEntityProperties 하나로 합친 것.
 * 플레이어마다 하나씩 붙으며(SuperAdminHandler), 플레이어 NBT 안의 하위 태그 "fluxnetworks_SuperAdmin"에 저장된다.
 * 원작처럼 사망 후 복사 처리는 없다 (리스폰하면 SuperAdmin이 해제된다).
 */
public class SuperAdminData implements ISuperAdmin, IExtendedEntityProperties {

    public static final String PROP_NAME = "fluxnetworks:SuperAdmin";

    /** 플레이어 NBT 안의 하위 태그 이름 */
    private static final String NBT_KEY = "fluxnetworks_SuperAdmin";

    private boolean SA = false;

    @Override
    public void changePermission() {
        SA = !SA;
    }

    @Override
    public boolean getPermission() {
        return SA;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("SA", SA);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        SA = nbt.getBoolean("SA");
    }

    //// IExtendedEntityProperties \\\\

    /** compound는 플레이어 엔티티의 최상위 NBT. 다른 모드와 키가 겹치지 않게 하위 태그에 쓴다 */
    @Override
    public void saveNBTData(NBTTagCompound compound) {
        compound.setTag(NBT_KEY, writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (compound.hasKey(NBT_KEY, Constants.NBT.TAG_COMPOUND)) {
            readFromNBT(compound.getCompoundTag(NBT_KEY));
        }
    }

    @Override
    public void init(Entity entity, World world) {}

    /** 플레이어에 붙은 SuperAdminData. 없으면 null */
    public static SuperAdminData get(EntityPlayer player) {
        IExtendedEntityProperties props = player.getExtendedProperties(PROP_NAME);
        return props instanceof SuperAdminData ? (SuperAdminData) props : null;
    }

    //// UTIL METHODS \\\\

    /** Server Only */
    public static boolean canActivateSuperAdmin(EntityPlayer player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return false;
        }
        if (server.isSinglePlayer()) {
            return true;
        }
        int required = FluxConfig.superAdminRequiredPermission;
        // 원작: OP가 아닌 플레이어의 권한 레벨은 0이므로, 요구 레벨이 0 이하면 누구나 켤 수 있다
        if (required <= 0) {
            return true;
        }
        // 원작처럼 ops.json의 플레이어별 level을 요구 레벨과 비교한다 (OP가 아니면 0).
        // func_152603_m = getOppedPlayers (UserListOps), func_152683_b = getEntry, func_152644_a = getPermissionLevel.
        // 1.7.10의 /op는 server.properties의 op-permission-level을 level로 기록하므로, ops.json을 직접 고친 경우에만 달라진다
        UserListEntry entry = server.getConfigurationManager()
            .func_152603_m()
            .func_152683_b(player.getGameProfile());
        int level = entry instanceof UserListOpsEntry ? ((UserListOpsEntry) entry).func_152644_a() : 0;
        return level >= required;
    }

    public static boolean isPlayerSuperAdmin(EntityPlayer player) {
        if (!player.worldObj.isRemote) {
            SuperAdminData data = get(player);
            return data != null && data.getPermission();
        }
        return FluxNetworkCache.instance.superAdminClient;
    }
}
