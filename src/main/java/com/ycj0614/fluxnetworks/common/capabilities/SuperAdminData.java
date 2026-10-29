package com.ycj0614.fluxnetworks.common.capabilities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
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
        // 1.7.10 바닐라 명령 권한 판정과 같은 방식: OP이면 서버의 OP 권한 레벨(server.properties의 op-permission-level)을
        // 요구 레벨과 비교하고, OP가 아니면 false.
        // (EntityPlayerMP.canCommandSenderUseCommand 내부: getConfigurationManager().func_152596_g(profile) &&
        // mcServer.getOpPermissionLevel() >= level. 명령 이름이 "seed"/"tell"/"help"/"me"일 때만 예외가 있으므로
        // 그 외 이름을 넘긴다)
        // TODO 확인: 원작처럼 ops.json의 플레이어별 level을 쓰려면 아래 경로가 필요하다. 매핑에 따라 이름이 다르므로 IDE에서 확인할 것
        // ServerConfigurationManager.func_152603_m() (= getOppedPlayers, UserListOps 반환)
        // → UserList.func_152683_b(GameProfile) (= getEntry, UserListOpsEntry 반환, OP가 아니면 null)
        // → UserListOpsEntry.func_152644_a() (= getPermissionLevel). OP 여부만은 func_152596_g(GameProfile) (=
        // canSendCommands)
        return player.canCommandSenderUseCommand(required, "fluxnetworks");
    }

    public static boolean isPlayerSuperAdmin(EntityPlayer player) {
        if (!player.worldObj.isRemote) {
            SuperAdminData data = get(player);
            return data != null && data.getPermission();
        }
        return FluxNetworkCache.instance.superAdminClient;
    }
}
