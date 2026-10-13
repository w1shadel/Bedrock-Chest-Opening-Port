package com.maxwell.bedrockchestopeningport.client;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;
import com.maxwell.bedrockchestopeningport.client.DataUtil.DropRarity;
import com.maxwell.bedrockchestopeningport.client.DataUtil.RarityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InterceptChestScreen extends AbstractContainerScreen<ChestMenu> {

    private int timeoutTicks = 20; 
    private final boolean isTestMode;
    private final AbstractContainerScreen<ChestMenu> originalScreen;

    public InterceptChestScreen(AbstractContainerScreen<ChestMenu> originalScreen, boolean isTestMode) {
        super(originalScreen.getMenu(), Minecraft.getInstance().player.getInventory(), originalScreen.getTitle());
        this.originalScreen = originalScreen;
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

            chestPool.add(stack.copy());
            DropRarity rarity = RarityResolver.resolveRarity(stack);

            if (bestRarity == null || rarity.getIndex() < bestRarity.getIndex()) {
                bestRarity = rarity;
                bestRewardStack = stack.copy();
            }
        }

        if (!bestRewardStack.isEmpty()) {
            System.out.println("[DEBUG-BCCO] Items loaded! Best item: "
                    + bestRewardStack.getHoverName().getString()
                    + " (" + bestRarity + ")");

            Minecraft.getInstance().gui.setScreen(
                    new CustomDropAnimationGui(bestRewardStack, chestPool, originalScreen)
            );
            return;
        }

        if (--timeoutTicks <= 0) {
            System.out.println("[DEBUG-BCCO] Timed out waiting for items, restoring screen.");
            Minecraft.getInstance().gui.setScreen(originalScreen);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }
}