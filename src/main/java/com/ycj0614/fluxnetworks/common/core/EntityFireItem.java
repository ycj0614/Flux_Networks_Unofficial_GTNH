package com.ycj0614.fluxnetworks.common.core;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.ycj0614.fluxnetworks.common.registry.RegistryItems;

/**
 * 원작 EntityFireItem. enableOldRecipe일 때 떨어진 레드스톤 아이템 엔티티를 이것으로 바꾼다 (CommonProxy.onEntityAdded).
 * 불(inFire)에 처음 닿으면 레드스톤이 같은 개수의 Flux로 바뀐다.
 * <p>
 * 원작과 다름: 1.7.10의 Entity.dealFireDamage는 isImmuneToFire면 attackEntityFrom을 부르지 않아 불을 감지할 수 없다.
 * 그래서 isImmuneToFire는 끄고, attackEntityFrom에서 모든 불 피해(inFire, onFire, lava)를 막는다 (원작 isEntityInvulnerable 대체).
 */
public class EntityFireItem extends EntityItem {

    public boolean changed;

    public EntityFireItem(World world) {
        super(world);
    }

    public EntityFireItem(EntityItem toConvert) {
        this(toConvert.worldObj);
        NBTTagCompound copyTag = new NBTTagCompound();
        toConvert.writeToNBT(copyTag);
        readFromNBT(copyTag);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.isFireDamage()) {
            if (source == DamageSource.inFire && !changed && !worldObj.isRemote) {
                ItemStack stack = getEntityItem();
                if (stack != null && stack.getItem() == Items.redstone) {
                    setEntityItemStack(new ItemStack(RegistryItems.FLUX, stack.stackSize, 0));
                }
                changed = true;
            }
            return false;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public boolean isBurning() {
        return false;
    }
}
