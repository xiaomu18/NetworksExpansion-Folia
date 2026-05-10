package com.ytdd9527.networksexpansion.implementation.machines.cargo.transfer.point.basic;

import com.balugaq.netex.api.atrributes.WhitelistedGrabber;
import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.enums.TransferType;
import com.balugaq.netex.api.factories.TransferConfigFactory;
import com.balugaq.netex.api.helpers.Icon;
import com.balugaq.netex.api.interfaces.SoftCellBannable;
import com.balugaq.netex.api.transfer.TransferConfiguration;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.slimefun.network.NetworkDirectional;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WhitelistedTransferGrabber extends NetworkDirectional implements SoftCellBannable, WhitelistedGrabber {
    private static final TransferConfiguration config = TransferConfigFactory
        .getTransferConfiguration(TransferType.WHITELISTED_TRANSFER_GRABBER);

    public WhitelistedTransferGrabber(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.GRABBER);
        for (int slot : getItemSlots()) {
            this.getSlotsToDrop().add(slot);
        }
    }

    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @NotNull Block block) {
        super.onTick(blockMenu, block);
        if (blockMenu != null) {
            tryGrabItem(blockMenu);
        }
    }

    private void tryGrabItem(@NotNull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getNode(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            sendFeedback(blockMenu.getLocation(), FeedbackType.NO_NETWORK_FOUND);
            return;
        }

        NetworkRoot root = definition.getNode().getRoot();

        if (checkSoftCellBan(blockMenu.getLocation(), root)) {
            return;
        }

        final BlockFace direction = this.getCurrentDirection(blockMenu);
        final org.bukkit.Location targetLocation = blockMenu.getBlock().getRelative(direction).getLocation();
        final List<ItemStack> templates = getClonedTemplateItems(blockMenu);
        FoliaSupport.runRegion(targetLocation, () -> {
            final BlockMenu targetMenu = StorageCacheUtils.getMenu(targetLocation);
            if (targetMenu == null) {
                FoliaSupport.runRegion(blockMenu.getLocation(), () -> sendFeedback(blockMenu.getLocation(), FeedbackType.NO_TARGET_BLOCK));
                return;
            }

            grabMenu(blockMenu, targetMenu, root, templates);
            FoliaSupport.runRegion(blockMenu.getLocation(), () -> sendFeedback(blockMenu.getLocation(), FeedbackType.WORKING));
        });
    }

    @Override
    public int getNorthSlot() {
        return config.getNorthSlot();
    }

    @Override
    public int getSouthSlot() {
        return config.getSouthSlot();
    }

    @Override
    public int getEastSlot() {
        return config.getEastSlot();
    }

    @Override
    public int getWestSlot() {
        return config.getWestSlot();
    }

    @Override
    public int getUpSlot() {
        return config.getUpSlot();
    }

    @Override
    public int getDownSlot() {
        return config.getDownSlot();
    }

    @Nullable
    @Override
    protected ItemStack getOtherBackgroundStack() {
        return Icon.GRABBER_TEMPLATE_BACKGROUND_STACK;
    }

    @Override
    public int @NotNull [] getBackgroundSlots() {
        return config.getBackgroundSlots();
    }

    @Override
    public int @NotNull [] getOtherBackgroundSlots() {
        return config.getTemplateBackgroundSlots();
    }

    @Override
    public int @NotNull [] getItemSlots() {
        return config.getTemplateSlots();
    }

    @Override
    public boolean runSync() {
        return true;
    }

    @Override
    public int[] getTemplateSlots() {
        return config.getTemplateSlots();
    }
}
