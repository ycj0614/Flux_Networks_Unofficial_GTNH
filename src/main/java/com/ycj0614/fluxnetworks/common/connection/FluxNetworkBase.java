package com.ycj0614.fluxnetworks.common.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import com.ycj0614.fluxnetworks.api.network.FluxLogicType;
import com.ycj0614.fluxnetworks.api.network.IFluxNetwork;
import com.ycj0614.fluxnetworks.api.network.NetworkMember;
import com.ycj0614.fluxnetworks.api.network.NetworkSettings;
import com.ycj0614.fluxnetworks.api.network.SecurityType;
import com.ycj0614.fluxnetworks.api.tiles.IFluxConnector;
import com.ycj0614.fluxnetworks.api.utils.ICustomValue;
import com.ycj0614.fluxnetworks.api.utils.NBTType;
import com.ycj0614.fluxnetworks.common.core.CustomValue;
import com.ycj0614.fluxnetworks.common.data.FluxNetworkData;

public abstract class FluxNetworkBase implements IFluxNetwork {

    // 필드 이름은 NetworkSettings의 람다(s -> s.network_id 등)와 정확히 일치해야 한다
    public ICustomValue<Integer> network_id = new CustomValue<>();
    public ICustomValue<String> network_name = new CustomValue<>();
    public ICustomValue<UUID> network_owner = new CustomValue<>();
    public ICustomValue<SecurityType> network_security = new CustomValue<>();
    public ICustomValue<String> network_password = new CustomValue<>();
    public ICustomValue<Integer> network_color = new CustomValue<>();
    public ICustomValue<Integer> network_wireless = new CustomValue<>(0);

    public ICustomValue<NetworkStatistics> network_stats = new CustomValue<>(new NetworkStatistics(this));
    public ICustomValue<List<IFluxConnector>> all_connectors = new CustomValue<>(new ArrayList<>());
    public ICustomValue<List<NetworkMember>> network_players = new CustomValue<>(new ArrayList<>());

    public FluxNetworkBase() {}

    public FluxNetworkBase(int id, String name, SecurityType security, int color, UUID owner, String password) {
        network_id.setValue(id);
        network_name.setValue(name);
        network_security.setValue(security);
        network_color.setValue(color);
        network_owner.setValue(owner);
        network_password.setValue(password);
    }

    @Override
    public <T> T getSetting(NetworkSettings<T> setting) {
        return setting.getValue(this)
            .getValue();
    }

    @Override
    public <T> void setSetting(NetworkSettings<T> settings, T value) {
        settings.getValue(this)
            .setValue(value);
    }

    @Override
    public void readNetworkNBT(NBTTagCompound nbt, NBTType type) {
        if (type == NBTType.NETWORK_GENERAL || type == NBTType.ALL_SAVE) {
            network_id.setValue(nbt.getInteger(FluxNetworkData.NETWORK_ID));
            network_name.setValue(nbt.getString(FluxNetworkData.NETWORK_NAME));
            // 1.12.2 setUniqueId/getUniqueId와 같은 키 규칙 (key + "Most" / key + "Least")
            network_owner.setValue(
                new UUID(
                    nbt.getLong(FluxNetworkData.OWNER_UUID + "Most"),
                    nbt.getLong(FluxNetworkData.OWNER_UUID + "Least")));
            network_security.setValue(SecurityType.values()[nbt.getInteger(FluxNetworkData.SECURITY_TYPE)]);
            network_password.setValue(nbt.getString(FluxNetworkData.NETWORK_PASSWORD));
            network_color.setValue(nbt.getInteger(FluxNetworkData.NETWORK_COLOR));
            network_wireless.setValue(nbt.getInteger(FluxNetworkData.WIRELESS_MODE));

            if (type == NBTType.ALL_SAVE) {
                FluxNetworkData.readPlayers(this, nbt);
                FluxNetworkData.readConnections(this, nbt);
            }
        }

        if (type == NBTType.NETWORK_PLAYERS) {
            FluxNetworkData.readPlayers(this, nbt);
        }

        if (type == NBTType.NETWORK_CONNECTIONS) {
            FluxNetworkData.readAllConnections(this, nbt);
        }
        if (type == NBTType.NETWORK_STATISTICS) {
            network_stats.getValue()
                .readNBT(nbt);
        }
    }

    @Override
    public void writeNetworkNBT(NBTTagCompound nbt, NBTType type) {
        if (type == NBTType.NETWORK_GENERAL || type == NBTType.ALL_SAVE) {
            nbt.setInteger(FluxNetworkData.NETWORK_ID, network_id.getValue());
            nbt.setString(FluxNetworkData.NETWORK_NAME, network_name.getValue());
            UUID owner = network_owner.getValue();
            nbt.setLong(FluxNetworkData.OWNER_UUID + "Most", owner.getMostSignificantBits());
            nbt.setLong(FluxNetworkData.OWNER_UUID + "Least", owner.getLeastSignificantBits());
            nbt.setInteger(
                FluxNetworkData.SECURITY_TYPE,
                network_security.getValue()
                    .ordinal());
            nbt.setString(FluxNetworkData.NETWORK_PASSWORD, network_password.getValue());
            nbt.setInteger(FluxNetworkData.NETWORK_COLOR, network_color.getValue());
            nbt.setInteger(FluxNetworkData.WIRELESS_MODE, network_wireless.getValue());

            if (type == NBTType.ALL_SAVE) {
                FluxNetworkData.writePlayers(this, nbt);
                FluxNetworkData.writeConnections(this, nbt);
            }
        }

        if (type == NBTType.NETWORK_PLAYERS) {
            FluxNetworkData.writeAllPlayers(this, nbt);
        }

        if (type == NBTType.NETWORK_CONNECTIONS) {
            all_connectors.getValue()
                .removeIf(IFluxConnector::isChunkLoaded);
            List<IFluxConnector> connectors = getConnections(FluxLogicType.ANY);
            connectors.forEach(
                f -> all_connectors.getValue()
                    .add(new FluxLiteConnector(f)));
            FluxNetworkData.writeAllConnections(this, nbt);
        }
        if (type == NBTType.NETWORK_STATISTICS) {
            network_stats.getValue()
                .writeNBT(nbt);
        }
        if (type == NBTType.NETWORK_CLEAR) {
            nbt.setBoolean("clear", true); // Nothing
        }
    }
}
