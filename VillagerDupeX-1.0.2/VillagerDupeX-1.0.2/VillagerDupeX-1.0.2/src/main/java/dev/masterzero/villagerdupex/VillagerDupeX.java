package dev.masterzero.villagerdupex;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public final class VillagerDupeX implements ClientModInitializer {
    public static final String MOD_ID = "villagerdupex";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static final String[] FILTERS = {"all", "saddle", "carpet", "leather horse armor"};
    private static final long[] INTERVALS_MS = {50, 100, 250, 500};
    private static final int DEFAULT_INTERVAL_INDEX = 1;

    private KeyMapping autoKey;
    private KeyMapping singleKey;
    private KeyMapping filterKey;
    private KeyMapping intervalKey;

    private boolean armed;
    private boolean repeating;
    private int filterIndex;
    private int intervalIndex = DEFAULT_INTERVAL_INDEX;
    private long lastBatchNanos;
    private long batchesRun;
    private long interactionsSent;
    private long eligibleVillagersSeen;

    @Override
    public void onInitializeClient() {
        autoKey = registerKey("auto", InputConstants.KEY_G);
        singleKey = registerKey("single", InputConstants.KEY_H);
        filterKey = registerKey("filter", InputConstants.KEY_J);
        intervalKey = registerKey("interval", InputConstants.KEY_K);

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        LOGGER.info("VillagerDupe X initialized for Minecraft 26.2.");
    }

    private KeyMapping registerKey(String path, int keyCode) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.villagerdupex." + path,
                InputConstants.Type.KEYSYM,
                keyCode,
                CATEGORY
        ));
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.getConnection() == null) {
            stopAll();
            return;
        }

        // Do not toggle modes from key presses while a menu/chat screen is open.
        if (client.gui.screen() != null) {
            return;
        }

        while (autoKey.consumeClick()) {
            if (repeating) {
                repeating = false;
                armed = false;
                report(client, "Repeating OFF | batches=" + batchesRun
                        + " | interaction attempts=" + interactionsSent
                        + " | eligible candidates=" + eligibleVillagersSeen);
            } else if (requirementsMet(client, true)) {
                repeating = true;
                armed = false;
                lastBatchNanos = 0L;
                batchesRun = 0L;
                interactionsSent = 0L;
                eligibleVillagersSeen = 0L;
                report(client, "Repeating ON | filter=" + FILTERS[filterIndex]
                        + " | interval=" + INTERVALS_MS[intervalIndex] + "ms");
            }
        }

        while (singleKey.consumeClick()) {
            repeating = false;
            armed = false;
            if (requirementsMet(client, true)) {
                armed = true;
                lastBatchNanos = 0L;
                report(client, "One batch armed.");
            }
        }

        while (filterKey.consumeClick()) {
            filterIndex = (filterIndex + 1) % FILTERS.length;
            report(client, "Filter: " + FILTERS[filterIndex]);
        }

        while (intervalKey.consumeClick()) {
            intervalIndex = (intervalIndex + 1) % INTERVALS_MS.length;
            report(client, "Batch interval: " + INTERVALS_MS[intervalIndex] + "ms");
        }

        if (!armed && !repeating) {
            return;
        }

        // Re-check prerequisites before every batch. Stop instead of silently using a
        // different main-hand item or replacing anything in the inventory.
        if (!requirementsMet(client, false)) {
            stopAll();
            report(client, "Stopped: keep an emerald selected in your main hand and shears in the hotbar.");
            return;
        }

        long now = System.nanoTime();
        long intervalNanos = INTERVALS_MS[intervalIndex] * 1_000_000L;
        if (lastBatchNanos != 0L && now - lastBatchNanos < intervalNanos) {
            return;
        }

        lastBatchNanos = now;
        armed = false;
        runBatch(client);
    }

    private boolean requirementsMet(Minecraft client, boolean explain) {
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }

        if (player.getMainHandItem().getItem() != Items.EMERALD) {
            if (explain) {
                report(client, "Select an emerald in your main hand before enabling the mod.");
            }
            return false;
        }

        if (findHotbarSlot(player, Items.SHEARS) < 0) {
            if (explain) {
                report(client, "Put shears in a hotbar slot before enabling the mod.");
            }
            return false;
        }

        return true;
    }

    /** Find every eligible, interactable villager currently tracked by the client. */
    private List<Villager> findTargets(Minecraft client, LocalPlayer player) {
        double range = player.entityInteractionRange();
        List<Villager> targets = new ArrayList<>();

        for (Entity entity : client.level.getEntities(
                (Entity) null, player.getBoundingBox().inflate(range + 1.0), candidate -> candidate instanceof Villager)) {
            if (!(entity instanceof Villager villager) || !villager.isAlive()) {
                continue;
            }
            if (!player.isWithinEntityInteractionRange(villager, 0.0)) {
                continue;
            }
            if (!matchesFilter(villager.getMainHandItem())) {
                continue;
            }
            // Extra conservative distance guard; the player's interaction-range method
            // remains the authoritative check for the client-side target list.
            if (player.distanceToSqr(villager) > (range + 1.0) * (range + 1.0)) {
                continue;
            }
            targets.add(villager);
        }

        targets.sort(Comparator.comparingDouble(villager -> player.distanceToSqr(villager)));
        return targets;
    }

    /**
     * Run one fast batch: select shears, interact with all currently eligible villagers,
     * and explicitly synchronize the selected emerald slot again afterwards.
     */
    private void runBatch(Minecraft client) {
        LocalPlayer player = client.player;
        int emeraldSlot = player.getInventory().getSelectedSlot();
        int shearsSlot = findHotbarSlot(player, Items.SHEARS);
        if (shearsSlot < 0 || emeraldSlot == shearsSlot
                || player.getInventory().getItem(emeraldSlot).getItem() != Items.EMERALD) {
            stopAll();
            report(client, "Stopped: emerald main hand or hotbar shears are no longer available.");
            return;
        }

        List<Villager> targets = findTargets(client, player);
        if (targets.isEmpty()) {
            return;
        }

        int sentThisBatch = 0;
        try {
            player.getInventory().setSelectedSlot(shearsSlot);
            // Make the slot transition explicit before the first interaction. This also
            // preserves the intended packet order when the emerald slot is restored below.
            client.getConnection().send(new ServerboundSetCarriedItemPacket(shearsSlot));

            for (Villager villager : targets) {
                // Revalidate immediately before interacting in case the target changed
                // while this batch was being assembled.
                if (!villager.isAlive()
                        || !player.isWithinEntityInteractionRange(villager, 0.0)
                        || !matchesFilter(villager.getMainHandItem())) {
                    continue;
                }

                EntityHitResult hit = new EntityHitResult(villager, closestPointOnBox(player.getEyePosition(), villager.getBoundingBox()));
                client.gameMode.interact(player, villager, hit, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
                sentThisBatch++;
            }
        } finally {
            // Always restore the user's emerald selection, even if an interaction throws.
            player.getInventory().setSelectedSlot(emeraldSlot);
            client.getConnection().send(new ServerboundSetCarriedItemPacket(emeraldSlot));
        }

        batchesRun++;
        interactionsSent += sentThisBatch;
        eligibleVillagersSeen += targets.size();

        if (sentThisBatch > 0) {
            report(client, "Batch " + batchesRun + ": sent " + sentThisBatch
                    + " interaction(s); emerald selected again.");
        }
    }

    private Vec3 closestPointOnBox(Vec3 point, AABB box) {
        double x = Math.max(box.minX, Math.min(point.x, box.maxX));
        double y = Math.max(box.minY, Math.min(point.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return new Vec3(x, y, z);
    }

    private int findHotbarSlot(LocalPlayer player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).getItem() == item) {
                return slot;
            }
        }
        return -1;
    }

    private boolean matchesFilter(ItemStack stack) {
        Item item = stack.getItem();
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        boolean saddle = item == Items.SADDLE;
        boolean carpet = item.builtInRegistryHolder().is(ItemTags.WOOL_CARPETS);
        boolean leatherHorseArmor = path.equals("leather_horse_armor");

        return switch (FILTERS[filterIndex]) {
            case "saddle" -> saddle;
            case "carpet" -> carpet;
            case "leather horse armor" -> leatherHorseArmor;
            default -> saddle || carpet || leatherHorseArmor;
        };
    }

    private void report(Minecraft client, String message) {
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal("[VillagerDupe X] " + message));
        }
    }

    private void stopAll() {
        armed = false;
        repeating = false;
        lastBatchNanos = 0L;
    }
}
