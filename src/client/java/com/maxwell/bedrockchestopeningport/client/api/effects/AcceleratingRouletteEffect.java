package com.maxwell.bedrockchestopeningport.client.api.effects;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;
import com.maxwell.bedrockchestopeningport.client.api.base.BaseRouletteEffect;


public final class AcceleratingRouletteEffect extends BaseRouletteEffect {
    private float elapsed;
    private boolean finished;

    public AcceleratingRouletteEffect() {
        super(12.0); 
    }

    @Override
    public void update(CustomDropAnimationGui gui, float deltaTime) {
        float remaining = gui.getStopPoint() - gui.getOffset();
        if (remaining <= 0.75f) {
            gui.setOffset(gui.getStopPoint());
            finished = true;
            return;
        }
        float speed = (float) gui.calculateVelocity(gui.getOffset() - gui.getSlowPoint());
        if (gui.getOffset() < gui.getSlowPoint()) {
            elapsed += deltaTime;
            float multiplier = Math.min(2.0f, 0.55f + elapsed * 0.7f);
            speed *= multiplier;
        }
        gui.addOffset(Math.max(0.0f, speed) * deltaTime);
        if (gui.getStopPoint() - gui.getOffset() <= 0.75f) {
            gui.setOffset(gui.getStopPoint());
            finished = true;
        }
    }

    @Override public boolean isFinished() { return finished; }
    @Override public void onComplete(CustomDropAnimationGui gui) { gui.playRewardSoundAndExit(); }
}
