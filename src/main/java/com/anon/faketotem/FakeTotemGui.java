package com.anon.faketotem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class FakeTotemGui {

    // \u041a\u0440\u0443\u0448\u0438\u0442\u0435\u043b\u044c = рушитель
    // \u041a\u0430\u0440\u0430\u0442\u0435\u043b\u044c = аратель
    // \u0422\u0435\u043d\u044c = Тень
    // \u2605 = ★ (Star)
    private static final List<Talisman> TALISMANS = List.of(
            new Talisman("\u00a7k|\u00a7r \u00a76\u2605 \u00a74\u00a7l\u0422\u0430\u043b\u0438\u0441\u043c\u0430\u043d \u041a\u0440\u0443\u0448\u0438\u0442\u0435\u043b\u044c", "\u00a77\u0421\u043d\u043e\u0441\u0438\u0442 \u0442\u043e\u0442\u0435\u043c \u0432\u0440\u0430\u0433\u0430", 1001, 50000),
            new Talisman("\u00a7k\u2588\u00a7r \u00a7c\u2605 \u00a75\u00a7l\u0422\u0430\u043b\u0438\u0441\u043c\u0430\u043d \u041a\u0430\u0440\u0430\u0442\u0435\u043b\u044c", "\u00a77\u0412\u044b\u0436\u0438\u0433\u0430\u0435\u0442 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c", 1002, 75000),
            new Talisman("\u00a7ki\u00a7r \u00a7b\u2605 \u00a73\u00a7l\u0422\u0430\u043b\u0438\u0441\u043c\u0430\u043d \u0422\u0435\u043d\u044c", "\u00a77\u0421\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u043e\u0442 \u0440\u0430\u0434\u0430\u0440\u043e\u0432", 1003, 120000)
    );

    private static long lastSellTime = 0;

    public static void injectPhantomSlots(GenericContainerScreen screen) {
        int x = screen.width / 2 + 95;
        int y = screen.height / 2 - 40;

        for (Talisman talisman : TALISMANS) {
            Text buttonText = parseLegacyFormatting(talisman.name());
            
            ButtonWidget btn = ButtonWidget.builder(
                    buttonText,
                    button -> sellFakeTotem(talisman)
            ).dimensions(x, y, 110, 20).build();
            
            // Теперь этот метод публичный благодаря AccessWidener
            screen.addDrawableChild(btn);
            y += 24;
        }
    }

    private static void sellFakeTotem(Talisman talisman) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastSellTime < 3600000L) {
            client.player.sendMessage(Text.literal("\u00a7c[FakeTotem] \u041a\u0443\u043b\u0434\u0430\u0443\u043d 1 \u0447\u0430\u0441."), false);
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        if (!mainHand.isOf(Items.TOTEM_OF_UNDYING)) {
            client.player.sendMessage(Text.literal("\u00a7c[FakeTotem] \u0412\u043e\u0437\u044c\u043c\u0438 \u043e\u0431\u044b\u0447\u043d\u044b\u0439 \u0442\u043e\u0442\u0435\u043c."), false);
            return;
        }

        ItemStack originalStack = mainHand.copy();
        int selectedSlot = client.player.getInventory().selectedSlot;

        Text parsedName = parseLegacyFormatting(talisman.name());
        Text parsedLore = parseLegacyFormatting(talisman.lore());

        mainHand.set(DataComponentTypes.CUSTOM_NAME, parsedName);
        mainHand.set(DataComponentTypes.LORE, new LoreComponent(List.of(parsedLore)));
        mainHand.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(talisman.modelData()));
        mainHand.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        NbtComponent existingCustomData = mainHand.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound customData = (existingCustomData != null) ? existingCustomData.copyNbt() : new NbtCompound();
        customData.putInt("CustomModelData", talisman.modelData());
        mainHand.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

        client.getNetworkHandler().sendCommand("ah sell " + talisman.price());
        lastSellTime = now;

        client.execute(() -> {
            if (client.player != null) {
                client.player.getInventory().setStack(selectedSlot, originalStack);
            }
        });
    }

    private static Text parseLegacyFormatting(String text) {
        MutableText result = Text.empty();
        StringBuilder current = new StringBuilder();
        Style currentStyle = Style.EMPTY;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                Formatting formatting = Formatting.byCode(code);
                if (formatting != null) {
                    if (current.length() > 0) {
                        result.append(Text.literal(current.toString()).setStyle(currentStyle));
                        current.setLength(0);
                    }
                    if (formatting == Formatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else {
                        currentStyle = currentStyle.withFormatting(formatting);
                    }
                    i++; 
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) {
            result.append(Text.literal(current.toString()).setStyle(currentStyle));
        }
        return result;
    }
}