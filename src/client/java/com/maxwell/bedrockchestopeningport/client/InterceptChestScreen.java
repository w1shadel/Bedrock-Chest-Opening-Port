package com.maxwell.bedrockchestopeningport.client;

import com.maxwell.bedrockchestopeningport.client.DataUtil.DropRarity;
import com.maxwell.bedrockchestopeningport.client.DataUtil.RarityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InterceptChestScreen extends AbstractContainerScreen<ChestMenu> {
    private final boolean isTestMode;
    private final String floor;
    private final String chestType;
    private final AbstractContainerScreen<ChestMenu> originalScreen;
    private int timeoutTicks = 20;

    public InterceptChestScreen(AbstractContainerScreen<ChestMenu> originalScreen, String floor, String chestType, boolean isTestMode) {
        super(originalScreen.getMenu(), Minecraft.getInstance().player.getInventory(), originalScreen.getTitle());
        this.originalScreen = originalScreen;
        this.floor = (floor != null && !floor.isEmpty()) ? floor : "F7";
        this.chestType = (chestType != null && !chestType.isEmpty()) ? chestType : "Bedrock";
        this.isTestMode = isTestMode;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int startSlot = isTestMode ? 0 : 10;
        int endSlot = isTestMode ? (this.menu.slots.size() - 36) : 17;
        if (endSlot <= startSlot) endSlot = this.menu.slots.size();
        ItemStack bestRewardStack = ItemStack.EMPTY;
        DropRarity bestRarity = null;
        List<ItemStack> chestPool = new ArrayList<>();
        for (int i = startSlot; i < endSlot; i++) {
            if (i >= this.menu.slots.size()) break;
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (stack == null || stack.isEmpty()) continue;
            if (isSpacer(stack)) continue;
            chestPool.add(stack.copy());
            DropRarity rarity = RarityResolver.resolveRarity(stack);
            if (bestRarity == null || rarity.getIndex() < bestRarity.getIndex()) {
                bestRarity = rarity;
                bestRewardStack = stack.copy();
            }
        }
        if (!bestRewardStack.isEmpty()) {
            System.out.println("[DEBUG-BCCO] Valid items loaded! Best item: "
                    + bestRewardStack.getHoverName().getString()
                    + " (" + bestRarity + "), Floor: " + floor);
            Minecraft.getInstance().gui.setScreen(
                    new CustomDropAnimationGui(bestRewardStack, chestPool, floor, chestType, originalScreen)
            );
            return;
        }
        if (--timeoutTicks <= 0) {
            System.out.println("[DEBUG-BCCO] Timed out waiting for items, restoring screen.");
            Minecraft.getInstance().gui.setScreen(originalScreen);
        }
    }

    private boolean isSpacer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (itemPath.contains("glass_pane")) {
            return true;
        }
        String cleanName = stack.getHoverName().getString()
                .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .trim();
        return cleanName.isEmpty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }
}