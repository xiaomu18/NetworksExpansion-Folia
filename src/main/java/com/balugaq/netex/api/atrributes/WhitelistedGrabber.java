package com.balugaq.netex.api.atrributes;

import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.utils.StackUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface WhitelistedGrabber {
    @NotNull
    default List<ItemStack> getClonedTemplateItems(@NotNull BlockMenu menu) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : getTemplateSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            items.add(item.clone());
        }

        return items;
    }

    default boolean inTemplates(@NotNull List<ItemStack> templates, @Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }

        for (ItemStack template : templates) {
            if (StackUtils.itemsMatch(template, stack)) {
                return true;
            }
        }

        return false;
    }

    default void grabMenu(@NotNull BlockMenu blockMenu, @NotNull BlockMenu targetMenu, @NotNull NetworkRoot root, @NotNull List<ItemStack> templates) {
        final int[] slots =
            targetMenu.getPreset().getSlotsAccessedByItemTransport(targetMenu, ItemTransportFlow.WITHDRAW, null);

        for (int slot : slots) {
            final ItemStack itemInSlot = targetMenu.getItemInSlot(slot);

            if (itemInSlot != null && itemInSlot.getType() != Material.AIR) {
                boolean found = false;
                for (ItemStack template : templates) {
                    if (StackUtils.itemsMatch(template, itemInSlot)) {
                        final ItemStack transfer = itemInSlot.clone();
                        final int originalAmount = transfer.getAmount();
                        root.addItemStack0Async(blockMenu.getLocation(), transfer).whenComplete((ignored, throwable) ->
                            FoliaSupport.runRegion(targetMenu.getLocation(), () -> {
                                final int moved = Math.max(0, originalAmount - transfer.getAmount());
                                if (moved > 0) {
                                    final ItemStack live = targetMenu.getItemInSlot(slot);
                                    if (live != null && live.getType() != Material.AIR) {
                                        live.setAmount(Math.max(0, live.getAmount() - moved));
                                    }
                                }
                            }));
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
    }

    int[] getTemplateSlots();
}
