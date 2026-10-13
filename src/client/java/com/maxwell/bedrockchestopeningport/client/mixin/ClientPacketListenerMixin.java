package com.maxwell.bedrockchestopeningport.client.mixin;

import com.maxwell.bedrockchestopeningport.client.ChestListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleOpenScreen", at = @At("TAIL"))
    private void onHandleOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.gui != null ? mc.gui.screen() : null;
        System.out.println("[DEBUG-BCCO] handleOpenScreen arrived! Title: "
                + packet.getTitle().getString()
                + ", CurrentScreen: " + currentScreen);
        if (currentScreen != null) {
            Screen replacement = ChestListener.handleScreenOpen(currentScreen);
            if (replacement != currentScreen) {
                mc.gui.setScreen(replacement);
            }
        }
    }
}