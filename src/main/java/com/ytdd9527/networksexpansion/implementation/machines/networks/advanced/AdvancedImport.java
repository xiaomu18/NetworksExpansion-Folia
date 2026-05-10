package com.ytdd9527.networksexpansion.implementation.machines.networks.advanced;

import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.ytdd9527.networksexpansion.implementation.ExpansionItems;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancedImport extends NetworkObject implements RecipeDisplayItem {
    private static final int[] INPUT_SLOTS = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35,
        36, 37, 38, 39, 40, 41, 42, 43, 44,
        45, 46, 47, 48, 49, 50, 51, 52, 53
    };
    private static final Set<org.bukkit.Location> PENDING_IMPORTS = ConcurrentHashMap.newKeySet();
    private final @NotNull ItemSetting<Integer> tickRate;

    public AdvancedImport(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ADVANCED_IMPORT);

        this.tickRate = new IntRangeSetting(this, "tick_rate", 1, 1, 10);
        addItemSetting(this.tickRate);

        for (int inputSlot : INPUT_SLOTS) {
            this.getSlotsToDrop().add(inputSlot);
        }

        addItemHandler(new BlockTicker() {
            private final Map<Block, Integer> tickMap = new ConcurrentHashMap<>();

            @Override
            public boolean isSynchronized() {
                return runSync();
            }

            @Override
            public void tick(@NotNull Block block, SlimefunItem item, @NotNull SlimefunBlockData data) {
                final int tick = tickMap.getOrDefault(block, 1);
                if (tick <= 1) {
                    final BlockMenu blockMenu = data.getBlockMenu();
                    if (blockMenu == null) {
                        return;
                    }
                    addToRegistry(block);
                    tryAddItem(blockMenu);
                }
                tickMap.put(block, tick <= 1 ? tickRate.getValue() : tick - 1);
            }
        });
    }

    private void tryAddItem(@NotNull BlockMenu blockMenu) {
        final org.bukkit.Location menuLocation = blockMenu.getLocation();
        final NodeDefinition definition = NetworkStorage.getNode(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        if (!PENDING_IMPORTS.add(menuLocation)) {
            return;
        }

        final NetworkRoot root = definition.getNode().getRoot();
        pushInputsAsync(blockMenu, root, 0).whenComplete((ignored, throwable) ->
            FoliaSupport.runRegion(menuLocation, () -> {
                PENDING_IMPORTS.remove(menuLocation);
                sendFeedback(menuLocation, FeedbackType.WORKING);
            }));
    }

    private CompletableFuture<Void> pushInputsAsync(
        @NotNull BlockMenu blockMenu,
        @NotNull NetworkRoot root,
        int index) {
        if (index >= INPUT_SLOTS.length) {
            return CompletableFuture.completedFuture(null);
        }

        final int slot = INPUT_SLOTS[index];
        final ItemStack current = blockMenu.getItemInSlot(slot);
        if (current == null || current.getType() == Material.AIR) {
            return pushInputsAsync(blockMenu, root, index + 1);
        }

        final ItemStack transfer = current.clone();
        final int originalAmount = transfer.getAmount();
        return root.addItemStack0Async(blockMenu.getLocation(), transfer)
            .thenCompose(ignored -> FoliaSupport.supplyRegion(blockMenu.getLocation(), () -> {
                final int moved = Math.max(0, originalAmount - transfer.getAmount());
                if (moved > 0) {
                    final ItemStack live = blockMenu.getItemInSlot(slot);
                    if (live != null && live.getType() != Material.AIR) {
                        live.setAmount(Math.max(0, live.getAmount() - moved));
                    }
                }
                return null;
            }))
            .thenCompose(ignored -> pushInputsAsync(blockMenu, root, index + 1));
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                setSize(54);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (ExpansionItems.ADVANCED_IMPORT.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return INPUT_SLOTS;
                }
                return new int[0];
            }
        };
    }

    @NotNull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> displayRecipes = new ArrayList<>();
        displayRecipes.add(Lang.getMechanism("import"));
        return displayRecipes;
    }
}
