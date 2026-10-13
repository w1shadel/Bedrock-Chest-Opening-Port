package com.maxwell.bedrockchestopeningport.client.api.effects;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;
import com.maxwell.bedrockchestopeningport.client.api.base.BaseRouletteEffect;


public final class FlashRouletteEffect extends BaseRouletteEffect {
    private boolean finished;

    public FlashRouletteEffect() {
        super(15.0); 
    }

    @Override
    public void update(CustomDropAnimationGui gui, float deltaTime) {
        float remaining = gui.getStopPoint() - gui.getOffset();
        float pulseRange = Math.max(gui.getSpacing() * 3.0f, 1.0f);
        float progress = Math.max(0.0f, Math.min(1.0f, 1.0f - remaining / pulseRange));
        float pulse = (float) Math.sin(progress * Math.PI);
        gui.setEffectFlashAlpha(pulse * 0.18f);

        if (remaining <= 0.75f) {
            gui.setOffset(gui.getStopPoint());
            gui.setEffectFlashAlpha(0.0f);
            finished = true;
            return;
        }
        float speed = (float) gui.calculateVelocity(gui.getOffset() - gui.getSlowPoint());
        gui.addOffset(Math.max(0.0f, speed) * deltaTime);
        if (gui.getStopPoint() - gui.getOffset() <= 0.75f) {
            gui.setOffset(gui.getStopPoint());
            gui.setEffectFlashAlpha(0.0f);
            finished = true;
        }
    }

    @Override public boolean isFinished() { return finished; }
    @Override public void onComplete(CustomDropAnimationGui gui) { gui.setEffectFlashAlpha(0.0f); gui.playRewardSoundAndExit(); }
}
