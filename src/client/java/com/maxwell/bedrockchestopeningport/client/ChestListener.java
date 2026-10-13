package com.maxwell.bedrockchestopeningport.client;

import com.maxwell.bedrockchestopeningport.client.DataUtil.DungeonBossLootManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChestListener {
    private static final boolean TEST_MODE = true;
    private static String curFloor = "F7";
    private static boolean openedChestBr = false;
    private static boolean openedChestOb = false;
    private static boolean isCroesusSession = false;

    @SuppressWarnings("unchecked")
    public static Screen handleScreenOpen(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return screen;
        }
        if (!(containerScreen.getMenu() instanceof ChestMenu)) {
            return screen;
        }
        if (TEST_MODE) {
            String testFloor = "F7";
            String testChest = "Bedrock";
            List<ItemStack> f7Pool = DungeonBossLootManager.getFloorDrops(testFloor, testChest);
            ItemStack randomReward = DungeonBossLootManager.getRandomFloorJackpot(testFloor, testChest);
            System.out.println("[DEBUG-BCCO] Test Mode Roll -> " + randomReward.getHoverName().getString());
            return new CustomDropAnimationGui(randomReward, f7Pool, testFloor, testChest, containerScreen);
        }
        String rawTitle = screen.getTitle().getString();
        String cleanTitle = rawTitle.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        System.out.println("[DEBUG-BCCO] Cleaned Title: '" + cleanTitle + "'");
        if (cleanTitle.contains("Croesus")) {
            isCroesusSession = true;
            openedChestBr = false;
            openedChestOb = false;
            return screen;
        }
        if (cleanTitle.contains("Catacombs") || cleanTitle.contains("Floor")) {
            String detected = parseFloorFromTitle(cleanTitle);
            if (detected != null) {
                curFloor = detected;
                System.out.println("[DEBUG-BCCO] Croesus Detected Floor from Title: " + curFloor);
            }
            openedChestBr = false;
            openedChestOb = false;
            return screen;
        }
        if (!isCroesusSession) {
            detectFloorFromScoreboard();
        }
        boolean isBedrock = cleanTitle.contains("Bedrock");
        boolean isObsidian = cleanTitle.contains("Obsidian");
        if (isBedrock || isObsidian) {
            String chestType = isBedrock ? "Bedrock" : "Obsidian";
            if (isBedrock) {
                if (!openedChestBr) openedChestBr = true;
                else return screen;
            } else {
                if (!openedChestOb) openedChestOb = true;
                else return screen;
            }
            System.out.println("[DEBUG-BCCO] Intercepting Dungeon Chest: " + cleanTitle + " (Floor: " + curFloor + ")");
            return new InterceptChestScreen((AbstractContainerScreen<ChestMenu>) containerScreen, curFloor, chestType, false);
        }
        return screen;
    }

    private static ItemStack createTestItem(String id, Item material, String displayName) {
        ItemStack stack = new ItemStack(material);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(displayName));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag extra = new CompoundTag();
            extra.putString("id", id);
            root.put("ExtraAttributes", extra);
        });
        return stack;
    }

    private static String parseFloorFromTitle(String title) {
        boolean isMaster = title.contains("Master");
        String[] roman = {"VII", "VI", "IV", "V", "III", "II", "I"};
        int[] num = {7, 6, 4, 5, 3, 2, 1};
        for (int i = 0; i < roman.length; i++) {
            if (title.matches(".*\\b" + roman[i] + "\\b.*")) {
                return (isMaster ? "M" : "F") + num[i];
            }
        }
        Matcher m = Pattern.compile("Floor\\s*([1-7])", Pattern.CASE_INSENSITIVE).matcher(title);
        if (m.find()) {
            return (isMaster ? "M" : "F") + m.group(1);
        }
        return null;
    }

    private static void detectFloorFromScoreboard() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Scoreboard sb = mc.level.getScoreboard();
        Objective sidebar = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return;
        for (var scoreHolder : sb.getTrackedPlayers()) {
            PlayerTeam team = sb.getPlayersTeam(scoreHolder.getScoreboardName());
            String line = scoreHolder.getScoreboardName();
            if (team != null) {
                line = team.getPlayerPrefix().getString() + line + team.getPlayerSuffix().getString();
            }
            line = line.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            if (line.contains("The Catacombs (")) {
                Matcher m = Pattern.compile("\\((.*?)\\)").matcher(line);
                if (m.find()) {
                    curFloor = m.group(1).trim().toUpperCase();
                    System.out.println("[DEBUG-BCCO] Scoreboard Detected Dungeon Floor: " + curFloor);
                }
                break;
            }
        }
    }

    public static void reset() {
        openedChestBr = false;
        openedChestOb = false;
        isCroesusSession = false;
    }
}