package com.maxwell.bedrockchestopeningport.client.api.base;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;


public interface RouletteEffect {
    
    default double getChancePercent() {
        return 0.0;
    }

    
    default boolean canApply(CustomDropAnimationGui gui) {
        return true;
    }

    
    default void onStart(CustomDropAnimationGui gui) {
    }

    void update(CustomDropAnimationGui gui, float deltaTime);

    boolean isFinished();

    void onComplete(CustomDropAnimationGui gui);
}
