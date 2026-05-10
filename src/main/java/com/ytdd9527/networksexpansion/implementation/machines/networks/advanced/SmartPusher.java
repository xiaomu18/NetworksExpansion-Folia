package com.ytdd9527.networksexpansion.implementation.machines.networks.advanced;

import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.utils.BlockMenuUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.network.AdminDebuggable;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("DuplicatedCode")
public class SmartPusher extends SpecialSlimefunItem implements AdminDebuggable {
    private static final Map<Location, BlockFace> DIRECTIONS = new ConcurrentHashMap<>();
    private static final Set<Location> PENDING_PUSHES = ConcurrentHashMap.newKeySet();
    private static final Set<BlockFace> VALID_FACES =
        EnumSet.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    private static final int[] BACKGROUND_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final int[] TEMPLATE_SLOTS = {4};

    public SmartPusher(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        @NotNull ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            @ParametersAreNonnullByDefault
            public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
                final Location location = event.getBlock().getLocation();
                final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);

                if (blockMenu != null) {
                    blockMenu.dropItems(location, TEMPLATE_SLOTS);
                }

                Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
            }
        });
    }

    @Nullable
    public static BlockFace getDirection(Location location) {
        return DIRECTIONS.get(location);
    }

    public static void setDirection(Location location, BlockFace face) {
        DIRECTIONS.put(location, face);
    }

    public static void removeDirection(Location location) {
        DIRECTIONS.remove(location);
    }

    private static boolean canDirectlyAccess(@NotNull Location location) {
        return location.getWorld() == null || FoliaSupport.isOwnedByCurrentRegion(location);
    }

    @Override
    public void preRegister() {
        addItemHandler(
            new BlockTicker() {
                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(
                    @NotNull Block block,
                    SlimefunItem slimefunItem,
                    @NotNull SlimefunBlockData slimefunBlockData) {
                    final Location location = block.getLocation();
                    final BlockFace cachedFace = getDirection(location);
                    if (cachedFace != null && VALID_FACES.contains(cachedFace)) {
                        final BlockMenu blockMenu = slimefunBlockData.getBlockMenu();
                        if (blockMenu != null) {
                            onTick(blockMenu, cachedFace);
                        } else {
                            sendFeedback(location, FeedbackType.INVALID_BLOCK);
                        }
                    } else if (block.getBlockData() instanceof Directional directional) {
                        final BlockFace face = directional.getFacing();
                        setDirection(location, face);
                        sendFeedback(block.getLocation(), FeedbackType.INITIALIZATION);
                    } else {
                        Slimefun.getDatabaseManager()
                            .getBlockDataController()
                            .removeBlock(location);
                        sendFeedback(block.getLocation(), FeedbackType.INVALID_BLOCK);
                    }
                }
            },
            new BlockBreakHandler(false, false) {
                @Override
                public void onPlayerBreak(
                    @NotNull BlockBreakEvent blockBreakEvent,
                    @NotNull ItemStack itemStack,
                    @NotNull List<ItemStack> list) {
                    removeDirection(blockBreakEvent.getBlock().getLocation());
                    final Location location = blockBreakEvent.getBlock().getLocation();
                    final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);
                    if (blockMenu != null) {
                        blockMenu.dropItems(location, getTemplateSlots());
                    }
                }
            },
            new BlockPlaceHandler(false) {
                @Override
                public void onPlayerPlace(@NotNull BlockPlaceEvent blockPlaceEvent) {
                    if (blockPlaceEvent.getBlock().getBlockData() instanceof Directional directional) {
                        final BlockFace face = directional.getFacing();
                        setDirection(blockPlaceEvent.getBlock().getLocation(), face);
                    }
                }
            });
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                setSize(9);
                drawBackground(getBackgroundSlots());
            }

            @Override
            public void newInstance(@NotNull BlockMenu blockMenu, @NotNull Block b) {
                for (int backgroundSlot : getBackgroundSlots()) {
                    blockMenu.addMenuClickHandler(backgroundSlot, ChestMenuUtils.getEmptyClickHandler());
                }
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (this.getSlimefunItem().canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }

    public void onTick(@NotNull BlockMenu blockMenu, @NotNull BlockFace bridgeFace) {
        final Location menuLocation = blockMenu.getLocation();
        final BlockFace containerFace = bridgeFace.getOppositeFace();
        final Block thisBlock = blockMenu.getBlock();
        final Block bridge = thisBlock.getRelative(bridgeFace);
        final Block container = thisBlock.getRelative(containerFace);
        final Location bridgeLocation = bridge.getLocation();
        final Location containerLocation = container.getLocation();
        FoliaSupport.runRegion(bridgeLocation, () -> {
            final NodeDefinition definition = NetworkStorage.getNode(bridgeLocation);
            if (definition == null || definition.getNode() == null) {
                FoliaSupport.runRegion(menuLocation, () -> sendFeedback(menuLocation, FeedbackType.NO_NETWORK_FOUND));
                return;
            }

            final NetworkRoot root = definition.getNode().getRoot();
            FoliaSupport.runRegion(containerLocation, () -> {
                final BlockMenu targetMenu = StorageCacheUtils.getMenu(containerLocation);
                if (targetMenu == null) {
                    FoliaSupport.runRegion(menuLocation, () -> sendFeedback(menuLocation, FeedbackType.NO_TARGET_BLOCK));
                    return;
                }

                final int[] slots = targetMenu
                    .getPreset()
                    .getSlotsAccessedByItemTransport(targetMenu, ItemTransportFlow.INSERT, null);
                for (ItemStack template : getTemplateItems(blockMenu)) {
                    final ItemStack clone = StackUtils.getAsQuantity(template, 1);
                    final ItemRequest itemRequest = new ItemRequest(clone, clone.getMaxStackSize());
                    final int limitQuantity = getLimitQuantity();
                    if (slots.length > 0) {
                        final ItemStack delta = targetMenu.getItemInSlot(slots[0]);
                        if (delta == null || delta.getType() == Material.AIR) {
                            int freeSpace = 0;
                            for (int slot : slots) {
                                final ItemStack itemStack = targetMenu.getItemInSlot(slot);
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
                                continue;
                            }
                            itemRequest.setAmount(Math.min(freeSpace, limitQuantity));

                            if (!PENDING_PUSHES.add(menuLocation)) {
                                return;
                            }
                            root.getItemStack0Async(menuLocation, itemRequest).whenComplete((retrieved, throwable) ->
                                FoliaSupport.runRegion(containerLocation, () -> {
                                    final BlockMenu liveTargetMenu = StorageCacheUtils.getMenu(containerLocation);
                                    FoliaSupport.runRegion(menuLocation, () -> {
                                        PENDING_PUSHES.remove(menuLocation);
                                        if (retrieved != null && retrieved.getType() != Material.AIR && liveTargetMenu != null) {
                                            BlockMenuUtil.pushItem(liveTargetMenu, retrieved, slots);
                                            sendFeedback(menuLocation, FeedbackType.WORKING);
                                        }
                                    });
                                }));
                            return;
                        }
                    }
                }
                FoliaSupport.runRegion(menuLocation, () -> sendFeedback(menuLocation, FeedbackType.WORKING));
            });
        });
    }

    public int getLimitQuantity() {
        return 3456;
    }

    public @NotNull List<ItemStack> getTemplateItems(@NotNull BlockMenu blockMenu) {
        final List<ItemStack> items = new ArrayList<>();
        for (int slot : getTemplateSlots()) {
            final ItemStack itemStack = blockMenu.getItemInSlot(slot);
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                items.add(itemStack);
            }
        }

        return items;
    }

    public int[] getTemplateSlots() {
        return TEMPLATE_SLOTS;
    }

    public int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }
}
