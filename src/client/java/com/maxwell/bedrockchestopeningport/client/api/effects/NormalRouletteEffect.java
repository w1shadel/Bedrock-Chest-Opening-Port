package com.maxwell.bedrockchestopeningport.client.api.effects;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;
import com.maxwell.bedrockchestopeningport.client.api.base.BaseRouletteEffect;


public final class NormalRouletteEffect extends BaseRouletteEffect {
    private boolean finished;

    public NormalRouletteEffect() {
        super(0.0);
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
        gui.addOffset(Math.max(0.0f, speed) * deltaTime);
        if (gui.getStopPoint() - gui.getOffset() <= 0.75f) {
            gui.setOffset(gui.getStopPoint());
            finished = true;
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void onComplete(CustomDropAnimationGui gui) {
        gui.playRewardSoundAndExit();
    }
}
