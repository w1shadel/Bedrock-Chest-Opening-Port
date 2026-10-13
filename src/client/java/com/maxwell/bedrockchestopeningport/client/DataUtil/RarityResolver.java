package com.maxwell.bedrockchestopeningport.client.DataUtil;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class RarityResolver {

    
    public static DropRarity resolveRarity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return DropRarity.COMMON;

        String name = stack.getHoverName().getString().toUpperCase();

        if (name.contains("HANDLE") || name.endsWith("SCROLL") || name.contains("CLAYMORE")) {
            return DropRarity.PRAYTORNG;
        }

        if (name.endsWith("THE FISH")) {
            return DropRarity.FISH;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            List<Component> lines = lore.lines();
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i).getString().toUpperCase();

                if (line.contains("PRAY TO RNGESUS") || line.contains("SPECIAL")) {
                    return DropRarity.PRAYTORNG;
                }
                if (line.contains("DIVINE")) {
                    return DropRarity.DIVINE;
                }
                if (line.contains("MYTHIC")) {
                    return DropRarity.MYTHIC;
                }
                if (line.contains("LEGENDARY")) {
                    return DropRarity.LEGENDARY;
                }
                if (line.contains("EPIC")) {
                    return DropRarity.EPIC;
                }
                if (line.contains("RARE")) {
                    return DropRarity.FISH; 
                }
                if (line.contains("UNCOMMON") || line.contains("COMMON")) {
                    return DropRarity.COMMON;
                }
            }
        }

        String rawName = stack.getHoverName().getString();
        if (rawName.startsWith("§c") || rawName.startsWith("§4")) return DropRarity.PRAYTORNG; 
        if (rawName.startsWith("§b")) return DropRarity.DIVINE;   
        if (rawName.startsWith("§d")) return DropRarity.MYTHIC;   
        if (rawName.startsWith("§6")) return DropRarity.LEGENDARY;
        if (rawName.startsWith("§5")) return DropRarity.EPIC;     
        if (rawName.startsWith("§9")) return DropRarity.FISH;     
        if (rawName.startsWith("§a") || rawName.startsWith("§f")) return DropRarity.COMMON;

        return DropRarity.COMMON;
    }
}