package io.github.sefiraat.networks.slimefun.network;

import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.helpers.Icon;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkWirelessReceiver extends NetworkObject {

    public static final int RECEIVED_SLOT = 13;

    private static final int[] BACKGROUND_SLOTS =
        new int[]{0, 1, 2, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};

    private static final int[] RECEIVED_SLOTS_TEMPLATE = new int[]{3, 4, 5, 12, 14, 21, 22, 23};
    private static final Set<org.bukkit.Location> PENDING_IMPORTS = ConcurrentHashMap.newKeySet();

    public NetworkWirelessReceiver(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.WIRELESS_RECEIVER);
        this.getSlotsToDrop().add(RECEIVED_SLOT);

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
                    onTick(blockMenu);
                }
            }
        });
    }

    private void onTick(@NotNull BlockMenu blockMenu) {
        final org.bukkit.Location menuLocation = blockMenu.getLocation();
        final NodeDefinition definition = NetworkStorage.getNode(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            sendFeedback(menuLocation, FeedbackType.NO_NETWORK_FOUND);
            return;
        }

        final ItemStack itemStack = blockMenu.getItemInSlot(RECEIVED_SLOT);

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            sendFeedback(menuLocation, FeedbackType.NO_ITEM_FOUND);
            return;
        }

        if (!PENDING_IMPORTS.add(menuLocation)) {
            return;
        }

        final NetworkRoot root = definition.getNode().getRoot();
        final ItemStack transfer = itemStack.clone();
        final int originalAmount = transfer.getAmount();
        root.addItemStack0Async(menuLocation, transfer).whenComplete((ignored, throwable) ->
            FoliaSupport.runRegion(menuLocation, () -> {
                PENDING_IMPORTS.remove(menuLocation);
                final int moved = Math.max(0, originalAmount - transfer.getAmount());
                if (moved > 0) {
                    final ItemStack live = blockMenu.getItemInSlot(RECEIVED_SLOT);
                    if (live != null && live.getType() != Material.AIR) {
                        live.setAmount(Math.max(0, live.getAmount() - moved));
                    }
                }
                sendFeedback(menuLocation, FeedbackType.WORKING);
            }));
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                drawBackground(Icon.RECEIVED_BACKGROUND_STACK, RECEIVED_SLOTS_TEMPLATE);
            }

            @Override
            public boolean canOpen(@NotNull Block block, @NotNull Player player) {
                return player.hasPermission("slimefun.inventory.bypass")
                    || (NetworkSlimefunItems.NETWORK_WIRELESS_RECEIVER.canUse(player, false)
                    && Slimefun.getProtectionManager()
                    .hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK));
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }
}
