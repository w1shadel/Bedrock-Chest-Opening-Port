package com.maxwell.bedrockchestopeningport.client;

import com.maxwell.bedrockchestopeningport.client.DataUtil.DropRarity;
import com.maxwell.bedrockchestopeningport.client.DataUtil.DungeonBossLootManager;
import com.maxwell.bedrockchestopeningport.client.DataUtil.RarityResolver;
import com.maxwell.bedrockchestopeningport.client.config.MyConfig;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomDropAnimationGui extends Screen {
    private final String currentFloor;
    private final String chestType;
    private final Screen originalScreen;
    private final ItemStack actualRewardStack;
    private final DropRarity rewardRarity;
    private final List<ItemStack> poolItems;
    private final List<ItemStack> carouselItems = new ArrayList<>();
    private final int itemCount = 50;
    private final int rewardSlot = 44;
    private final float slowTime = MyConfig.getSlowTime();
    private final int slowDistance = MyConfig.getSlowDistance();
    private float currentScrollSpeed;
    private float offsetX = 0f;
    private int lastBoxDistance = 0;
    private boolean hasShownResult = false;
    private float itemBoxWidth, itemBoxHeight, spacing;
    private float stopPoint, slowPoint;
    private float randStop, randSlow;
    private long lastFrameTime = 0L;

    public CustomDropAnimationGui(ItemStack actualRewardStack, List<ItemStack> chestPool, String currentFloor, String chestType, Screen originalScreen) {
        super(Component.literal("Bedrock Chest Opening"));
        this.actualRewardStack = actualRewardStack;
        this.rewardRarity = RarityResolver.resolveRarity(actualRewardStack);
        this.poolItems = (chestPool != null && !chestPool.isEmpty()) ? chestPool : List.of(actualRewardStack);
        this.currentFloor = currentFloor != null ? currentFloor : "F7";
        this.chestType = chestType != null ? chestType : "Bedrock";
        this.originalScreen = originalScreen;
        buildCarousel();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.randStop = (float) randStopPoint();
        this.randSlow = (float) ThreadLocalRandom.current().nextDouble(-2, 2);
        this.lastFrameTime = System.nanoTime();
    }

    private static double randStopPoint() {
        double rand = ThreadLocalRandom.current().nextDouble(0, 100);
        if (rand < 40) return ThreadLocalRandom.current().nextDouble(0, 0.2);
        else if (rand < 60) return ThreadLocalRandom.current().nextDouble(0.2, 0.8);
        else return ThreadLocalRandom.current().nextDouble(0.8, 1.0);
    }

    private void buildCarousel() {
        for (int i = 0; i < itemCount; i++) {
            ItemStack sampled = poolItems.get(ThreadLocalRandom.current().nextInt(poolItems.size())).copy();
            carouselItems.add(sampled);
        }
        if (rewardRarity != DropRarity.PRAYTORNG) {
            int teaseSlot = ThreadLocalRandom.current().nextInt(41, 44);
            ItemStack jackpotTease = DungeonBossLootManager.getFloorJackpot(currentFloor, chestType);
            carouselItems.set(teaseSlot, jackpotTease);
        }
        carouselItems.set(rewardSlot, actualRewardStack);
    }

    private double velocityFromX(double x) {
        double X = stopPoint - slowPoint;
        double T = slowTime;
        if (x <= 0) return X * 3 / T;
        if (x >= X) return 0;
        double normX = x / X;
        double t = T * (1 - Math.cbrt(1 - normX));
        double nt = t / T;
        return X * 3 * Math.pow(1 - nt, 2) / T;
    }

    @Override
    protected void init() {
        super.init();
        this.itemBoxWidth = this.width / 5f;
        this.itemBoxHeight = this.height / 4f;
        float itemBoxPadding = this.width / 64f;
        this.spacing = itemBoxWidth + itemBoxPadding;
        this.stopPoint = 41 * spacing + randStop * itemBoxWidth;
        this.slowPoint = stopPoint - ((slowDistance + randSlow) * spacing);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        long now = System.nanoTime();
        if (lastFrameTime == 0) lastFrameTime = now;
        float deltaTime = (now - lastFrameTime) / 1_000_000_000f;
        deltaTime = Math.min(deltaTime, 0.05f);
        lastFrameTime = now;
        if (!hasShownResult) {
            currentScrollSpeed = (float) velocityFromX(offsetX - slowPoint);
            offsetX += currentScrollSpeed * deltaTime;
            if (currentScrollSpeed == 0) {
                hasShownResult = true;
            }
        }
        if (hasShownResult) {
            playRewardSound(rewardRarity.getIndex());
            Minecraft.getInstance().gui.setScreen(this.originalScreen);
            return;
        }
        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        drawCarousel(graphics, centerX, centerY);
        renderJudgementLine(graphics, centerX, centerY);
        int fadeWidth = this.width / 6;
        graphics.fillGradient(0, 0, fadeWidth, this.height, 0xFF000000, 0x00000000);
        graphics.fillGradient(this.width - fadeWidth, 0, this.width, this.height, 0x00000000, 0xFF000000);
    }

    private void drawCarousel(GuiGraphicsExtractor graphics, float centerX, float centerY) {
        for (int i = 0; i < carouselItems.size(); i++) {
            float x = centerX - (offsetX - (i - 3) * spacing);
            float y = centerY - itemBoxHeight / 2f;
            if ((x + itemBoxWidth) < 0 || x > this.width) continue;
            ItemStack stack = carouselItems.get(i);
            DropRarity rarity = RarityResolver.resolveRarity(stack);
            int boxColor = getBoxColor(rarity);
            graphics.fill((int) x, (int) y, (int) (x + itemBoxWidth), (int) (y + itemBoxHeight), 0x50000000);
            float barHeight = itemBoxHeight / 16f;
            graphics.fill((int) x, (int) (y + itemBoxHeight - barHeight), (int) (x + itemBoxWidth), (int) (y + itemBoxHeight), boxColor);
            renderItemModel(graphics, stack, x + itemBoxWidth / 2f, y + itemBoxHeight / 2.5f);
            if (MyConfig.isAllowText()) {
                String name = stack.getHoverName().getString().replaceAll("§[0-9a-fk-or]", "");
                float scale = MyConfig.getTextScale() * 0.4f;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + itemBoxWidth / 2f, y + itemBoxHeight * 0.75f);
                graphics.pose().scale(scale, scale);
                graphics.centeredText(this.font, name, 0, 0, boxColor);
                graphics.pose().popMatrix();
            }
        }
    }

    private void renderItemModel(GuiGraphicsExtractor graphics, ItemStack stack, float centerX, float centerY) {
        float itemScale = itemBoxHeight * 0.035f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(itemScale, itemScale);
        graphics.item(stack, -8, -8);
        graphics.pose().popMatrix();
    }

    private void renderJudgementLine(GuiGraphicsExtractor graphics, float centerX, float centerY) {
        float lineWidth = 2f;
        float lineHeight = itemBoxHeight * 1.2f;
        graphics.fill(
                (int) (centerX - lineWidth / 2),
                (int) (centerY - lineHeight / 2),
                (int) (centerX + lineWidth / 2),
                (int) (centerY + lineHeight / 2),
                0xFFFFA500
        );
        int boxDistance = (int) offsetX % (int) spacing;
        if (boxDistance < lastBoxDistance) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        lastBoxDistance = boxDistance;
    }

    private int getBoxColor(DropRarity rarity) {
        return switch (rarity) {
            case PRAYTORNG -> getRainbowGlowColor();
            case DIVINE -> 0xFF4EEBEB;
            case MYTHIC -> 0xFFF953F9;
            case LEGENDARY -> 0xFFEF9E01;
            case EPIC -> 0xFFAA00AA;
            case FISH -> 0xFF4C4BE2;
            case COMMON -> 0xFFE0DFE0;
        };
    }

    private int getRainbowGlowColor() {
        double time = System.currentTimeMillis() % 10000L / 1000.0;
        int rgb = Color.HSBtoRGB((float) time, 0.6f, 1.0f);
        return 0xFF000000 | rgb;
    }

    private void playRewardSound(int rarityIndex) {
        var soundManager = Minecraft.getInstance().getSoundManager();
        SoundEvent sound = switch (rarityIndex) {
            case 7 -> SoundEvents.GRASS_BREAK;
            case 6 -> SoundEvents.PLAYER_SPLASH;
            case 5 -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case 4 -> SoundEvents.FIREWORK_ROCKET_LAUNCH;
            case 3 -> SoundEvents.PLAYER_LEVELUP;
            case 2 -> SoundEvents.WITHER_SPAWN;
            case 1 -> SoundEvents.PLAYER_LEVELUP;
            default -> null;
        };
        if (sound != null) {
            soundManager.play(SimpleSoundInstance.forUI(sound, 1.0F));
        }
        if (rarityIndex == 1) {
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F));
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.WITHER_SPAWN, 1.0F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            Minecraft.getInstance().gui.setScreen(this.originalScreen);
            return true;
        }
        return super.keyPressed(event);
    }
}