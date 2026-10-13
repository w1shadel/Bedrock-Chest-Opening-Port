package com.maxwell.bedrockchestopeningport.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;

public class ChestListener {

    private static final boolean TEST_MODE = false;

    private static boolean openedChestBr = false;
    private static boolean openedChestOb = false;

    @SuppressWarnings("unchecked")
    public static Screen handleScreenOpen(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return screen;
        }

        if (!(containerScreen.getMenu() instanceof ChestMenu)) {
            return screen;
        }



        if (TEST_MODE) {
            return new InterceptChestScreen((AbstractContainerScreen<ChestMenu>) containerScreen, true);
        }


        String title = screen.getTitle().getString();

        if (title.endsWith(" Chest")) {
            boolean isBedrock = title.startsWith("Bedrock");
            boolean isObsidian = title.startsWith("Obsidian");

            if (isBedrock) {
                if (!openedChestBr) openedChestBr = true; else return screen;
            } else if (isObsidian) {
                if (!openedChestOb) openedChestOb = true; else return screen;
            }

            return new InterceptChestScreen((AbstractContainerScreen<ChestMenu>) containerScreen, false);
        }

        return screen;
    }

    public static void reset() {
        openedChestBr = false;
        openedChestOb = false;
    }
}