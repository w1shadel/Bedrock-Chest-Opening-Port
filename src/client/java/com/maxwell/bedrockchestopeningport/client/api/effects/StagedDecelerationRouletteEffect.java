package com.maxwell.bedrockchestopeningport.client.api.effects;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;
import com.maxwell.bedrockchestopeningport.client.api.base.BaseRouletteEffect;


public final class StagedDecelerationRouletteEffect extends BaseRouletteEffect {
    private boolean finished;

    public StagedDecelerationRouletteEffect() {
        super(10.0); 
    }

    @Override
    public void update(CustomDropAnimationGui gui, float deltaTime) {
        float stop = gui.getStopPoint();
        float remaining = stop - gui.getOffset();
        if (remaining <= 0.75f) {
            gui.setOffset(stop);
            finished = true;
            return;
        }

        float speed = (float) gui.calculateVelocity(gui.getOffset() - gui.getSlowPoint());
        float slowDistance = Math.max(1.0f, stop - gui.getSlowPoint());
        float progress = Math.max(0.0f, Math.min(1.0f, (gui.getOffset() - gui.getSlowPoint()) / slowDistance));
        float multiplier = progress < (1.0f / 3.0f) ? 1.0f : (progress < (2.0f / 3.0f) ? 0.72f : 0.48f);
        gui.addOffset(Math.max(0.0f, speed * multiplier) * deltaTime);
        if (stop - gui.getOffset() <= 0.75f) {
            gui.setOffset(stop);
            finished = true;
        }
    }

    @Override public boolean isFinished() { return finished; }
    @Override public void onComplete(CustomDropAnimationGui gui) { gui.playRewardSoundAndExit(); }
}
