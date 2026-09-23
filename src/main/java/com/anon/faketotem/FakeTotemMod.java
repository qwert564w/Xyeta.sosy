package com.anon.faketotem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

public class FakeTotemMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GenericContainerScreen containerScreen) {
                String title = containerScreen.getTitle().getString().toLowerCase();
                if (title.contains("аукцион") || title.contains("ah") || title.contains("auction")) {
                    FakeTotemGui.injectPhantomSlots(containerScreen);
                }
            }
        });
    }
}
