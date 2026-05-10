package io.github.sefiraat.networks.slimefun.network;

import com.balugaq.netex.api.enums.FeedbackType;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("DuplicatedCode")
public class NetworkImport extends NetworkObject {

    private static final int[] INPUT_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final Set<org.bukkit.Location> PENDING_IMPORTS = ConcurrentHashMap.newKeySet();

    private final @NotNull ItemSetting<Integer> tickRate;

    public NetworkImport(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.IMPORT);

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
            sendFeedback(menuLocation, FeedbackType.NO_NETWORK_FOUND);
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
                setSize(9);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (NetworkSlimefunItems.NETWORK_IMPORT.canUse(player, false)
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
}
