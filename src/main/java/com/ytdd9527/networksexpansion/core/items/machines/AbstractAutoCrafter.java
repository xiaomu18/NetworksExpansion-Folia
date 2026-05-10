package com.ytdd9527.networksexpansion.core.items.machines;

import com.balugaq.netex.api.enums.CraftType;
import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.helpers.Icon;
import com.balugaq.netex.api.interfaces.CraftTyped;
import com.balugaq.netex.api.interfaces.SoftCellBannable;
import com.balugaq.netex.utils.BlockMenuUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.ytdd9527.networksexpansion.core.items.unusable.AbstractBlueprint;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.BlueprintInstance;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentCraftingBlueprintType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("DuplicatedCode")
public abstract class AbstractAutoCrafter extends NetworkObject implements SoftCellBannable, CraftTyped {
    public static final int BLUEPRINT_SLOT = 10;
    public static final int OUTPUT_SLOT = 16;
    public static final Map<Location, BlueprintInstance> INSTANCE_MAP = new ConcurrentHashMap<>();
    public static final Map<Location, Boolean> PENDING_CRAFTS = new ConcurrentHashMap<>();
    private static final int[] BACKGROUND_SLOTS = new int[]{3, 4, 5, 12, 13, 14, 21, 22, 23};
    private static final int[] BLUEPRINT_BACKGROUND = new int[]{0, 1, 2, 9, 11, 18, 19, 20};
    private static final int[] OUTPUT_BACKGROUND = new int[]{6, 7, 8, 15, 17, 24, 25, 26};
    protected final int chargePerCraft;
    protected final boolean withholding;

    public AbstractAutoCrafter(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe,
        int chargePerCraft,
        boolean withholding) {
        super(itemGroup, item, recipeType, recipe, NodeType.CRAFTER);

        this.chargePerCraft = chargePerCraft;
        this.withholding = withholding;

        this.getSlotsToDrop().add(BLUEPRINT_SLOT);
        this.getSlotsToDrop().add(OUTPUT_SLOT);

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return runSync();
            }

            @Override
            public void tick(@NotNull Block block, SlimefunItem slimefunItem, @NotNull SlimefunBlockData data) {
                BlockMenu blockMenu = data.getBlockMenu();
                if (blockMenu != null) {
                    addToRegistry(block);
                    craftPreFlight(blockMenu);
                }
            }
        });
    }

    public static void updateCache(@NotNull BlockMenu blockMenu) {
        AbstractAutoCrafter.INSTANCE_MAP.remove(blockMenu.getLocation());
    }

    protected void craftPreFlight(@NotNull BlockMenu blockMenu) {
        final Location menuLocation = blockMenu.getLocation();

        releaseCache(blockMenu);

        final NodeDefinition definition = NetworkStorage.getNode(menuLocation);

        if (definition == null || definition.getNode() == null) {
            sendDebugMessage(menuLocation, "No network found");
            sendFeedback(menuLocation, FeedbackType.NO_NETWORK_FOUND);
            return;
        }

        final NetworkRoot root = definition.getNode().getRoot();

        if (checkSoftCellBan(menuLocation, root)) {
            return;
        }

        if (!withholding) {
            final ItemStack stored = blockMenu.getItemInSlot(OUTPUT_SLOT);
            if (stored != null && stored.getType() != Material.AIR) {
                final ItemStack transfer = stored.clone();
                final int originalAmount = transfer.getAmount();
                root.addItemStack0Async(menuLocation, transfer).whenComplete((ignored, throwable) ->
                    FoliaSupport.runRegion(menuLocation, () -> {
                        final int moved = Math.max(0, originalAmount - transfer.getAmount());
                        if (moved > 0) {
                            final ItemStack live = blockMenu.getItemInSlot(OUTPUT_SLOT);
                            if (live != null && live.getType() != Material.AIR) {
                                live.setAmount(Math.max(0, live.getAmount() - moved));
                            }
                        }
                    }));
                return;
            }
        }

        final ItemStack blueprint = blockMenu.getItemInSlot(BLUEPRINT_SLOT);

        if (blueprint == null || blueprint.getType() == Material.AIR) {
            sendDebugMessage(menuLocation, "No blueprint found");
            sendFeedback(menuLocation, FeedbackType.NO_BLUEPRINT_FOUND);
            return;
        }

        final long networkCharge = root.getRootPower();

        if (networkCharge > this.chargePerCraft) {
            final SlimefunItem item = SlimefunItem.getByItem(blueprint);

            if (!isValidBlueprint(item)) {
                sendDebugMessage(menuLocation, "Invalid blueprint");
                sendFeedback(menuLocation, FeedbackType.INVALID_BLUEPRINT);
                return;
            }

            BlueprintInstance instance = AbstractAutoCrafter.INSTANCE_MAP.get(menuLocation);

            if (instance == null) {
                final ItemMeta blueprintMeta = blueprint.getItemMeta();
                Optional<BlueprintInstance> optional;
                optional = DataTypeMethods.getOptionalCustom(
                    blueprintMeta, Keys.BLUEPRINT_INSTANCE, PersistentCraftingBlueprintType.TYPE);

                if (optional.isEmpty()) {
                    optional = DataTypeMethods.getOptionalCustom(
                        blueprintMeta, Keys.BLUEPRINT_INSTANCE2, PersistentCraftingBlueprintType.TYPE);
                }

                if (optional.isEmpty()) {
                    optional = DataTypeMethods.getOptionalCustom(
                        blueprintMeta, Keys.BLUEPRINT_INSTANCE3, PersistentCraftingBlueprintType.TYPE);
                }

                if (optional.isEmpty()) {
                    sendDebugMessage(menuLocation, "No blueprint instance found");
                    sendFeedback(menuLocation, FeedbackType.NO_BLUEPRINT_INSTANCE_FOUND);
                    return;
                }

                instance = optional.get();
                setCache(blockMenu, instance);
            }

            final ItemStack output = blockMenu.getItemInSlot(OUTPUT_SLOT);
            int blueprintAmount = canBlueprintStack() ? blueprint.getAmount() : 1;

            ItemStack targetOutput = instance.getItemStack();
            if (output != null
                && output.getType() != Material.AIR
                && targetOutput != null
                && (output.getAmount() + targetOutput.getAmount() * blueprintAmount > output.getMaxStackSize()
                || !StackUtils.itemsMatch(targetOutput, output))) {
                sendDebugMessage(menuLocation, "Output slot is full");
                sendFeedback(menuLocation, FeedbackType.OUTPUT_FULL);
                return;
            }

            if (PENDING_CRAFTS.putIfAbsent(menuLocation, Boolean.TRUE) != null) {
                return;
            }
            tryCraftAsync(blockMenu, instance, root, blueprintAmount).whenComplete((success, throwable) ->
                FoliaSupport.runRegion(menuLocation, () -> {
                    PENDING_CRAFTS.remove(menuLocation);
                    if (Boolean.TRUE.equals(success)) {
                        root.removeRootPowerAsync(this.chargePerCraft);
                    }
                }));
        } else {
            sendFeedback(menuLocation, FeedbackType.NOT_ENOUGH_POWER);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private CompletableFuture<Boolean> tryCraftAsync(
        @NotNull BlockMenu blockMenu,
        @NotNull BlueprintInstance instance,
        @NotNull NetworkRoot root,
        int blueprintAmount) {
        /* Make sure the network has the required items
         * Needs to be revisited as matching is happening stacks 2x when it should
         * only need the one
         */
        final Location menuLocation = blockMenu.getLocation();
        HashMap<ItemStack, Integer> requiredItems = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            final ItemStack requested = instance.getRecipeItems()[i];
            if (requested != null) {
                requiredItems.merge(requested, requested.getAmount() * blueprintAmount, Integer::sum);
            }
        }

        return hasRequiredItemsAsync(root, requiredItems).thenCompose(hasRequiredItems -> {
            if (!Boolean.TRUE.equals(hasRequiredItems)) {
                return FoliaSupport.supplyRegion(menuLocation, () -> {
                    sendDebugMessage(menuLocation, "Not enough items in network");
                    sendFeedback(menuLocation, FeedbackType.NOT_ENOUGH_ITEMS_IN_NETWORK);
                    return false;
                });
            }

            return fetchRecipeItemsAsync(blockMenu, root, menuLocation, instance, blueprintAmount, 0, new ItemStack[9]).thenCompose(fetcheds -> {
                if (fetcheds == null) {
                    return CompletableFuture.completedFuture(false);
                }

                return FoliaSupport.supplyRegion(menuLocation, () -> {
                    final Location particleLocation = menuLocation.clone().add(0.5, 1.1, 0.5);
                    if (root.isDisplayParticles()) {
                        particleLocation.getWorld().spawnParticle(Particle.WAX_OFF, particleLocation, 0, 0, 4, 0);
                    }

                    ItemStack crafted = instance.getItemStack().clone();
                    crafted.setAmount(crafted.getAmount() * blueprintAmount);

                    if (crafted.getAmount() > crafted.getMaxStackSize()) {
                        returnItems(root, fetcheds, blockMenu);
                        sendDebugMessage(menuLocation, "Result is too large");
                        sendFeedback(menuLocation, FeedbackType.RESULT_IS_TOO_LARGE);
                        return false;
                    }

                    final ItemStack existing = blockMenu.getItemInSlot(OUTPUT_SLOT);
                    if (existing != null && existing.getType() != Material.AIR) {
                        root.addItemStack0Async(menuLocation, crafted);
                    }
                    if (crafted.getType() != Material.AIR) {
                        BlockMenuUtil.pushItem(blockMenu, crafted, OUTPUT_SLOT);
                    }
                    sendFeedback(menuLocation, FeedbackType.WORKING);
                    return true;
                });
            });
        });
    }

    private CompletableFuture<Boolean> hasRequiredItemsAsync(
        @NotNull NetworkRoot root,
        @NotNull Map<ItemStack, Integer> requiredItems) {
        CompletableFuture<Boolean> chain = CompletableFuture.completedFuture(true);
        for (Map.Entry<ItemStack, Integer> entry : requiredItems.entrySet()) {
            final ItemStack itemStack = entry.getKey();
            final int amount = entry.getValue();
            chain = chain.thenCompose(hasItems -> {
                if (!Boolean.TRUE.equals(hasItems)) {
                    return CompletableFuture.completedFuture(false);
                }
                return root.containsAsync(new ItemRequest(itemStack, amount));
            });
        }
        return chain;
    }

    private CompletableFuture<ItemStack[]> fetchRecipeItemsAsync(
        @NotNull BlockMenu blockMenu,
        @NotNull NetworkRoot root,
        @NotNull Location menuLocation,
        @NotNull BlueprintInstance instance,
        int blueprintAmount,
        int index,
        @NotNull ItemStack[] fetchedItems) {
        if (index >= 9) {
            return CompletableFuture.completedFuture(fetchedItems);
        }

        final ItemStack requested = instance.getRecipeItems()[index];
        if (requested == null) {
            return fetchRecipeItemsAsync(blockMenu, root, menuLocation, instance, blueprintAmount, index + 1, fetchedItems);
        }

        final int requestedAmount = requested.getAmount() * blueprintAmount;
        return root.getItemStack0Async(menuLocation, new ItemRequest(requested, requestedAmount)).thenCompose(fetched -> {
            fetchedItems[index] = fetched;
            if (fetched == null || fetched.getAmount() < requestedAmount) {
                return FoliaSupport.supplyRegion(menuLocation, () -> {
                    returnItems(root, fetchedItems, blockMenu);
                    return (ItemStack[]) null;
                });
            }
            return fetchRecipeItemsAsync(blockMenu, root, menuLocation, instance, blueprintAmount, index + 1, fetchedItems);
        });
    }

    protected void returnItems(
        @NotNull NetworkRoot root, @Nullable ItemStack @NotNull [] inputs, @NotNull BlockMenu blockMenu) {
        for (ItemStack input : inputs) {
            if (input != null) {
                root.addItemStack0Async(blockMenu.getLocation(), input);
            }
        }
    }

    public void releaseCache(@NotNull BlockMenu blockMenu) {
        INSTANCE_MAP.remove(blockMenu.getLocation());
    }

    public void setCache(@NotNull BlockMenu blockMenu, @NotNull BlueprintInstance blueprintInstance) {
        if (!blockMenu.hasViewer()) {
            INSTANCE_MAP.putIfAbsent(blockMenu.getLocation().clone(), blueprintInstance);
        }
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                drawBackground(Icon.BLUEPRINT_BACKGROUND_STACK, BLUEPRINT_BACKGROUND);
                drawBackground(Icon.OUTPUT_BACKGROUND_STACK, OUTPUT_BACKGROUND);
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block b) {
                menu.addMenuOpeningHandler(p -> releaseCache(menu));
                menu.addMenuCloseHandler(p -> releaseCache(menu));
                menu.addMenuClickHandler(BLUEPRINT_SLOT, (player, slot, clickedItem, clickAction) -> {
                    releaseCache(menu);
                    return true;
                });
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
                if (AbstractAutoCrafter.this.withholding && flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_SLOT};
                }
                return new int[0];
            }
        };
    }

    public boolean isValidBlueprint(SlimefunItem item) {
        return item instanceof AbstractBlueprint;
    }

    public boolean canBlueprintStack() {
        return false;
    }
}
