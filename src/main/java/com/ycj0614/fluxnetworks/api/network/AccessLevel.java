package com.ycj0614.fluxnetworks.api.network;

import net.minecraft.util.EnumChatFormatting;

import com.ycj0614.fluxnetworks.api.translate.FluxTranslate;
import com.ycj0614.fluxnetworks.api.translate.Translation;

public enum AccessLevel {

    OWNER(FluxTranslate.OWNER, 0xffaa00, EnumChatFormatting.GOLD),
    ADMIN(FluxTranslate.ADMIN, 0x66cc00, EnumChatFormatting.GREEN),
    USER(FluxTranslate.USER, 0x6699ff, EnumChatFormatting.BLUE),
    NONE(FluxTranslate.BLOCKED, 0xa9a9a9, EnumChatFormatting.GRAY),
    SUPER_ADMIN(FluxTranslate.SUPER_ADMIN, 0x4b0082, EnumChatFormatting.DARK_PURPLE);

    public Translation localization;
    public int color;
    public EnumChatFormatting formatting;

    AccessLevel(Translation localization, int color, EnumChatFormatting formatting) {
        this.localization = localization;
        this.color = color;
        this.formatting = formatting;
    }

    public String getName() {
        return formatting + localization.t();
    }

    public int getColor() {
        return color;
    }

    public boolean canAccess() {
        return this != NONE;
    }

    public boolean canEdit() {
        return canAccess() && this != USER;
    }

    public boolean canDelete() {
        return this == OWNER || this == SUPER_ADMIN;
    }

}
