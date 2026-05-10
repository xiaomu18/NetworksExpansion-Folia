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
        requireRegionOwnership(startLocation);
        Location location = startLocation.clone();
        int finalLimit = limit;
        if (optimizeExperience) {
            finalLimit += 1;
        }
        for (int i = 0; i < finalLimit; i++) {
            switch (direction) {
                case NORTH -> location.setZ(location.getZ() - 1);
                case SOUTH -> location.setZ(location.getZ() + 1);
                case EAST -> location.setX(location.getX() + 1);
                case WEST -> location.setX(location.getX() - 1);
                case UP -> location.setY(location.getY() + 1);
                case DOWN -> location.setY(location.getY() - 1);
            }
            requireRegionOwnership(location);
            final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
            if (blockMenu == null) {
                if (skipNoMenu) {
                    continue;
                } else {
                    return;
                }
            }
            consumer.accept(blockMenu);
        }
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
        requireRegionOwnership(startLocation);
        Location location = startLocation.clone();
        int finalLimit = limit;
        if (optimizeExperience) {
            finalLimit += 1;
        }
        for (int i = 0; i < finalLimit; i++) {
            switch (direction) {
                case NORTH -> location.setZ(location.getZ() - 1);
                case SOUTH -> location.setZ(location.getZ() + 1);
                case EAST -> location.setX(location.getX() + 1);
                case WEST -> location.setX(location.getX() - 1);
                case UP -> location.setY(location.getY() + 1);
                case DOWN -> location.setY(location.getY() - 1);
            }
            requireRegionOwnership(location);
            BlockState state = location.getBlock().getState(false);
            if (state instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                if (inv != null) {
                    var wrapper = new VanillaInventoryWrapper(inv, state);
                    consumer.accept(wrapper);
                }
            } else {
                if (skipNoInventory) {
                    continue;
                } else {
                    return;
                }
            }
        }
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
        requireRegionOwnership(startLocation);
        Location location = startLocation.clone();
        int finalLimit = limit;
        if (optimizeExperience) {
            finalLimit += 1;
        }
        for (int i = 0; i < finalLimit; i++) {
            switch (direction) {
                case NORTH -> location.setZ(location.getZ() - 1);
                case SOUTH -> location.setZ(location.getZ() + 1);
                case EAST -> location.setX(location.getX() + 1);
                case WEST -> location.setX(location.getX() - 1);
                case UP -> location.setY(location.getY() + 1);
                case DOWN -> location.setY(location.getY() - 1);
            }
            requireRegionOwnership(location);
            final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
            if (blockMenu == null) {
                if (!allowNoMenu) {
                    return;
                }
            }
            consumer.accept(location);
        }
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
        final int[] slots =
            blockMenu.getPreset().getSlotsAccessedByItemTransport(blockMenu, ItemTransportFlow.WITHDRAW, null);

        int limit = limitQuantity;
        switch (transportMode) {
            case NONE, NONNULL_ONLY -> {
                /*
                 * Grab all the items.
                 */
                for (int slot : slots) {
                    final ItemStack item = blockMenu.getItemInSlot(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        final int exceptedReceive = Math.min(item.getAmount(), limit);
                        final ItemStack clone = StackUtils.getAsQuantity(item, exceptedReceive);
                        root.addItemStack0(accessor, clone);
                        item.setAmount(item.getAmount() - (exceptedReceive - clone.getAmount()));
                        limit -= exceptedReceive - clone.getAmount();
                        if (limit <= 0) {
                            break;
                        }
                    }
                }
            }
            case NULL_ONLY -> {
                /*
                 * Nothing to do.
                 */
            }
            case FIRST_ONLY -> {
                /*
                 * Grab the first item only.
                 */
                if (slots.length > 0) {
                    final ItemStack item = blockMenu.getItemInSlot(slots[0]);
                    if (item != null && item.getType() != Material.AIR) {
                        final int exceptedReceive = Math.min(item.getAmount(), limit);
                        final ItemStack clone = StackUtils.getAsQuantity(item, exceptedReceive);
                        root.addItemStack0(accessor, clone);
                        item.setAmount(item.getAmount() - (exceptedReceive - clone.getAmount()));
                        clone.getAmount();
                    }
                }
            }
            case LAST_ONLY -> {
                /*
                 * Grab the last item only.
                 */
                if (slots.length > 0) {
                    final ItemStack item = blockMenu.getItemInSlot(slots[slots.length - 1]);
                    if (item != null && item.getType() != Material.AIR) {
                        final int exceptedReceive = Math.min(item.getAmount(), limit);
                        final ItemStack clone = StackUtils.getAsQuantity(item, exceptedReceive);
                        root.addItemStack0(accessor, clone);
                        item.setAmount(item.getAmount() - (exceptedReceive - clone.getAmount()));
                        clone.getAmount();
                    }
                }
            }
            case FIRST_STOP -> {
                /*
                 * Grab the first non-null item only.
                 */
                for (int slot : slots) {
                    final ItemStack item = blockMenu.getItemInSlot(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        final int exceptedReceive = Math.min(item.getAmount(), limit);
                        final ItemStack clone = StackUtils.getAsQuantity(item, exceptedReceive);
                        root.addItemStack0(accessor, clone);
                        item.setAmount(item.getAmount() - (exceptedReceive - clone.getAmount()));
                        clone.getAmount();
                        break;
                    }
                }
            }
            case LAZY -> {
                /*
                 * When it's first item is non-null, we will grab all the items.
                 */
                if (slots.length > 0) {
                    final ItemStack delta = blockMenu.getItemInSlot(slots[0]);
                    if (delta != null && delta.getType() != Material.AIR) {
                        for (int slot : slots) {
                            ItemStack item = blockMenu.getItemInSlot(slot);
                            if (item != null && item.getType() != Material.AIR) {
                                final int exceptedReceive = Math.min(item.getAmount(), limit);
                                final ItemStack clone = StackUtils.getAsQuantity(item, exceptedReceive);
                                root.addItemStack0(accessor, clone);
                                item.setAmount(item.getAmount() - (exceptedReceive - clone.getAmount()));
                                limit -= exceptedReceive - clone.getAmount();
                                if (limit <= 0) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            case VOID -> {
                /*
                 * Grab all the items or trash it
                 */
                for (int slot : slots) {
                    final ItemStack item = blockMenu.getItemInSlot(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        final int exceptedReceive = Math.min(item.getAmount(), limit);
                        final ItemStack clone = StackUtils.getAsQuantity(item, exceptedReceive);
                        root.addItemStack0(accessor, clone);
                        limit -= exceptedReceive - clone.getAmount();
                        item.setAmount(0);
                        if (limit <= 0) {
                            break;
                        }
                    }
                }
            }
            case SPECIFIED_QUANTITY -> {
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
                for (var entry : itemSamples.entrySet()) {
                    final int total = itemTotals.get(entry.getKey());
                    if (total <= limitQuantity) {
                        continue;
                    }
                    int toRemove = total - limitQuantity;
                    for (int i = slots.length - 1; i >= 0 && toRemove > 0; i--) {
                        final ItemStack item = blockMenu.getItemInSlot(slots[i]);
                        if (item == null || item.getType() == Material.AIR) {
                            continue;
                        }
                        if (!StackUtils.itemsMatch(entry.getValue(), item)) {
                            continue;
                        }
                        final int grabFromSlot = Math.min(item.getAmount(), toRemove);
                        final int beforeAmount = item.getAmount();
                        item.setAmount(grabFromSlot);
                        root.addItemStack0(accessor, item);
                        final int afterAmount = item.getAmount();
                        final int actualGrabbed = grabFromSlot - afterAmount;
                        item.setAmount(beforeAmount - actualGrabbed);
                        toRemove -= actualGrabbed;
                    }
                }
            }
        }
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
        final ItemRequest itemRequest = new ItemRequest(clone, clone.getMaxStackSize());

        final int[] slots =
            blockMenu.getPreset().getSlotsAccessedByItemTransport(blockMenu, ItemTransportFlow.INSERT, clone);
        switch (transportMode) {
            case NONE -> {
                int freeSpace = 0;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        freeSpace += clone.getMaxStackSize();
                    } else {
                        if (itemStack.getAmount() >= clone.getMaxStackSize()) {
                            continue;
                        }
                        if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                            final int availableSpace = itemStack.getMaxStackSize() - itemStack.getAmount();
                            if (availableSpace > 0) {
                                freeSpace += availableSpace;
                            }
                        }
                    }
                }
                if (freeSpace <= 0) {
                    return;
                }
                itemRequest.setAmount(Math.min(freeSpace, limitQuantity));

                final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                if (retrieved != null && retrieved.getType() != Material.AIR) {
                    BlockMenuUtil.pushItem(blockMenu, retrieved, slots);
                }
            }

            case NULL_ONLY -> {
                int free = limitQuantity;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        itemRequest.setAmount(clone.getMaxStackSize());
                    } else {
                        continue;
                    }
                    itemRequest.setAmount(Math.min(itemRequest.getAmount(), free));

                    final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                    if (retrieved != null && retrieved.getType() != Material.AIR) {
                        free -= retrieved.getAmount();
                        BlockMenuUtil.pushItem(blockMenu, retrieved, slot);
                        if (free <= 0) {
                            break;
                        }
                    }
                }
            }

            case NONNULL_ONLY -> {
                int free = limitQuantity;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        continue;
                    }
                    if (itemStack.getAmount() >= clone.getMaxStackSize()) {
                        continue;
                    }
                    if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                        final int space = itemStack.getMaxStackSize() - itemStack.getAmount();
                        if (space > 0) {
                            itemRequest.setAmount(space);
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                    itemRequest.setAmount(Math.min(itemRequest.getAmount(), free));

                    final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                    if (retrieved != null && retrieved.getType() != Material.AIR) {
                        free -= retrieved.getAmount();
                        BlockMenuUtil.pushItem(blockMenu, retrieved, slot);
                        if (free <= 0) {
                            break;
                        }
                    }
                }
            }
            case FIRST_ONLY -> {
                if (slots.length == 0) {
                    break;
                }
                final int slot = slots[0];
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    itemRequest.setAmount(clone.getMaxStackSize());
                } else {
                    if (itemStack.getAmount() >= clone.getMaxStackSize()) {
                        return;
                    }
                    if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                        final int space = itemStack.getMaxStackSize() - itemStack.getAmount();
                        if (space > 0) {
                            itemRequest.setAmount(space);
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                itemRequest.setAmount(Math.min(itemRequest.getAmount(), limitQuantity));

                final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                if (retrieved != null && retrieved.getType() != Material.AIR) {
                    retrieved.getAmount();
                    BlockMenuUtil.pushItem(blockMenu, retrieved, slot);
                }
            }
            case LAST_ONLY -> {
                if (slots.length == 0) {
                    break;
                }
                final int slot = slots[slots.length - 1];
                final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    itemRequest.setAmount(clone.getMaxStackSize());
                } else {
                    if (itemStack.getAmount() >= clone.getMaxStackSize()) {
                        return;
                    }
                    if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                        final int space = itemStack.getMaxStackSize() - itemStack.getAmount();
                        if (space > 0) {
                            itemRequest.setAmount(space);
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                itemRequest.setAmount(Math.min(itemRequest.getAmount(), limitQuantity));

                final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                if (retrieved != null && retrieved.getType() != Material.AIR) {
                    retrieved.getAmount();
                    BlockMenuUtil.pushItem(blockMenu, retrieved, slot);
                }
            }
            case FIRST_STOP -> {
                int freeSpace = 0;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        freeSpace += clone.getMaxStackSize();
                        break;
                    } else {
                        if (itemStack.getAmount() >= clone.getMaxStackSize()) {
                            continue;
                        }
                        if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                            final int availableSpace = itemStack.getMaxStackSize() - itemStack.getAmount();
                            if (availableSpace > 0) {
                                freeSpace += availableSpace;
                            }
                        }
                        break;
                    }
                }
                if (freeSpace <= 0) {
                    return;
                }
                itemRequest.setAmount(Math.min(freeSpace, limitQuantity));

                final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                if (retrieved != null && retrieved.getType() != Material.AIR) {
                    BlockMenuUtil.pushItem(blockMenu, retrieved, slots);
                }
            }
            case LAZY -> {
                if (slots.length > 0) {
                    final ItemStack delta = blockMenu.getItemInSlot(slots[0]);
                    if (delta == null || delta.getType() == Material.AIR) {
                        int freeSpace = 0;
                        for (int slot : slots) {
                            final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                            if (itemStack == null || itemStack.getType() == Material.AIR) {
                                freeSpace += clone.getMaxStackSize();
                            } else {
                                if (itemStack.getAmount() >= clone.getMaxStackSize()) {
                                    continue;
                                }
                                if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                                    final int availableSpace = itemStack.getMaxStackSize() - itemStack.getAmount();
                                    if (availableSpace > 0) {
                                        freeSpace += availableSpace;
                                    }
                                }
                            }
                        }
                        if (freeSpace <= 0) {
                            return;
                        }
                        itemRequest.setAmount(Math.min(freeSpace, limitQuantity));

                        final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                        if (retrieved != null && retrieved.getType() != Material.AIR) {
                            BlockMenuUtil.pushItem(blockMenu, retrieved, slots);
                        }
                    }
                }
            }
            case VOID -> {
                itemRequest.setAmount(limitQuantity);

                final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                if (retrieved != null && retrieved.getType() != Material.AIR) {
                    BlockMenuUtil.pushItem(blockMenu, retrieved, slots);
                }
            }
            case SPECIFIED_QUANTITY -> {
                int existingCount = 0;
                for (int slot : slots) {
                    final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                    if (itemStack != null && itemStack.getType() != Material.AIR) {
                        if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                            existingCount += itemStack.getAmount();
                        }
                    }
                }
                if (existingCount < limitQuantity) {
                    final int deficit = limitQuantity - existingCount;
                    int availableSpace = 0;
                    for (int slot : slots) {
                        final ItemStack itemStack = blockMenu.getItemInSlot(slot);
                        if (itemStack == null || itemStack.getType() == Material.AIR) {
                            availableSpace += clone.getMaxStackSize();
                        } else if (StackUtils.itemsMatch(itemRequest, itemStack)) {
                            availableSpace += Math.max(0, itemStack.getMaxStackSize() - itemStack.getAmount());
                        }
                    }
                    if (availableSpace <= 0) {
                        return;
                    }
                    final int toRequest = Math.min(deficit, availableSpace);
                    itemRequest.setAmount(toRequest);
                    final ItemStack retrieved = root.getItemStack0(accessor, itemRequest);
                    if (retrieved != null && retrieved.getType() != Material.AIR) {
                        BlockMenuUtil.pushItem(blockMenu, retrieved, slots);
                    }
                }
            }
        }
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
        requireRegionOwnership(location);
        final SlimefunBlockData blockData = StorageCacheUtils.getBlock(location);
        if (blockData == null) {
            return;
        }

        if (!blockData.isDataLoaded()) {
            StorageCacheUtils.requestLoad(blockData);
            return;
        }

        final SlimefunItem slimefunItem = SlimefunItem.getById(blockData.getSfId());
        if (!(slimefunItem instanceof EnergyNetComponent component) || slimefunItem instanceof NetworkObject) {
            return;
        }

        int existingCharge = component.getCharge(location);

        final int capacity = component.getCapacity();
        final int space = capacity - existingCharge;

        if (space <= 0) {
            return;
        }

        final int possibleGeneration = Math.min(rate, space);
        final long power = root.getRootPower();

        if (power <= 0) {
            return;
        }

        final int gen = power < possibleGeneration ? (int) power : possibleGeneration;

        component.addCharge(location, gen);
        root.removeRootPower(gen);
    }

    private static void requireRegionOwnership(@NotNull Location location) {
        if (location.getWorld() != null && !FoliaSupport.isOwnedByCurrentRegion(location)) {
            throw new IllegalStateException("Cross-region line operation at " + location);
        }
    }
}
