package io.github.sefiraat.networks.slimefun.network;

import com.balugaq.netex.api.interfaces.HangingBlock;
import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.exceptions.IncompatibleItemHandlerException;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public abstract class NetworkObject extends SpecialSlimefunItem implements AdminDebuggable {
    protected static final Set<BlockFace> CHECK_FACES =
        Set.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    private final NodeType nodeType;
    private final List<Integer> slotsToDrop = new ArrayList<>();
    private final Set<Location> firstTickLocations = ConcurrentHashMap.newKeySet();

    protected NetworkObject(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        @NotNull ItemStack @NotNull [] recipe,
        @Range(from = 1, to = 64) int outputAmount,
        @NotNull NodeType type
    ) {
        this(itemGroup, item, recipeType, recipe, StackUtils.getAsQuantity(item, outputAmount), type);
    }

    protected NetworkObject(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe,
        NodeType type) {
        this(itemGroup, item, recipeType, recipe, null, type);
    }

    protected NetworkObject(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe,
        ItemStack recipeOutput,
        NodeType type) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
        this.nodeType = type;
        addItemHandler(
            new BlockTicker() {

                @Override
                public boolean isSynchronized() {
                    return runSync();
                }

                @Override
                    public void tick(@NotNull Block b, SlimefunItem item, @NotNull SlimefunBlockData data) {
                    if (!firstTickLocations.contains(b.getLocation())) {
                        HangingBlock.loadHangingBlocks(data);
                        HangingBlock.doFirstTick(data);
                        firstTickLocations.add(b.getLocation());
                        return;
                    }

                    addToRegistry(b);
                    tickHangingBlocks(b);
                }

                @Override
                @NotNull
                public Optional<IncompatibleItemHandlerException> validate(@NotNull SlimefunItem slimefunItem) {
                    return Optional.empty();
                }
            },
            new BlockBreakHandler(false, false) {
                @Override
                @ParametersAreNonnullByDefault
                public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
                    preBreak(event);
                    onBreak(event);
                    postBreak(event);
                }
            },
            new BlockPlaceHandler(false) {
                @Override
                @ParametersAreNonnullByDefault
                public void onPlayerPlace(BlockPlaceEvent event) {
                    prePlace(event);
                    onPlace(event);
                    postPlace(event);
                }
            });
    }

    protected void addToRegistry(@NotNull Block block) {
        if (!NetworkStorage.containsKey(block.getLocation())) {
            final NodeDefinition nodeDefinition = new NodeDefinition(nodeType);
            NetworkStorage.registerNode(block.getLocation(), nodeDefinition);
        }
    }

    protected void tickHangingBlocks(@NotNull Block block) {
        HangingBlock.tickHangingBlocks(block);
    }

    @OverridingMethodsMustInvokeSuper
    protected void preBreak(@NotNull BlockBreakEvent event) {
        NetworkRoot.removePersistentAccessHistory(event.getBlock().getLocation());
        NetworkRoot.removeCountObservingAccessHistory(event.getBlock().getLocation());
    }

    @OverridingMethodsMustInvokeSuper
    protected void onBreak(@NotNull BlockBreakEvent event) {
        final Location location = event.getBlock().getLocation();
        final BlockMenu blockMenu = StorageCacheUtils.getMenu(location);

        if (blockMenu != null) {
            for (int i : getSlotsToDrop()) {
                blockMenu.dropItems(location, i);
            }
        }

        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
    }

    @OverridingMethodsMustInvokeSuper
    protected void postBreak(@NotNull BlockBreakEvent event) {
    }

    @OverridingMethodsMustInvokeSuper
    @SuppressWarnings("unused")
    protected void prePlace(@NotNull BlockPlaceEvent event) {
    }

    @SuppressWarnings("unused")
    protected void cancelPlace(@NotNull BlockPlaceEvent event) {
        event.getPlayer().sendMessage(Lang.getString("messages.unsupported-operation.comprehensive.cancel_place"));
        event.setCancelled(true);
    }

    @OverridingMethodsMustInvokeSuper
    protected void onPlace(@NotNull BlockPlaceEvent event) {
    }

    @OverridingMethodsMustInvokeSuper
    @SuppressWarnings("unused")
    protected void postPlace(@NotNull BlockPlaceEvent event) {
    }

    public boolean isAdminDebuggable() {
        return false;
    }

    public boolean runSync() {
        return true;
    }
}
