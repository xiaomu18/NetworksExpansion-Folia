package io.github.sefiraat.networks.slimefun.network;

import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.interfaces.SoftCellBannable;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("DuplicatedCode")
public class NetworkGrabber extends NetworkDirectional implements SoftCellBannable {
    private static final Set<org.bukkit.Location> PENDING_GRABS = ConcurrentHashMap.newKeySet();

    public NetworkGrabber(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.GRABBER);
    }

    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @NotNull Block block) {
        super.onTick(blockMenu, block);
        if (blockMenu != null) {
            tryGrabItem(blockMenu);
        }
    }

    private void tryGrabItem(@NotNull BlockMenu blockMenu) {
        final org.bukkit.Location menuLocation = blockMenu.getLocation();
        final NodeDefinition definition = NetworkStorage.getNode(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            sendFeedback(menuLocation, FeedbackType.NO_NETWORK_FOUND);
            return;
        }

        final NetworkRoot root = definition.getNode().getRoot();
        if (checkSoftCellBan(menuLocation, root)) {
            return;
        }

        final BlockFace direction = this.getCurrentDirection(blockMenu);
        final Block targetBlock = blockMenu.getBlock().getRelative(direction);
        if (!canDirectlyAccess(targetBlock.getLocation())) {
            sendFeedback(blockMenu.getLocation(), FeedbackType.NO_TARGET_BLOCK);
            return;
        }
        final BlockMenu targetMenu = StorageCacheUtils.getMenu(targetBlock.getLocation());

        if (targetMenu == null) {
            sendFeedback(blockMenu.getLocation(), FeedbackType.NO_TARGET_BLOCK);
            return;
        }

        int[] slots =
            targetMenu.getPreset().getSlotsAccessedByItemTransport(targetMenu, ItemTransportFlow.WITHDRAW, null);

        for (int slot : slots) {
            final ItemStack itemStack = targetMenu.getItemInSlot(slot);

            if (itemStack != null && itemStack.getType() != Material.AIR) {
                if (!PENDING_GRABS.add(menuLocation)) {
                    return;
                }

                final int targetSlot = slot;
                final ItemStack transfer = itemStack.clone();
                final int originalAmount = transfer.getAmount();
                root.addItemStack0Async(menuLocation, transfer).whenComplete((ignored, throwable) ->
                    FoliaSupport.runRegion(targetMenu.getLocation(), () -> {
                        final int moved = Math.max(0, originalAmount - transfer.getAmount());
                        if (moved > 0) {
                            final ItemStack live = targetMenu.getItemInSlot(targetSlot);
                            if (live != null && live.getType() != Material.AIR) {
                                live.setAmount(Math.max(0, live.getAmount() - moved));
                            }
                        }
                        FoliaSupport.runRegion(menuLocation, () -> {
                            PENDING_GRABS.remove(menuLocation);
                            sendFeedback(menuLocation, FeedbackType.WORKING);
                            if (root.isDisplayParticles() && moved > 0) {
                                showParticle(menuLocation, direction);
                            }
                        });
                    }));
                return;
            }
        }
    }

    @Override
    protected Particle.@NotNull DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.FUCHSIA, 1);
    }
}
