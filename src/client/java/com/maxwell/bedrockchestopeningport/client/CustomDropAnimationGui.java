package com.maxwell.bedrockchestopeningport.client;

import com.maxwell.bedrockchestopeningport.client.DataUtil.DropRarity;
import com.maxwell.bedrockchestopeningport.client.DataUtil.DungeonBossLootManager;
import com.maxwell.bedrockchestopeningport.client.DataUtil.RarityResolver;
import com.maxwell.bedrockchestopeningport.client.config.MyConfig;
import com.maxwell.bedrockchestopeningport.client.api.base.RouletteEffect;
import com.maxwell.bedrockchestopeningport.client.api.base.RouletteEffectSelector;
import com.maxwell.bedrockchestopeningport.client.api.effects.AcceleratingRouletteEffect;
import com.maxwell.bedrockchestopeningport.client.api.effects.FakeoutRouletteEffect;
import com.maxwell.bedrockchestopeningport.client.api.effects.FlashRouletteEffect;
import com.maxwell.bedrockchestopeningport.client.api.effects.NearMissRouletteEffect;
import com.maxwell.bedrockchestopeningport.client.api.effects.NormalRouletteEffect;
import com.maxwell.bedrockchestopeningport.client.api.effects.StagedDecelerationRouletteEffect;
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
    private boolean completionHandled = false;
    private RouletteEffect activeEffect;
    private float effectFlashAlpha = 0.0f;
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
        double distance = stopPoint - slowPoint;
        double duration = Math.max(0.05, slowTime);
        if (distance <= 0 || x >= distance) return 0.0;
        if (x <= 0) return distance * 3.0 / duration;

        double normalizedDistance = Math.min(1.0, x / distance);
        double normalizedTime = 1.0 - Math.cbrt(1.0 - normalizedDistance);
        return distance * 3.0 * Math.pow(1.0 - normalizedTime, 2.0) / duration;
    }

    public float getOffset() { return offsetX; }
    public void setOffset(float offset) {
        this.offsetX = Math.max(0.0f, Math.min(stopPoint, offset));
    }
    public void addOffset(float amount) { setOffset(offsetX + amount); }
    public float getStopPoint() { return stopPoint; }
    public float getSlowPoint() { return slowPoint; }
    public float getSpacing() { return spacing; }

    public double calculateVelocity(double distanceFromSlowPoint) {
        return velocityFromX(distanceFromSlowPoint);
    }

    public double calculateVelocityCustom(double distanceFromSlowPoint, float targetStopPoint) {
        double distance = targetStopPoint - slowPoint;
        double duration = Math.max(0.05, slowTime);
        if (distance <= 0 || distanceFromSlowPoint >= distance) return 0.0;
        if (distanceFromSlowPoint <= 0) return distance * 3.0 / duration;
        double normalizedDistance = Math.min(1.0, distanceFromSlowPoint / distance);
        double normalizedTime = 1.0 - Math.cbrt(1.0 - normalizedDistance);
        return distance * 3.0 * Math.pow(1.0 - normalizedTime, 2.0) / duration;
    }

    
    public void playRewardSoundAndExit() {
        if (completionHandled) return;
        completionHandled = true;
        playRewardSound(rewardRarity.getIndex());
        Minecraft.getInstance().gui.setScreen(originalScreen);
    }

    @Override
    protected void init() {
        super.init();
        this.itemBoxWidth = this.width / 5f;
        this.itemBoxHeight = this.height / 4f;
        float itemBoxPadding = this.width / 64f;
        this.spacing = itemBoxWidth + itemBoxPadding;
        this.stopPoint = 41 * spacing + randStop * itemBoxWidth;
        float effectiveSlowDistance = Math.max(1.0f, slowDistance + randSlow);
        this.slowPoint = Math.max(0.0f, stopPoint - effectiveSlowDistance * spacing);


        this.activeEffect = RouletteEffectSelector.select(
                List.of(
                        new FakeoutRouletteEffect(),
                        new AcceleratingRouletteEffect(),
                        new NearMissRouletteEffect(),
                        new FlashRouletteEffect(),
                        new StagedDecelerationRouletteEffect()
                ),
                new NormalRouletteEffect(),
                this
        );
        this.activeEffect.onStart(this);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        long now = System.nanoTime();
        if (lastFrameTime == 0L) lastFrameTime = now;
        float deltaTime = (now - lastFrameTime) / 1_000_000_000f;
        deltaTime = Math.max(0.0f, Math.min(deltaTime, 0.05f));
        lastFrameTime = now;

        if (!hasShownResult) {
            if (activeEffect == null) {
                activeEffect = new NormalRouletteEffect();
                activeEffect.onStart(this);
            }
            activeEffect.update(this, deltaTime);
            if (activeEffect.isFinished()) {
                offsetX = stopPoint;
                hasShownResult = true;
            }
        }

        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        drawCarousel(graphics, centerX, centerY);
        renderJudgementLine(graphics, centerX, centerY);
        drawEffectOverlay(graphics);

        if (hasShownResult) {
            if (!completionHandled) activeEffect.onComplete(this);
            return;
        }

        int fadeWidth = this.width / 6;
        graphics.fillGradient(0, 0, fadeWidth, this.height, 0xFF000000, 0x00000000);
        graphics.fillGradient(this.width - fadeWidth, 0, this.width, this.height, 0x00000000, 0xFF000000);
    }

    public void setEffectFlashAlpha(float alpha) {
        this.effectFlashAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    private void drawEffectOverlay(GuiGraphicsExtractor graphics) {
        if (effectFlashAlpha <= 0.001f) return;
        int alpha = Math.max(0, Math.min(255, Math.round(effectFlashAlpha * 255.0f)));
        graphics.fill(0, 0, this.width, this.height, (alpha << 24) | 0x00FFD54A);
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
        int boxIndex = spacing > 0.0f ? (int) Math.floor(offsetX / spacing) : 0;
        if (lastBoxDistance >= 0 && boxIndex > lastBoxDistance) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
        }
        lastBoxDistance = boxIndex;
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