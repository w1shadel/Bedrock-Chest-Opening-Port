package com.maxwell.bedrockchestopeningport.client.api.effects;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;
import com.maxwell.bedrockchestopeningport.client.api.base.BaseRouletteEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;


public final class FakeoutRouletteEffect extends BaseRouletteEffect {
    private enum State { APPROACHING, PAUSED, SLIDING, FINISHED }
    private State state = State.APPROACHING;
    private long pauseStartedNanos;

    public FakeoutRouletteEffect() {
        super(5.0); 
    }

    @Override
    public void update(CustomDropAnimationGui gui, float deltaTime) {
        float target = gui.getStopPoint();
        float fakeStop = Math.max(0.0f, target - gui.getSpacing());
        switch (state) {
            case APPROACHING -> {
                if (fakeStop - gui.getOffset() <= 0.75f) {
                    gui.setOffset(fakeStop);
                    state = State.PAUSED;
                    pauseStartedNanos = System.nanoTime();
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
                } else {
                    float speed = (float) gui.calculateVelocity(gui.getOffset() - gui.getSlowPoint());
                    gui.addOffset(Math.max(0.0f, speed) * deltaTime);
                    if (gui.getOffset() >= fakeStop) {
                        gui.setOffset(fakeStop);
                        state = State.PAUSED;
                        pauseStartedNanos = System.nanoTime();
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
                    }
                }
            }
            case PAUSED -> {
                if (System.nanoTime() - pauseStartedNanos >= 600_000_000L) {
                    state = State.SLIDING;
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5F));
                }
            }
            case SLIDING -> {
                float remaining = target - gui.getOffset();
                if (remaining <= 0.75f) {
                    gui.setOffset(target);
                    state = State.FINISHED;
                } else {
                    gui.addOffset(Math.max(0.0f, remaining * 8.0f) * deltaTime);
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
