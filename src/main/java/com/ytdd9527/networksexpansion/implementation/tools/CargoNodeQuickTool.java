package com.ytdd9527.networksexpansion.implementation.tools;

import com.balugaq.netex.utils.Debug;
import com.balugaq.netex.utils.Lang;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import com.ytdd9527.networksexpansion.utils.TextUtil;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CargoNodeQuickTool extends SpecialSlimefunItem {
    private final @NotNull NamespacedKey listKey, configKey, cargoKey;
    private final int[] listSlots = {19, 20, 21, 28, 29, 30, 37, 38, 39};
    private final Gson gson = new Gson();

    private static boolean canDirectlyAccess(@NotNull Location location) {
        return location.getWorld() == null || FoliaSupport.isOwnedByCurrentRegion(location);
    }

    private static void runAtTargetRegion(@NotNull Location location, @NotNull Runnable runnable) {
        if (canDirectlyAccess(location)) {
            runnable.run();
        } else {
            FoliaSupport.runRegion(location, runnable);
        }
    }

    private static void sendPlayerMessage(@NotNull Player player, @NotNull String message) {
        FoliaSupport.runPlayer(player, () -> player.sendMessage(message));
    }

    public CargoNodeQuickTool(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
        listKey = Keys.newKey("item_list");
        configKey = Keys.newKey("config");
        cargoKey = Keys.newKey("cargo_type");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void preRegister() {
        addItemHandler((ItemUseHandler) e -> {
            e.cancel();
            Block target = e.getClickedBlock().orElse(null);
            // No target, return
            if (target == null) return;
            ItemStack tool = e.getItem();
            Player p = e.getPlayer();
            if (!isTool(tool)) {
                // Not holding a valid tool, return
                p.sendMessage(Lang.getString("messages.unsupported-operation.cargo_node_quick_tool.invalid_tool"));
                return;
            }
            final Location bLoc = target.getLocation();
            runAtTargetRegion(bLoc, () -> {
                final SlimefunBlockData blockData = StorageCacheUtils.getBlock(bLoc);
                if (blockData == null || !blockData.getSfId().startsWith("CARGO_NODE_")) {
                    sendPlayerMessage(p, Lang.getString("messages.unsupported-operation.cargo_node_quick_tool.invalid_node"));
                    return;
                }

                if (p.isSneaking()) {
                    saveConfigToTool(p, tool, bLoc, blockData);
                } else {
                    pasteConfigFromTool(p, tool, bLoc, blockData);
                }
            });
        });
    }

    private void saveConfigToTool(
        @NotNull Player player,
        @NotNull ItemStack tool,
        @NotNull Location location,
        @NotNull SlimefunBlockData blockData) {
        final String sfId = blockData.getSfId();
        String listConfigString = null;
        if ("CARGO_NODE_INPUT".equals(sfId) || "CARGO_NODE_OUTPUT_ADVANCED".equals(sfId)) {
            final BlockMenu inv = blockData.getBlockMenu();
            if (inv == null) {
                return;
            }

            final YamlConfiguration itemConfig = new YamlConfiguration();
            for (int slot : listSlots) {
                final ItemStack itemInSlot = inv.getItemInSlot(slot);
                if (itemInSlot != null) {
                    itemConfig.set(String.valueOf(slot), itemInSlot);
                }
            }
            listConfigString = itemConfig.saveToString();
        } else if (!"CARGO_NODE_OUTPUT".equals(sfId)) {
            sendPlayerMessage(player, Lang.getString("messages.unsupported-operation.cargo_node_quick_tool.invalid_node"));
            return;
        }

        final String finalListConfigString = listConfigString;
        final String configJson = gson.toJson(blockData.getAllData());
        final SlimefunItem sf = SlimefunItem.getById(sfId);
        FoliaSupport.runPlayer(player, () -> {
            final ItemMeta meta = tool.getItemMeta();
            if (meta == null) {
                return;
            }
            final PersistentDataContainer container = meta.getPersistentDataContainer();
            if (finalListConfigString != null) {
                container.set(listKey, PersistentDataType.STRING, finalListConfigString);
            }
            container.set(cargoKey, PersistentDataType.STRING, sfId);
            container.set(configKey, PersistentDataType.STRING, configJson);

            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>(1);
            }
            if (lore.isEmpty()) {
                lore.add("");
            }
            if (sf != null) {
                lore.set(
                    lore.size() - 1,
                    String.format(
                        Lang.getString("messages.completed-operation.cargo_node_quick_tool.node_set"),
                        sf.getItemName()));
            }
            meta.setLore(lore);
            tool.setItemMeta(meta);
            player.sendMessage(Lang.getString("messages.completed-operation.cargo_node_quick_tool.config_saved"));
        });
    }

    private void pasteConfigFromTool(
        @NotNull Player player,
        @NotNull ItemStack tool,
        @NotNull Location location,
        @NotNull SlimefunBlockData blockData) {
        final ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return;
        }
        final PersistentDataContainer container = meta.getPersistentDataContainer();
        final String storedId = container.get(cargoKey, PersistentDataType.STRING);
        if (storedId == null) {
            sendPlayerMessage(player, Lang.getString("messages.unsupported-operation.cargo_node_quick_tool.no-config"));
            return;
        }
        if (!storedId.equalsIgnoreCase(blockData.getSfId())) {
            sendPlayerMessage(player, Lang.getString("messages.unsupported-operation.cargo_node_quick_tool.node-type-not-same"));
            return;
        }

        final BlockMenu inv = blockData.getBlockMenu();
        if (inv == null) {
            return;
        }

        final String sfId = blockData.getSfId();
        final String cfg = container.get(listKey, PersistentDataType.STRING);
        final String configJson = container.get(configKey, PersistentDataType.STRING);
        if ("CARGO_NODE_INPUT".equals(sfId) || "CARGO_NODE_OUTPUT_ADVANCED".equals(sfId)) {
            final YamlConfiguration itemConfig = new YamlConfiguration();
            try {
                if (cfg == null) {
                    return;
                }
                itemConfig.loadFromString(cfg);
            } catch (Exception ex) {
                Debug.trace(ex);
                return;
            }

            final Map<Integer, ItemStack> desiredItems = new LinkedHashMap<>();
            final Map<Integer, ItemStack> targetSnapshot = new LinkedHashMap<>();
            for (int slot : listSlots) {
                desiredItems.put(slot, itemConfig.getItemStack(String.valueOf(slot)));
                final ItemStack current = inv.getItemInSlot(slot);
                targetSnapshot.put(slot, current == null ? null : current.clone());
            }

            FoliaSupport.runPlayer(player, () -> {
                final List<ItemStack> missing = new ArrayList<>();
                final List<ItemStack> requiredItems = buildRequiredItems(desiredItems, targetSnapshot);
                for (ItemStack required : requiredItems) {
                    final int available = countAvailable(player.getInventory(), required);
                    if (available < required.getAmount()) {
                        missing.add(required.clone());
                    }
                }

                if (!missing.isEmpty()) {
                    player.sendMessage(Lang.getString(
                        "messages.unsupported-operation.cargo_node_quick_tool.not-enough-items"));
                    for (ItemStack item : missing) {
                        player.sendMessage(TextUtil.color("- &e" + ItemStackHelper.getDisplayName(item) + "x"
                            + item.getAmount()));
                    }
                    return;
                }

                for (ItemStack required : requiredItems) {
                    consumeFromInventory(player.getInventory(), required);
                }

                FoliaSupport.runRegion(location, () -> {
                    for (int slot : listSlots) {
                        inv.replaceExistingItem(slot, desiredItems.get(slot));
                    }
                    applyCargoNodeData(inv, blockData, location, configJson);
                    sendPlayerMessage(player,
                        Lang.getString("messages.completed-operation.cargo_node_quick_tool.pasted_config"));
                });
            });
            return;
        }

        if ("CARGO_NODE_OUTPUT".equals(sfId)) {
            applyCargoNodeData(inv, blockData, location, configJson);
            sendPlayerMessage(player,
                Lang.getString("messages.completed-operation.cargo_node_quick_tool.pasted_config"));
            return;
        }

        sendPlayerMessage(player, Lang.getString("messages.unsupported-operation.cargo_node_quick_tool.invalid_node"));
    }

    private void applyCargoNodeData(
        @NotNull BlockMenu inv,
        @NotNull SlimefunBlockData blockData,
        @NotNull Location location,
        @Nullable String configJson) {
        Map<String, String> config = gson.fromJson(
            configJson,
            new TypeToken<Map<String, String>>() {
            }.getType());
        if (config != null) {
            config.forEach(blockData::setData);
        }
        inv.getPreset().newInstance(inv, location);
    }

    private @NotNull List<ItemStack> buildRequiredItems(
        @NotNull Map<Integer, ItemStack> desiredItems,
        @NotNull Map<Integer, ItemStack> targetSnapshot) {
        final List<ItemStack> required = new ArrayList<>();
        for (int slot : listSlots) {
            final ItemStack desired = desiredItems.get(slot);
            if (desired == null) {
                continue;
            }
            final ItemStack existing = targetSnapshot.get(slot);
            int neededAmount = desired.getAmount();
            if (SlimefunUtils.isItemSimilar(existing, desired, true, false)) {
                neededAmount = Math.max(0, desired.getAmount() - existing.getAmount());
            }
            if (neededAmount <= 0) {
                continue;
            }
            mergeRequiredItem(required, StackUtils.getAsQuantity(desired, neededAmount));
        }
        return required;
    }

    private void mergeRequiredItem(@NotNull List<ItemStack> required, @NotNull ItemStack incoming) {
        for (ItemStack existing : required) {
            if (StackUtils.itemsMatch(existing, incoming, true, false)) {
                existing.setAmount(existing.getAmount() + incoming.getAmount());
                return;
            }
        }
        required.add(incoming);
    }

    private int countAvailable(@NotNull PlayerInventory inventory, @NotNull ItemStack template) {
        int available = 0;
        for (int i = 0; i <= 35; i++) {
            final ItemStack item = inventory.getItem(i);
            if (item != null && StackUtils.itemsMatch(item, template, true, false)) {
                available += item.getAmount();
            }
        }
        return available;
    }

    private void consumeFromInventory(@NotNull PlayerInventory inventory, @NotNull ItemStack template) {
        int remaining = template.getAmount();
        for (int i = 0; i <= 35 && remaining > 0; i++) {
            final ItemStack item = inventory.getItem(i);
            if (item == null || !StackUtils.itemsMatch(item, template, true, false)) {
                continue;
            }
            final int handled = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - handled);
            inventory.setItem(i, item.getAmount() > 0 ? item : null);
            remaining -= handled;
        }
    }

    private boolean isTool(@Nullable ItemStack tool) {
        if (tool != null && tool.getItemMeta() != null) {
            Slimefun sf = Slimefun.instance();
            if (sf != null) {
                NamespacedKey idKey = Keys.customNewKey(sf, "slimefun_item");
                PersistentDataContainer container = tool.getItemMeta().getPersistentDataContainer();
                if (container.has(idKey, PersistentDataType.STRING)) {
                    return getId().equalsIgnoreCase(container.get(idKey, PersistentDataType.STRING))
                        && (tool.getAmount() == 1);
                }
            }
        }
        return false;
    }
}
