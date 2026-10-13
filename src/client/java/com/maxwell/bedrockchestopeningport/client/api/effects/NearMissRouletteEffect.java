package com.maxwell.bedrockchestopeningport.client.api.effects;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;
import com.maxwell.bedrockchestopeningport.client.api.base.BaseRouletteEffect;


public final class NearMissRouletteEffect extends BaseRouletteEffect {
    private enum State { APPROACHING, HESITATING, RECOVERING, FINISHED }
    private State state = State.APPROACHING;
    private long hesitationStartedNanos;

    public NearMissRouletteEffect() {
        super(8.0); 
    }

    @Override
    public void update(CustomDropAnimationGui gui, float deltaTime) {
        float target = gui.getStopPoint();
        float nearPoint = Math.max(0.0f, target - gui.getSpacing());
        switch (state) {
            case APPROACHING -> {
                if (nearPoint - gui.getOffset() <= 0.75f) {
                    gui.setOffset(nearPoint);
                    state = State.HESITATING;
                    hesitationStartedNanos = System.nanoTime();
                } else {
                    float speed = (float) gui.calculateVelocity(gui.getOffset() - gui.getSlowPoint());
                    gui.addOffset(Math.max(0.0f, speed) * deltaTime);
                    if (gui.getOffset() >= nearPoint) {
                        gui.setOffset(nearPoint);
                        state = State.HESITATING;
                        hesitationStartedNanos = System.nanoTime();
                    }
                }
            }
            case HESITATING -> {
                if (System.nanoTime() - hesitationStartedNanos >= 180_000_000L) {
                    state = State.RECOVERING;
                }
            }
            case RECOVERING -> {
                float remaining = target - gui.getOffset();
                if (remaining <= 0.75f) {
                    gui.setOffset(target);
                    state = State.FINISHED;
                } else {
                    gui.addOffset(remaining * 5.0f * deltaTime);
                    if (target - gui.getOffset() <= 0.75f) {
                        gui.setOffset(target);
                        state = State.FINISHED;
                    }
                }
            }
            case FINISHED -> { }
        }
    }

    @Override public boolean isFinished() { return state == State.FINISHED; }
    @Override public void onComplete(CustomDropAnimationGui gui) { gui.playRewardSoundAndExit(); }
}
