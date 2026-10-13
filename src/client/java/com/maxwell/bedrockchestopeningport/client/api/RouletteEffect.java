package com.maxwell.bedrockchestopeningport.client.api;

import com.maxwell.bedrockchestopeningport.client.CustomDropAnimationGui;

public interface RouletteEffect {
    void update(CustomDropAnimationGui gui, float deltaTime);

    boolean isFinished();

    void onComplete(CustomDropAnimationGui gui);
}