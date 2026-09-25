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

import java.lang.reflect.Field;
import java.util.List;

public class FakeTotemGui {

    private static final List<Talisman> TALISMANS = List.of(
            new Talisman("§k|§r §6★ §4§lТалисман рушителя", "§7Сносит тотем врага", 1001, 50000),
            new Talisman("§k█§r §c★ §5§lТалисман арателя", "§7ыжигает инвентарь", 1002, 75000),
            new Talisman("§ki§r §b★ §3§lТалисман Тени", "§7Скрывает от радаров", 1003, 120000)
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
            
            // ефлексия для добавления виджета, так как addDrawableChild protected
            addChildViaReflection(screen, btn);
            y += 24;
        }
    }

    @SuppressWarnings("unchecked")
    private static void addChildViaReflection(Object screenObj, ButtonWidget widget) {
        try {
            Field field = getFieldRecursively(screenObj.getClass(), "children");
            if (field != null) {
                field.setAccessible(true);
                List<Object> children = (List<Object>) field.get(screenObj);
                if (children != null) {
                    children.add(widget);
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("[FakeTotem] Reflection failed: " + e.getMessage());
        }
    }

    private static Field getFieldRecursively(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static void sellFakeTotem(Talisman talisman) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastSellTime < 3600000L) {
            client.player.sendMessage(Text.literal("§c[FakeTotem] улдаун 1 час."), false);
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        if (!mainHand.isOf(Items.TOTEM_OF_UNDYING)) {
            client.player.sendMessage(Text.literal("§c[FakeTotem] озьми обычный тотем."), false);
            return;
        }

        ItemStack originalStack = mainHand.copy();
        int selectedSlot = client.player.getInventory().selectedSlot;

        Text parsedName = parseLegacyFormatting(talisman.name());
        Text parsedLore = parseLegacyFormatting(talisman.lore());

        mainHand.set(DataComponentTypes.CUSTOM_NAME, parsedName);
        mainHand.set(DataComponentTypes.LORE, new LoreComponent(List.of(parsedLore)));
        
        // С: спользуем CustomModelDataComponent вместо int
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
            if (c == '§' && i + 1 < text.length()) {
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