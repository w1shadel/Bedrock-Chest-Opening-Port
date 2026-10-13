package com.maxwell.bedrockchestopeningport.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class MyConfig {
    public static final MyConfig CLIENT;
    public static final ModConfigSpec SPEC;

    public final ModConfigSpec.DoubleValue slowTime;
    public final ModConfigSpec.IntValue slowDistance;
    public final ModConfigSpec.DoubleValue textScale;
    public final ModConfigSpec.BooleanValue allowText;
    public final ModConfigSpec.BooleanValue debugMode;

    static {
        Pair<MyConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(MyConfig::new);
        CLIENT = pair.getLeft();
        SPEC = pair.getRight();
    }

    private MyConfig(ModConfigSpec.Builder builder) {
        builder.push("general");

        slowTime = builder
                .comment("Time during the wind down phase")
                .defineInRange("slowTime", 7.0, 1.0, 20.0);

        slowDistance = builder
                .comment("The number of caroucel of the wind down phase")
                .defineInRange("slowDistance", 20, 10, 40);

        textScale = builder
                .comment("Text size in the carousel")
                .defineInRange("textScale", 3.0, 1.0, 5.0);

        allowText = builder
                .comment("For people who don't like the enchating book tier-list meme")
                .define("allowText", true);

        debugMode = builder
                .comment("Use for dev to debug")
                .define("debugMode", false);

        builder.pop();
    }

    public static float getSlowTime() { return CLIENT.slowTime.get().floatValue(); }
    public static int getSlowDistance() { return CLIENT.slowDistance.get(); }
    public static float getTextScale() { return CLIENT.textScale.get().floatValue(); }
    public static boolean isAllowText() { return CLIENT.allowText.get(); }
    public static boolean isDebugMode() { return CLIENT.debugMode.get(); }
}