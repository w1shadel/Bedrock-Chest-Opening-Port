package com.maxwell.bedrockchestopeningport.client.DataUtil;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public class NBTUtils {

    public static CompoundTag getCustomData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        return customData.copyTag();
    }

    public static String getExtraAttributeId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "STACKNULL";

        CompoundTag root = getCustomData(stack);
        if (root == null) return "TAGNULL";

        Optional<CompoundTag> extraOpt = root.getCompound("ExtraAttributes");
        if (extraOpt.isEmpty()) return "KEYNULL";

        CompoundTag extra = extraOpt.get();

        Optional<String> idOpt = extra.getString("id");
        if (idOpt.isEmpty()) return "IDNULL";

        String id = idOpt.get();

        if ("ENCHANTED_BOOK".equalsIgnoreCase(id)) {
            Optional<CompoundTag> enchantsOpt = extra.getCompound("enchantments");
            if (enchantsOpt.isPresent()) {
                CompoundTag enchants = enchantsOpt.get();

                for (String key : enchants.keySet()) {

                    int level = enchants.getIntOr(key, 0);
                    return key + "_" + level;
                }
            }
            return "ENCHNULL";
        }

        return id;
    }
}