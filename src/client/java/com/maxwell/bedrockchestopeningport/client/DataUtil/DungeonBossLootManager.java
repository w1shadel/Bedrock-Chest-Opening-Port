package com.maxwell.bedrockchestopeningport.client.DataUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonBossLootManager {
    private static final String NEU_REPO_BASE =
            "https:/" + "/raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/";
    private static final Path CACHE_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("bedrock-chest-opening").resolve("boss_loot");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Map<String, String> FLOOR_TO_BOSS = Map.ofEntries(
            Map.entry("F1", "BONZO_BOSS"),
            Map.entry("F2", "SCARF_BOSS"),
            Map.entry("F3", "PROFESSOR_BOSS"),
            Map.entry("F4", "THORN_BOSS"),
            Map.entry("F5", "LIVID_BOSS"),
            Map.entry("F6", "SADAN_BOSS"),
            Map.entry("F7", "NECRON_BOSS"),
            Map.entry("M1", "BONZO_BOSS"),
            Map.entry("M2", "SCARF_BOSS"),
            Map.entry("M3", "PROFESSOR_BOSS"),
            Map.entry("M4", "THORN_BOSS"),
            Map.entry("M5", "LIVID_BOSS"),
            Map.entry("M6", "SADAN_BOSS"),
            Map.entry("M7", "NECRON_BOSS")
    );
    private static final Map<String, Map<String, List<JackpotItem>>> BOSS_LOOT_CACHE = new ConcurrentHashMap<>();

    public static void initialize() {
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(CACHE_DIR);
            } catch (Exception ignored) {
            }
            Set<String> processedBosses = new HashSet<>();
            for (Map.Entry<String, String> entry : FLOOR_TO_BOSS.entrySet()) {
                String floor = entry.getKey();
                String bossFile = entry.getValue();
                loadBossLoot(floor, bossFile, processedBosses);
            }
        });
    }

    private static void loadBossLoot(String floor, String bossFile, Set<String> processedBosses) {
        try {
            Path localCache = CACHE_DIR.resolve(bossFile + ".json");
            JsonObject json = null;
            if (Files.exists(localCache)) {
                try (var reader = Files.newBufferedReader(localCache, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
            if (json == null && processedBosses.add(bossFile)) {
                String url = NEU_REPO_BASE + bossFile + ".json";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    json = JsonParser.parseString(response.body()).getAsJsonObject();
                    Files.writeString(localCache, response.body(), StandardCharsets.UTF_8);
                    System.out.println("[BCCO] Downloaded & cached: " + bossFile + ".json");
                }
            }
            if (json != null) {
                boolean isMaster = floor.startsWith("M");
                Map<String, List<JackpotItem>> chestDrops = parseBossDrops(json, isMaster);
                if (!chestDrops.isEmpty()) {
                    BOSS_LOOT_CACHE.put(floor, chestDrops);
                    System.out.println("[BCCO] Loaded " + chestDrops.size() + " chest tables for " + floor);
                }
            }
        } catch (Exception e) {
            System.out.println("[BCCO] Failed to load boss loot for " + floor + ": " + e.getMessage());
        }
    }

    private static Map<String, List<JackpotItem>> parseBossDrops(JsonObject root, boolean isMasterMode) {
        Map<String, List<JackpotItem>> chestMap = new HashMap<>();
        if (!root.has("recipes")) return chestMap;
        JsonArray recipes = root.getAsJsonArray("recipes");
        for (JsonElement rElem : recipes) {
            if (!rElem.isJsonObject()) continue;
            JsonObject recipe = rElem.getAsJsonObject();
            if (!"drops".equals(recipe.get("type").getAsString()) || !recipe.has("drops")) continue;
            String recipeName = recipe.get("name").getAsString();
            boolean isMmRecipe = recipeName.contains("Master Mode");
            if (isMasterMode != isMmRecipe) continue;
            JsonArray drops = recipe.getAsJsonArray("drops");
            for (JsonElement dElem : drops) {
                if (!dElem.isJsonObject()) continue;
                JsonObject drop = dElem.getAsJsonObject();
                String id = drop.get("id").getAsString();
                double chance = parseChance(drop.get("chance").getAsString());
                String targetChest = "Bedrock";
                if (drop.has("extra")) {
                    for (JsonElement ex : drop.getAsJsonArray("extra")) {
                        String line = ex.getAsString();
                        if (line.contains("Bedrock Chest")) {
                            targetChest = "Bedrock";
                            break;
                        } else if (line.contains("Obsidian Chest")) {
                            targetChest = "Obsidian";
                            break;
                        }
                    }
                }
                if (chance < 1.0 || isHighTier(id)) {
                    chestMap.computeIfAbsent(targetChest, _ -> new ArrayList<>())
                            .add(new JackpotItem(id, chance, formatDisplayName(id)));
                }
            }
        }
        for (List<JackpotItem> list : chestMap.values()) {
            list.sort(Comparator.comparingDouble(JackpotItem::chance));
        }
        return chestMap;
    }

    private static double parseChance(String chanceStr) {
        try {
            return Double.parseDouble(chanceStr.replace("%", "").trim());
        } catch (Exception e) {
            return 100.0;
        }
    }

    private static boolean isHighTier(String id) {
        return id.contains("SWORD") || id.contains("HANDLE") || id.contains("SCROLL")
                || id.contains("CLAYMORE") || id.contains("CHESTPLATE") || id.contains("STAR")
                || id.contains("EYE");
    }

    public static ItemStack getFloorJackpot(String floor) {
        return getFloorJackpot(floor, "Bedrock");
    }

    public static ItemStack getFloorJackpot(String floor, String chestType) {
        String fKey = floor != null ? floor.toUpperCase(Locale.ROOT) : "F7";
        String cKey = (chestType != null && chestType.contains("Obsidian")) ? "Obsidian" : "Bedrock";
        Map<String, List<JackpotItem>> floorMap = BOSS_LOOT_CACHE.get(fKey);
        if (floorMap != null) {
            List<JackpotItem> items = floorMap.get(cKey);
            if (items != null && !items.isEmpty()) {
                int pick = Math.min(items.size() - 1, new Random().nextInt(Math.min(2, items.size())));
                return items.get(pick).createStack();
            }
        }
        return getFallbackJackpot(fKey);
    }

    private static Item inferMaterial(String id) {
        if (id.equals("NECRON_HANDLE")) return Items.STICK;
        if (id.endsWith("SCROLL")) return Items.WRITABLE_BOOK;
        if (id.equals("GIANTS_SWORD") || id.equals("LIVID_DAGGER") || id.equals("NECROMANCER_SWORD"))
            return Items.IRON_SWORD;
        if (id.equals("DARK_CLAYMORE")) return Items.STONE_SWORD;
        if (id.contains("SWORD") || id.contains("BLADE")) return Items.DIAMOND_SWORD;
        if (id.contains("BOW")) return Items.BOW;
        if (id.contains("STAFF")) return Items.BLAZE_ROD;
        if (id.contains("CHESTPLATE") || id.contains("CLOAK")) return Items.DIAMOND_CHESTPLATE;
        if (id.contains("HELMET") || id.contains("EYE") || id.contains("MASK")) return Items.DIAMOND_HELMET;
        if (id.contains("STAR") || id.contains("SKULL")) return Items.NETHER_STAR;
        return Items.NETHER_STAR;
    }

    public static List<ItemStack> getFloorDrops(String floor, String chestType) {
        String fKey = floor != null ? floor.toUpperCase(Locale.ROOT) : "F7";
        String cKey = (chestType != null && chestType.contains("Obsidian")) ? "Obsidian" : "Bedrock";
        Map<String, List<JackpotItem>> floorMap = BOSS_LOOT_CACHE.get(fKey);
        if (floorMap != null) {
            List<JackpotItem> items = floorMap.get(cKey);
            if (items != null && !items.isEmpty()) {
                List<ItemStack> stacks = new ArrayList<>();
                for (JackpotItem ji : items) {
                    stacks.add(ji.createStack());
                }
                return stacks;
            }
        }
        return List.of(
                DynamicItemBuilder.build("NECRON_HANDLE"),
                DynamicItemBuilder.build("IMPLOSION_SCROLL"),
                DynamicItemBuilder.build("WITHER_CHESTPLATE"),
                DynamicItemBuilder.build("FUMING_POTATO_BOOK")
        );
    }

    public static ItemStack getRandomFloorJackpot(String floor, String chestType) {
        List<ItemStack> drops = getFloorDrops(floor, chestType);
        if (drops.isEmpty()) {
            return DynamicItemBuilder.build("NECRON_HANDLE");
        }
        return drops.get(new Random().nextInt(drops.size()));
    }

    private static String formatDisplayName(String id) {
        StringBuilder sb = new StringBuilder("§6");
        for (String part : id.split("_")) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static ItemStack getFallbackJackpot(String floor) {
        return switch (floor) {
            case "F1", "M1" -> new JackpotItem("BONZO_STAFF", 0.2, "§9Bonzo's Staff").createStack();
            case "F5", "M5" -> new JackpotItem("SHADOW_FURY", 0.2, "§6Shadow Fury").createStack();
            case "F6", "M6" -> new JackpotItem("GIANTS_SWORD", 0.187, "§6Giant's Sword").createStack();
            case "M7" -> new JackpotItem("DARK_CLAYMORE", 0.1, "§6Dark Claymore").createStack();
            default -> new JackpotItem("NECRON_HANDLE", 0.1, "§c§lNecron's Handle").createStack();
        };
    }

    public record JackpotItem(String id, double chance, String name) {
        public ItemStack createStack() {
            return DynamicItemBuilder.build(id);
        }
    }
}