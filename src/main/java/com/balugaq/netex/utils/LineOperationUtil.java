package com.balugaq.netex.utils;

import com.balugaq.netex.api.data.VanillaInventoryWrapper;
import com.balugaq.netex.api.enums.TransportMode;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import lombok.experimental.UtilityClass;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("DuplicatedCode")
@UtilityClass
public class LineOperationUtil {
    public static final Location UNKNOWN_LOCATION = new Location(null, 0, 0, 0);

    public static void doOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        @NotNull Consumer<BlockMenu> consumer) {
        doOperation(startLocation, direction, limit, false, true, consumer);
    }

    public static void doOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean skipNoMenu,
        @NotNull Consumer<BlockMenu> consumer) {
        doOperation(startLocation, direction, limit, skipNoMenu, true, consumer);
    }

    public static void doOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean skipNoMenu,
        boolean optimizeExperience,
        @NotNull Consumer<BlockMenu> consumer) {
        doOperationAsync(startLocation, direction, limit, skipNoMenu, optimizeExperience, blockMenu -> {
            consumer.accept(blockMenu);
            return CompletableFuture.completedFuture(null);
        }).join();
    }

    public static @NotNull CompletableFuture<Void> doOperationAsync(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean skipNoMenu,
        boolean optimizeExperience,
        @NotNull Function<BlockMenu, CompletableFuture<Void>> consumer) {
        int finalLimit = optimizeExperience ? limit + 1 : limit;
        return iterateMenusAsync(startLocation.clone(), direction, finalLimit, skipNoMenu, consumer);
    }

    public static void doVanillaOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        @NotNull Consumer<BlockMenu> consumer) {
        doVanillaOperation(startLocation, direction, limit, false, false, consumer);
    }

    public static void doVanillaOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean skipNoInventory,
        @NotNull Consumer<BlockMenu> consumer) {
        doVanillaOperation(startLocation, direction, limit, skipNoInventory, false, consumer);
    }

    public static void doVanillaOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean skipNoInventory,
        boolean optimizeExperience,
        @NotNull Consumer<BlockMenu> consumer) {
        doVanillaOperationAsync(startLocation, direction, limit, skipNoInventory, optimizeExperience, blockMenu -> {
            consumer.accept(blockMenu);
            return CompletableFuture.completedFuture(null);
        }).join();
    }

    public static @NotNull CompletableFuture<Void> doVanillaOperationAsync(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean skipNoInventory,
        boolean optimizeExperience,
        @NotNull Function<BlockMenu, CompletableFuture<Void>> consumer) {
        int finalLimit = optimizeExperience ? limit + 1 : limit;
        return iterateVanillaMenusAsync(startLocation.clone(), direction, finalLimit, skipNoInventory, consumer);
    }

    public static void doEnergyOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        @NotNull Consumer<Location> consumer) {
        doEnergyOperation(startLocation, direction, limit, true, true, consumer);
    }

    public static void doEnergyOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean allowNoMenu,
        @NotNull Consumer<Location> consumer) {
        doEnergyOperation(startLocation, direction, limit, allowNoMenu, true, consumer);
    }

    public static void doEnergyOperation(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean allowNoMenu,
        boolean optimizeExperience,
        @NotNull Consumer<Location> consumer) {
        doEnergyOperationAsync(startLocation, direction, limit, allowNoMenu, optimizeExperience, location -> {
            consumer.accept(location);
            return CompletableFuture.completedFuture(null);
        }).join();
    }

    public static @NotNull CompletableFuture<Void> doEnergyOperationAsync(
        @NotNull Location startLocation,
        @NotNull BlockFace direction,
        int limit,
        boolean allowNoMenu,
        boolean optimizeExperience,
        @NotNull Function<Location, CompletableFuture<Void>> consumer) {
        int finalLimit = optimizeExperience ? limit + 1 : limit;
        return iterateEnergyAsync(startLocation.clone(), direction, finalLimit, allowNoMenu, consumer);
    }

    @Deprecated
    public static void grabItem(
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        grabItem(UNKNOWN_LOCATION, root, blockMenu, transportMode, limitQuantity);
    }

    /**
     * @param accessor      the target menu's location
     * @param root          the root
     * @param blockMenu     the target menu
     * @param transportMode the transport mode
     * @param limitQuantity the max amount to transport
     */
    public static void grabItem(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        grabItemAsync(accessor, root, blockMenu, transportMode, limitQuantity);
    }

    @Deprecated
    public static @NotNull CompletableFuture<Void> grabItemAsync(
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        return grabItemAsync(UNKNOWN_LOCATION, root, blockMenu, transportMode, limitQuantity);
    }

    public static @NotNull CompletableFuture<Void> grabItemAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        final int[] slots =
            blockMenu.getPreset().getSlotsAccessedByItemTransport(blockMenu, ItemTransportFlow.WITHDRAW, null);

        return switch (transportMode) {
            case NONE, NONNULL_ONLY -> grabSlotsAsync(accessor, root, blockMenu, slots, 0, limitQuantity, false);
            case NULL_ONLY -> CompletableFuture.completedFuture(null);
            case FIRST_ONLY -> slots.length == 0
                ? CompletableFuture.completedFuture(null)
                : grabSingleSlotAsync(accessor, root, blockMenu, slots[0], limitQuantity, false);
            case LAST_ONLY -> slots.length == 0
                ? CompletableFuture.completedFuture(null)
                : grabSingleSlotAsync(accessor, root, blockMenu, slots[slots.length - 1], limitQuantity, false);
            case FIRST_STOP -> grabFirstNonNullSlotAsync(accessor, root, blockMenu, slots, 0, limitQuantity, false);
            case LAZY -> {
                if (slots.length == 0) {
                    yield CompletableFuture.completedFuture(null);
                }

                final ItemStack delta = blockMenu.getItemInSlot(slots[0]);
                if (delta == null || delta.getType() == Material.AIR) {
                    yield CompletableFuture.completedFuture(null);
                }

                yield grabSlotsAsync(accessor, root, blockMenu, slots, 0, limitQuantity, false);
            }
            case VOID -> grabSlotsAsync(accessor, root, blockMenu, slots, 0, limitQuantity, true);
            case SPECIFIED_QUANTITY -> grabSpecifiedQuantityAsync(accessor, root, blockMenu, slots, limitQuantity);
        };
    }

    @Deprecated
    public static void pushItem(
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull List<ItemStack> clones,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        pushItem(UNKNOWN_LOCATION, root, blockMenu, clones, transportMode, limitQuantity);
    }

    /**
     * @param accessor      the target menu's location
     * @param root          the root
     * @param blockMenu     the target menu
     * @param transportMode the transport mode
     * @param limitQuantity the max amount to transport
     */
    public static void pushItem(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull List<ItemStack> clones,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        for (ItemStack clone : clones) {
            pushItem(accessor, root, blockMenu, clone, transportMode, limitQuantity);
        }
    }

    @Deprecated
    public static @NotNull CompletableFuture<Void> pushItemAsync(
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull List<ItemStack> clones,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        return pushItemAsync(UNKNOWN_LOCATION, root, blockMenu, clones, transportMode, limitQuantity);
    }

    public static @NotNull CompletableFuture<Void> pushItemAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull List<ItemStack> clones,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (ItemStack clone : clones) {
            if (clone == null || clone.getType() == Material.AIR) {
                continue;
            }

            final ItemStack sample = StackUtils.getAsQuantity(clone, 1);
            chain = chain.thenCompose(ignored -> pushItemAsync(accessor, root, blockMenu, sample, transportMode, limitQuantity));
        }
        return chain;
    }

    @Deprecated
    public static void pushItem(
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull ItemStack clone,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        pushItem(UNKNOWN_LOCATION, root, blockMenu, clone, transportMode, limitQuantity);
    }

    public static void pushItem(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull ItemStack clone,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        pushItemAsync(accessor, root, blockMenu, clone, transportMode, limitQuantity);
    }

    @Deprecated
    public static @NotNull CompletableFuture<Void> pushItemAsync(
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull ItemStack clone,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        return pushItemAsync(UNKNOWN_LOCATION, root, blockMenu, clone, transportMode, limitQuantity);
    }

    public static @NotNull CompletableFuture<Void> pushItemAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull ItemStack clone,
        @NotNull TransportMode transportMode,
        int limitQuantity) {
        final int[] slots =
            blockMenu.getPreset().getSlotsAccessedByItemTransport(blockMenu, ItemTransportFlow.INSERT, clone);
        if (slots.length == 0 || limitQuantity <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        final ItemStack sample = StackUtils.getAsQuantity(clone, 1);
        return switch (transportMode) {
            case NONE -> {
                int freeSpace = 0;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        freeSpace += sample.getMaxStackSize();
                    } else if (itemStack.getAmount() < sample.getMaxStackSize() && StackUtils.itemsMatch(sample, itemStack)) {
                        freeSpace += itemStack.getMaxStackSize() - itemStack.getAmount();
                    }
                }

                if (freeSpace <= 0) {
                    yield CompletableFuture.completedFuture(null);
                }

                yield requestAndPushAsync(
                    accessor,
                    root,
                    blockMenu,
                    sample,
                    Math.min(freeSpace, limitQuantity),
                    true,
                    slots
                ).thenAccept(ignored -> {
                });
            }
            case NULL_ONLY -> pushAcrossSlotsAsync(
                accessor,
                root,
                blockMenu,
                sample,
                slots,
                0,
                limitQuantity,
                (itemStack, template) -> itemStack == null || itemStack.getType() == Material.AIR ? template.getMaxStackSize() : 0
            );
            case NONNULL_ONLY -> pushAcrossSlotsAsync(
                accessor,
                root,
                blockMenu,
                sample,
                slots,
                0,
                limitQuantity,
                (itemStack, template) -> {
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        return 0;
                    }

                    if (itemStack.getAmount() >= template.getMaxStackSize() || !StackUtils.itemsMatch(template, itemStack)) {
                        return 0;
                    }

                    return itemStack.getMaxStackSize() - itemStack.getAmount();
                }
            );
            case FIRST_ONLY -> pushSingleSlotAsync(accessor, root, blockMenu, sample, slots[0], limitQuantity);
            case LAST_ONLY -> pushSingleSlotAsync(accessor, root, blockMenu, sample, slots[slots.length - 1], limitQuantity);
            case FIRST_STOP -> {
                int freeSpace = getSingleSlotRequestAmount(blockMenu.getItemInSlot(slots[0]), sample);
                if (freeSpace <= 0) {
                    yield CompletableFuture.completedFuture(null);
                }

                yield requestAndPushAsync(
                    accessor,
                    root,
                    blockMenu,
                    sample,
                    Math.min(freeSpace, limitQuantity),
                    true,
                    slots
                ).thenAccept(ignored -> {
                });
            }
            case LAZY -> {
                final ItemStack delta = blockMenu.getItemInSlot(slots[0]);
                if (delta != null && delta.getType() != Material.AIR) {
                    yield CompletableFuture.completedFuture(null);
                }

                int freeSpace = 0;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        freeSpace += sample.getMaxStackSize();
                    } else if (itemStack.getAmount() < sample.getMaxStackSize() && StackUtils.itemsMatch(sample, itemStack)) {
                        freeSpace += itemStack.getMaxStackSize() - itemStack.getAmount();
                    }
                }

                if (freeSpace <= 0) {
                    yield CompletableFuture.completedFuture(null);
                }

                yield requestAndPushAsync(
                    accessor,
                    root,
                    blockMenu,
                    sample,
                    Math.min(freeSpace, limitQuantity),
                    true,
                    slots
                ).thenAccept(ignored -> {
                });
            }
            case VOID -> requestAndPushAsync(accessor, root, blockMenu, sample, limitQuantity, false, slots).thenAccept(ignored -> {
            });
            case SPECIFIED_QUANTITY -> {
                int existingCount = 0;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack != null && itemStack.getType() != Material.AIR && StackUtils.itemsMatch(sample, itemStack)) {
                        existingCount += itemStack.getAmount();
                    }
                }

                if (existingCount >= limitQuantity) {
                    yield CompletableFuture.completedFuture(null);
                }

                final int deficit = limitQuantity - existingCount;
                int availableSpace = 0;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        availableSpace += sample.getMaxStackSize();
                    } else if (StackUtils.itemsMatch(sample, itemStack)) {
                        availableSpace += Math.max(0, itemStack.getMaxStackSize() - itemStack.getAmount());
                    }
                }

                if (availableSpace <= 0) {
                    yield CompletableFuture.completedFuture(null);
                }

                yield requestAndPushAsync(
                    accessor,
                    root,
                    blockMenu,
                    sample,
                    Math.min(deficit, availableSpace),
                    true,
                    slots
                ).thenAccept(ignored -> {
                });
            }
        };
    }

    private static @NotNull CompletableFuture<Void> pushAcrossSlotsAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull ItemStack sample,
        int @NotNull [] slots,
        int index,
        int remaining,
        @NotNull SlotRequestStrategy strategy) {
        if (remaining <= 0 || index >= slots.length) {
            return CompletableFuture.completedFuture(null);
        }

        final int slot = slots[index];
        final ItemStack existing = blockMenu.getItemInSlot(slot);
        final int requestAmount = Math.min(remaining, Math.max(0, strategy.getRequestAmount(existing, sample)));
        if (requestAmount <= 0) {
            return pushAcrossSlotsAsync(accessor, root, blockMenu, sample, slots, index + 1, remaining, strategy);
        }

        return requestAndPushAsync(accessor, root, blockMenu, sample, requestAmount, true, slot)
            .thenCompose(moved -> pushAcrossSlotsAsync(
                accessor,
                root,
                blockMenu,
                sample,
                slots,
                index + 1,
                Math.max(0, remaining - moved),
                strategy
            ));
    }

    private static @NotNull CompletableFuture<Void> pushSingleSlotAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull ItemStack sample,
        int slot,
        int limitQuantity) {
        final int requestAmount = Math.min(limitQuantity, getSingleSlotRequestAmount(blockMenu.getItemInSlot(slot), sample));
        if (requestAmount <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        return requestAndPushAsync(accessor, root, blockMenu, sample, requestAmount, true, slot).thenAccept(ignored -> {
        });
    }

    private static int getSingleSlotRequestAmount(@Nullable ItemStack itemStack, @NotNull ItemStack sample) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return sample.getMaxStackSize();
        }

        if (itemStack.getAmount() >= sample.getMaxStackSize() || !StackUtils.itemsMatch(sample, itemStack)) {
            return 0;
        }

        return itemStack.getMaxStackSize() - itemStack.getAmount();
    }

    private static @NotNull CompletableFuture<Void> grabSlotsAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        int @NotNull [] slots,
        int index,
        int limit,
        boolean clearSlot) {
        if (limit <= 0 || index >= slots.length) {
            return CompletableFuture.completedFuture(null);
        }

        final int slot = slots[index];
        final ItemStack item = blockMenu.getItemInSlot(slot);
        if (item == null || item.getType() == Material.AIR) {
            return grabSlotsAsync(accessor, root, blockMenu, slots, index + 1, limit, clearSlot);
        }

        final int expectedReceive = Math.min(item.getAmount(), limit);
        return depositSlotAsync(accessor, root, blockMenu, slot, expectedReceive, clearSlot)
            .thenCompose(moved -> grabSlotsAsync(accessor, root, blockMenu, slots, index + 1, limit - moved, clearSlot));
    }

    private static @NotNull CompletableFuture<Void> grabSingleSlotAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        int slot,
        int limit,
        boolean clearSlot) {
        final ItemStack item = blockMenu.getItemInSlot(slot);
        if (item == null || item.getType() == Material.AIR) {
            return CompletableFuture.completedFuture(null);
        }

        final int expectedReceive = Math.min(item.getAmount(), limit);
        if (expectedReceive <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        return depositSlotAsync(accessor, root, blockMenu, slot, expectedReceive, clearSlot).thenAccept(ignored -> {
        });
    }

    private static @NotNull CompletableFuture<Void> grabFirstNonNullSlotAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        int @NotNull [] slots,
        int index,
        int limit,
        boolean clearSlot) {
        if (limit <= 0 || index >= slots.length) {
            return CompletableFuture.completedFuture(null);
        }

        final int slot = slots[index];
        final ItemStack item = blockMenu.getItemInSlot(slot);
        if (item == null || item.getType() == Material.AIR) {
            return grabFirstNonNullSlotAsync(accessor, root, blockMenu, slots, index + 1, limit, clearSlot);
        }

        final int expectedReceive = Math.min(item.getAmount(), limit);
        return depositSlotAsync(accessor, root, blockMenu, slot, expectedReceive, clearSlot).thenAccept(ignored -> {
        });
    }

    private static @NotNull CompletableFuture<Void> grabSpecifiedQuantityAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        int @NotNull [] slots,
        int limitQuantity) {
        java.util.Map<Integer, ItemStack> itemSamples = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Integer> itemTotals = new java.util.LinkedHashMap<>();
        int typeIndex = 0;
        for (int slot : slots) {
            final ItemStack item = blockMenu.getItemInSlot(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            boolean found = false;
            for (var entry : itemSamples.entrySet()) {
                if (StackUtils.itemsMatch(entry.getValue(), item)) {
                    itemTotals.merge(entry.getKey(), item.getAmount(), Integer::sum);
                    found = true;
                    break;
                }
            }

            if (!found) {
                itemSamples.put(typeIndex, StackUtils.getAsQuantity(item, 1));
                itemTotals.put(typeIndex, item.getAmount());
                typeIndex++;
            }
        }

        return grabSpecifiedQuantityAsync(
            accessor,
            root,
            blockMenu,
            slots,
            new java.util.ArrayList<>(itemSamples.entrySet()),
            itemTotals,
            limitQuantity,
            0
        );
    }

    private static @NotNull CompletableFuture<Void> grabSpecifiedQuantityAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        int @NotNull [] slots,
        @NotNull List<java.util.Map.Entry<Integer, ItemStack>> itemEntries,
        @NotNull java.util.Map<Integer, Integer> itemTotals,
        int limitQuantity,
        int entryIndex) {
        if (entryIndex >= itemEntries.size()) {
            return CompletableFuture.completedFuture(null);
        }

        final java.util.Map.Entry<Integer, ItemStack> entry = itemEntries.get(entryIndex);
        final int total = itemTotals.getOrDefault(entry.getKey(), 0);
        if (total <= limitQuantity) {
            return grabSpecifiedQuantityAsync(accessor, root, blockMenu, slots, itemEntries, itemTotals, limitQuantity, entryIndex + 1);
        }

        final int toRemove = total - limitQuantity;
        return grabMatchingSlotsAsync(accessor, root, blockMenu, slots, entry.getValue(), slots.length - 1, toRemove)
            .thenCompose(ignored -> grabSpecifiedQuantityAsync(
                accessor,
                root,
                blockMenu,
                slots,
                itemEntries,
                itemTotals,
                limitQuantity,
                entryIndex + 1
            ));
    }

    private static @NotNull CompletableFuture<Void> grabMatchingSlotsAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        int @NotNull [] slots,
        @NotNull ItemStack sample,
        int index,
        int remaining) {
        if (remaining <= 0 || index < 0) {
            return CompletableFuture.completedFuture(null);
        }

        final int slot = slots[index];
        final ItemStack item = blockMenu.getItemInSlot(slot);
        if (item == null || item.getType() == Material.AIR || !StackUtils.itemsMatch(sample, item)) {
            return grabMatchingSlotsAsync(accessor, root, blockMenu, slots, sample, index - 1, remaining);
        }

        final int grabFromSlot = Math.min(item.getAmount(), remaining);
        return depositSlotAsync(accessor, root, blockMenu, slot, grabFromSlot, false)
            .thenCompose(moved -> grabMatchingSlotsAsync(accessor, root, blockMenu, slots, sample, index - 1, remaining - moved));
    }

    private static @NotNull CompletableFuture<Integer> depositSlotAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        int slot,
        int amount,
        boolean clearSlot) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(0);
        }

        final ItemStack item = blockMenu.getItemInSlot(slot);
        if (item == null || item.getType() == Material.AIR) {
            return CompletableFuture.completedFuture(0);
        }

        final ItemStack transfer = StackUtils.getAsQuantity(item, amount);
        final int originalAmount = transfer.getAmount();
        final Location targetLocation = blockMenu.getLocation() == null ? accessor : blockMenu.getLocation();
        return root.addItemStack0Async(accessor, transfer)
            .thenCompose(ignored -> FoliaSupport.supplyRegion(targetLocation, () -> {
                final int moved = Math.max(0, originalAmount - transfer.getAmount());
                final ItemStack live = blockMenu.getItemInSlot(slot);
                if (live != null && live.getType() != Material.AIR) {
                    if (clearSlot) {
                        live.setAmount(0);
                    } else if (moved > 0) {
                        live.setAmount(Math.max(0, live.getAmount() - moved));
                    }
                }
                return moved;
            }));
    }

    private static @NotNull CompletableFuture<Integer> requestAndPushAsync(
        @NotNull Location accessor,
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull ItemStack sample,
        int amount,
        boolean returnRemainder,
        int @NotNull ... slots) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(0);
        }

        final ItemRequest request = new ItemRequest(sample, amount);
        final Location targetLocation = blockMenu.getLocation() == null ? accessor : blockMenu.getLocation();
        return root.getItemStack0Async(accessor, request).thenCompose(retrieved -> {
            if (retrieved == null || retrieved.getType() == Material.AIR || retrieved.getAmount() <= 0) {
                return CompletableFuture.completedFuture(0);
            }

            final int requestedAmount = retrieved.getAmount();
            return FoliaSupport.supplyRegion(targetLocation, () -> BlockMenuUtil.pushItem(blockMenu, retrieved, slots))
                .thenCompose(leftOver -> {
                    final int leftAmount = leftOver == null || leftOver.getType() == Material.AIR ? 0 : leftOver.getAmount();
                    final int moved = Math.max(0, requestedAmount - leftAmount);
                    if (returnRemainder && leftAmount > 0) {
                        return root.addItemStack0Async(accessor, leftOver).thenApply(ignored -> moved);
                    }

                    return CompletableFuture.completedFuture(moved);
                });
        });
    }

    @FunctionalInterface
    private interface SlotRequestStrategy {
        int getRequestAmount(@Nullable ItemStack existing, @NotNull ItemStack sample);
    }

    public static void outPower(@NotNull Location location, @NotNull NetworkRoot root, int rate) {
        outPowerAsync(location, root, rate).join();
    }

    public static @NotNull CompletableFuture<Void> outPowerAsync(
        @NotNull Location location,
        @NotNull NetworkRoot root,
        int rate) {
        return FoliaSupport.supplyRegion(location, () -> {
            final SlimefunBlockData blockData = StorageCacheUtils.getBlock(location);
            if (blockData == null) {
                return 0;
            }

            if (!blockData.isDataLoaded()) {
                StorageCacheUtils.requestLoad(blockData);
                return 0;
            }

            final SlimefunItem slimefunItem = SlimefunItem.getById(blockData.getSfId());
            if (!(slimefunItem instanceof EnergyNetComponent component) || slimefunItem instanceof NetworkObject) {
                return 0;
            }

            final int existingCharge = component.getCharge(location);
            final int capacity = component.getCapacity();
            final int space = capacity - existingCharge;

            if (space <= 0) {
                return 0;
            }

            final int possibleGeneration = Math.min(rate, space);
            return possibleGeneration;
        }).thenCompose(possibleGeneration -> {
            if (possibleGeneration <= 0) {
                return CompletableFuture.completedFuture(null);
            }

            return root.removeRootPowerUpToAsync(possibleGeneration).thenCompose(reserved -> {
                if (reserved <= 0) {
                    return CompletableFuture.completedFuture(null);
                }

                return FoliaSupport.supplyRegion(location, () -> {
                    final SlimefunBlockData blockData = StorageCacheUtils.getBlock(location);
                    if (blockData == null || !blockData.isDataLoaded()) {
                        return 0;
                    }

                    final SlimefunItem slimefunItem = SlimefunItem.getById(blockData.getSfId());
                    if (!(slimefunItem instanceof EnergyNetComponent component) || slimefunItem instanceof NetworkObject) {
                        return 0;
                    }

                    final int existingCharge = component.getCharge(location);
                    final int capacity = component.getCapacity();
                    final int space = Math.max(0, capacity - existingCharge);
                    final int generated = Math.min(reserved, space);
                    if (generated > 0) {
                        component.addCharge(location, generated);
                    }
                    return generated;
                }).thenCompose(generated -> {
                    final int refund = reserved - generated;
                    if (refund <= 0) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return root.restoreRootPowerAsync(refund).thenAccept(ignored -> {
                    });
                });
            });
        });
    }

    private static @NotNull CompletableFuture<Void> iterateMenusAsync(
        @NotNull Location current,
        @NotNull BlockFace direction,
        int remaining,
        boolean skipNoMenu,
        @NotNull Function<BlockMenu, CompletableFuture<Void>> consumer) {
        if (remaining <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        final Location next = step(current, direction);
        return FoliaSupport.supplyRegion(next, () -> StorageCacheUtils.getMenu(next))
            .thenCompose(blockMenu -> {
                if (blockMenu == null) {
                    if (skipNoMenu) {
                        return iterateMenusAsync(next, direction, remaining - 1, true, consumer);
                    }
                    return CompletableFuture.completedFuture(null);
                }

                return consumer.apply(blockMenu)
                    .thenCompose(ignored -> iterateMenusAsync(next, direction, remaining - 1, skipNoMenu, consumer));
            });
    }

    private static @NotNull CompletableFuture<Void> iterateVanillaMenusAsync(
        @NotNull Location current,
        @NotNull BlockFace direction,
        int remaining,
        boolean skipNoInventory,
        @NotNull Function<BlockMenu, CompletableFuture<Void>> consumer) {
        if (remaining <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        final Location next = step(current, direction);
        return FoliaSupport.supplyRegion(next, () -> {
            BlockState state = next.getBlock().getState(false);
            if (state instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                if (inv != null) {
                    return new VanillaInventoryWrapper(inv, state);
                }
            }
            return null;
        }).thenCompose(wrapper -> {
            if (wrapper == null) {
                if (skipNoInventory) {
                    return iterateVanillaMenusAsync(next, direction, remaining - 1, true, consumer);
                }
                return CompletableFuture.completedFuture(null);
            }

            return consumer.apply(wrapper)
                .thenCompose(ignored -> iterateVanillaMenusAsync(next, direction, remaining - 1, skipNoInventory, consumer));
        });
    }

    private static @NotNull CompletableFuture<Void> iterateEnergyAsync(
        @NotNull Location current,
        @NotNull BlockFace direction,
        int remaining,
        boolean allowNoMenu,
        @NotNull Function<Location, CompletableFuture<Void>> consumer) {
        if (remaining <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        final Location next = step(current, direction);
        return FoliaSupport.supplyRegion(next, () -> StorageCacheUtils.getMenu(next) != null)
            .thenCompose(hasMenu -> {
                if (!hasMenu && !allowNoMenu) {
                    return CompletableFuture.completedFuture(null);
                }

                return consumer.apply(next)
                    .thenCompose(ignored -> iterateEnergyAsync(next, direction, remaining - 1, allowNoMenu, consumer));
            });
    }

    private static @NotNull Location step(@NotNull Location location, @NotNull BlockFace direction) {
        Location next = location.clone();
        switch (direction) {
            case NORTH -> next.setZ(next.getZ() - 1);
            case SOUTH -> next.setZ(next.getZ() + 1);
            case EAST -> next.setX(next.getX() + 1);
            case WEST -> next.setX(next.getX() - 1);
            case UP -> next.setY(next.getY() + 1);
            case DOWN -> next.setY(next.getY() - 1);
            default -> {
            }
        }
        return next;
    }
}
