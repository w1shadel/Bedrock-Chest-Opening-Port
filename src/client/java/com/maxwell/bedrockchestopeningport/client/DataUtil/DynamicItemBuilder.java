package com.maxwell.bedrockchestopeningport.client.DataUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DynamicItemBuilder {
    private static final String NEU_ITEM_BASE =
            "https:/" + "/raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/";
    private static final Path ITEM_CACHE_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("bedrock-chest-opening").resolve("item_cache");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    static {
        try {
            Files.createDirectories(ITEM_CACHE_DIR);
        } catch (Exception ignored) {
        }
    }

    public static ItemStack build(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return new ItemStack(Items.NETHER_STAR);
        }
        try {
            JsonObject json = fetchItemJson(internalName);
            if (json != null) {
                return parseItemStackFromJson(json, internalName);
            }
        } catch (Exception e) {
            System.out.println("[BCCO] Failed to dynamically build item " + internalName + ": " + e.getMessage());
        }
        return createFallbackStack(internalName);
    }

    private static JsonObject fetchItemJson(String internalName) {
        Path cacheFile = ITEM_CACHE_DIR.resolve(internalName + ".json");
        try {
            if (Files.exists(cacheFile)) {
                try (var reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
            String url = NEU_ITEM_BASE + internalName + ".json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                Files.writeString(cacheFile, body, StandardCharsets.UTF_8);
                return JsonParser.parseString(body).getAsJsonObject();
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static ItemStack parseItemStackFromJson(JsonObject json, String internalName) {
        String itemIdStr = json.has("itemid") ? json.get("itemid").getAsString() : "minecraft:player_head";
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(itemIdStr)).orElse(Items.PLAYER_HEAD);
        ItemStack stack = new ItemStack(item);
        if (json.has("displayname")) {
            String displayName = json.get("displayname").getAsString();
            stack.set(DataComponents.CUSTOM_NAME, LegacyStringParser.parse(displayName));
        }
        if (json.has("lore") && json.get("lore").isJsonArray()) {
            List<Component> loreComponents = new ArrayList<>();
            for (var elem : json.getAsJsonArray("lore")) {
                loreComponents.add(LegacyStringParser.parse(elem.getAsString()));
            }
            if (!loreComponents.isEmpty()) {
                stack.set(DataComponents.LORE, new ItemLore(loreComponents));
            }
        }
        if (json.has("nbttag")) {
            String nbtString = json.get("nbttag").getAsString();
            try {
                String normalizedNbt = preprocessNeuSnbt(nbtString);
                CompoundTag tag = TagParser.parseCompoundFully(normalizedNbt);
                if (tag.contains("SkullOwner")) {
                    CompoundTag skullOwner = tag.getCompound("SkullOwner").get();
                    ResolvableProfile profile = resolveProfile(skullOwner);
                    if (profile != null) {
                        stack.set(DataComponents.PROFILE, profile);
                    }
                }
                String itemModel = tag.getStringOr("ItemModel", "");
                if (!itemModel.isEmpty()) {
                    Identifier modelId = Identifier.tryParse(itemModel);
                    if (modelId != null) {
                        stack.set(DataComponents.ITEM_MODEL, modelId);
                    }
                }
                if (tag.contains("ExtraAttributes")) {
                    CompoundTag extra = tag.getCompound("ExtraAttributes").get();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
                        root.merge(extra);
                        root.putString("id", internalName);
                    });
                }
            } catch (Exception e) {
                applyBasicExtraAttributes(stack, internalName);
            }
        } else {
            applyBasicExtraAttributes(stack, internalName);
        }
        return stack;
    }

    private static void applyBasicExtraAttributes(ItemStack stack, String internalName) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag extra = new CompoundTag();
            extra.putString("id", internalName);
            root.put("ExtraAttributes", extra);
            root.putString("id", internalName);
        });
    }

    private static ItemStack createFallbackStack(String internalName) {
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + internalName));
        applyBasicExtraAttributes(stack, internalName);
        return stack;
    }

    private static String preprocessNeuSnbt(String snbt) {
        if (snbt == null || snbt.isEmpty()) return snbt;
        StringBuilder result = null;
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);
            if (escape) {
                if (result != null) result.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                if (result != null) result.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                if (result != null) result.append(c);
                continue;
            }
            if (!inString && (c == '[' || c == ',')) {
                int j = i + 1;
                while (j < snbt.length() && Character.isWhitespace(snbt.charAt(j))) j++;
                int digitStart = j;
                while (j < snbt.length() && Character.isDigit(snbt.charAt(j))) j++;
                if (j > digitStart && j < snbt.length() && snbt.charAt(j) == ':') {
                    if (result == null) {
                        result = new StringBuilder(snbt.length());
                        result.append(snbt, 0, i);
                    }
                    result.append(c);
                    i = j;
                    continue;
                }
                if (result != null) result.append(c);
                continue;
            }
            if (result != null) result.append(c);
        }
        return result == null ? snbt : result.toString();
    }

    private static ResolvableProfile resolveProfile(CompoundTag skullOwner) {
        try {
            UUID uuid = UUID.randomUUID();
            if (skullOwner.contains("Id")) {
                String idStr = skullOwner.getStringOr("Id", "");
                if (!idStr.isEmpty()) {
                    try {
                        uuid = UUID.fromString(idStr);
                    } catch (Exception ignored) {
                    }
                }
            }
            String name = skullOwner.getStringOr("Name", "SkyBlock");
            if (skullOwner.contains("Properties")) {
                CompoundTag properties = skullOwner.getCompound("Properties").get();
                ListTag textures = properties.getListOrEmpty("textures");
                if (!textures.isEmpty()) {
                    CompoundTag firstTexture = textures.getCompoundOrEmpty(0);
                    String value = firstTexture.getStringOr("Value", "");
                    if (!value.isEmpty()) {
                        Multimap<String, Property> props = ArrayListMultimap.create();
                        props.put("textures", new Property("textures", value));
                        GameProfile profile = new GameProfile(uuid, name, new com.mojang.authlib.properties.PropertyMap(props));
                        return ResolvableProfile.createResolved(profile);
                    }
                }
            }
            return ResolvableProfile.createUnresolved(uuid);
        } catch (Exception e) {
            return null;
        }
    }
}