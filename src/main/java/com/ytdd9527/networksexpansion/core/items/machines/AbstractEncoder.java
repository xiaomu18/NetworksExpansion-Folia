package com.ytdd9527.networksexpansion.core.items.machines;

import com.balugaq.netex.api.enums.CraftType;
import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.helpers.Icon;
import com.balugaq.netex.api.interfaces.CraftTyped;
import com.balugaq.netex.api.interfaces.RecipeCompletableWithGuide;
import com.balugaq.netex.utils.BlockMenuUtil;
import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEncoder extends NetworkObject implements CraftTyped, RecipeCompletableWithGuide {
    private static final int[] BACKGROUND = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 15, 17, 18, 20, 24, 25, 27, 28, 29, 33, 36, 37, 38, 39, 40, 41,
        42, 43, 44
    };
    private static final int[] RECIPE_SLOTS = new int[]{12, 13, 14, 21, 22, 23, 30, 31, 32};
    private static final int[] BLUEPRINT_BACK = new int[]{10, 28};
    private static final int BLANK_BLUEPRINT_SLOT = 19;
    private static final int ENCODE_SLOT = 16;
    private static final int OUTPUT_SLOT = 34;
    private static final int ITEM_TARGET_DESC_SLOT = 26;
    private static final int ITEM_TARGET_SLOT = 35;
    private static final int JEG_SLOT = 4;
    private static final int CHARGE_COST = 2000;
    private static final Map<Location, Boolean> PENDING_ENCODINGS = new ConcurrentHashMap<>();

    public AbstractEncoder(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.ENCODER);
        for (int recipeSlot : RECIPE_SLOTS) {
            this.getSlotsToDrop().add(recipeSlot);
        }
        this.getSlotsToDrop().add(BLANK_BLUEPRINT_SLOT);
        this.getSlotsToDrop().add(OUTPUT_SLOT);
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {

            @Override
            public void init() {
                drawBackground(BACKGROUND);
                drawBackground(Icon.BLUEPRINT_BACK_STACK, BLUEPRINT_BACK);

                addItem(ENCODE_SLOT, Icon.ENCODE_STACK, (player, i, itemStack, clickAction) -> false);
                addItem(ITEM_TARGET_DESC_SLOT, Icon.ITEM_TARGET_DESC_STACK, (player, i, itemStack, clickAction) -> false);
            }

            @Override
            public void newInstance(@NotNull BlockMenu menu, @NotNull Block b) {
                menu.addMenuClickHandler(ENCODE_SLOT, (player, s, itemStack, clickAction) -> {
                    int times = clickAction.isShiftClicked() ? 64 : 1;
                    tryEncode(player, menu, times);
                    return false;
                });
                addJEGButton(menu, JEG_SLOT);
                var fix = menu.getItemInSlot(ITEM_TARGET_SLOT);
                if (StackUtils.itemsMatch(fix, ChestMenuUtils.getBackground())) {
                    menu.replaceExistingItem(ITEM_TARGET_SLOT, null);
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
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_SLOT};
                }

                return new int[0];
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack itemStack) {
                if (flow == ItemTransportFlow.WITHDRAW) return new int[]{OUTPUT_SLOT};

                List<Integer> slots = new ArrayList<>();
                if (StackUtils.itemsMatch(itemStack, menu.getItemInSlot(BLANK_BLUEPRINT_SLOT))) {
                    slots.add(BLANK_BLUEPRINT_SLOT);
                }

                for (int slot : RECIPE_SLOTS) {
                    if (StackUtils.itemsMatch(itemStack, menu.getItemInSlot(slot))) {
                        slots.add(slot);
                    }
                }
                return slots.stream().mapToInt(Integer::intValue).toArray();
            }
        };
    }

    public void tryEncode(@NotNull Player player, @NotNull BlockMenu blockMenu, int times) {
        final Location menuLocation = blockMenu.getLocation();
        if (PENDING_ENCODINGS.putIfAbsent(menuLocation, Boolean.TRUE) != null) {
            return;
        }
        tryEncodeTimesAsync(player, blockMenu, times).whenComplete((ignored, throwable) ->
            FoliaSupport.runRegion(menuLocation, () -> PENDING_ENCODINGS.remove(menuLocation)));
    }

    private CompletableFuture<Void> tryEncodeTimesAsync(@NotNull Player player, @NotNull BlockMenu blockMenu, int times) {
        if (times <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        return tryEncodeOnceAsync(player, blockMenu).thenCompose(success -> {
            if (!Boolean.TRUE.equals(success)) {
                return CompletableFuture.completedFuture(null);
            }
            return tryEncodeTimesAsync(player, blockMenu, times - 1);
        });
    }

    public CompletableFuture<Boolean> tryEncodeOnceAsync(@NotNull Player player, @NotNull BlockMenu blockMenu) {
        final Location menuLocation = blockMenu.getLocation();
        final NodeDefinition definition = NetworkStorage.getNode(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            sendFeedback(menuLocation, FeedbackType.NO_NETWORK_FOUND);
            player.sendMessage(Lang.getString("messages.feedback.no_network_found"));
            return CompletableFuture.completedFuture(false);
        }

        final NetworkRoot root = definition.getNode().getRoot();
        final long networkCharge = root.getRootPower();

        if (networkCharge < CHARGE_COST) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.not_enough_power"));
            sendFeedback(menuLocation, FeedbackType.NOT_ENOUGH_POWER);
            return CompletableFuture.completedFuture(false);
        }

        ItemStack blueprint = blockMenu.getItemInSlot(BLANK_BLUEPRINT_SLOT);

        SlimefunItem sfi = SlimefunItem.getByItem(blueprint);
        if (sfi != null && sfi.isDisabled()) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.disabled_blueprint"));
            sendFeedback(menuLocation, FeedbackType.DISABLED_BLUEPRINT);
            return CompletableFuture.completedFuture(false);
        }

        if (!isValidBlueprint(sfi)) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.invalid_blueprint"));
            sendFeedback(menuLocation, FeedbackType.INVALID_BLUEPRINT);
            return CompletableFuture.completedFuture(false);
        }

        // Get the recipe input
        final ItemStack[] inputs = new ItemStack[RECIPE_SLOTS.length];
        int i = 0;
        for (int recipeSlot : RECIPE_SLOTS) {
            ItemStack stackInSlot = blockMenu.getItemInSlot(recipeSlot);
            if (stackInSlot != null) {
                inputs[i] = ItemStackUtil.getCleanItem(stackInSlot.clone());
            }
            i++;
        }

        ItemStack crafted = null;
        ItemStack[] inp = inputs.clone();
        for (int k = 0; k < inp.length; k++) {
            if (inp[k] != null) {
                inp[k] = ItemStackUtil.getCleanItem(inp[k]);
            }
        }

        ItemStack target = blockMenu.getItemInSlot(ITEM_TARGET_SLOT);
        if (target != null && target.getType() == Material.AIR) target = null;

        for (var entries : CraftType.entries()) {
            for (Map.Entry<ItemStack[], ItemStack> entry : entries) {
                if (testRecipe(inputs, entry.getKey())) {
                    crafted = ItemStackUtil.getCleanItem(entry.getValue().clone());
                    if (target != null && !StackUtils.itemsMatch(crafted, target)) {
                        continue;
                    }
                    inp = entry.getKey().clone();
                    for (int k = 0; k < inp.length; k++) {
                        if (inp[k] != null) {
                            inp[k] = ItemStackUtil.getCleanItem(inp[k]);
                        }
                    }
                    break;
                }
            }
        }

        if (crafted != null) {
            final SlimefunItem sfi2 = SlimefunItem.getByItem(crafted);
            if (sfi2 != null && sfi2.isDisabled()) {
                player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.disabled_output"));
                sendFeedback(menuLocation, FeedbackType.DISABLED_OUTPUT);
                return CompletableFuture.completedFuture(false);
            }
        }

        if (crafted == null && canTestVanillaRecipe(inputs)) {
            crafted = Bukkit.craftItem(inputs.clone(), player.getWorld(), player);
            for (int k = 0; k < RECIPE_SLOTS.length; k++) {
                if (inputs[k] != null) {
                    inp[k] = StackUtils.getAsQuantity(inputs[k], 1);
                }
            }
        }

        if (crafted == null || crafted.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.invalid_recipe"));
            sendFeedback(menuLocation, FeedbackType.INVALID_RECIPE);
            return CompletableFuture.completedFuture(false);
        }

        final ItemStack blueprintClone = StackUtils.getAsQuantity(blueprint, 1);
        final ItemStack craftedResult = crafted.clone();
        final ItemStack[] recipeInputs = inp;

        blueprintSetter(blueprintClone, recipeInputs, craftedResult);
        if (BlockMenuUtil.fits(blockMenu, blueprintClone, OUTPUT_SLOT)) {
            final CompletableFuture<ItemStack> recoverFuture = blueprint.getAmount() == 1
                ? root.getItemStack0Async(menuLocation, new ItemRequest(blueprint, blueprint.getMaxStackSize()))
                : CompletableFuture.completedFuture(null);
            return recoverFuture.thenCompose(recover -> FoliaSupport.supplyRegion(menuLocation, () -> {
                final ItemStack currentBlueprint = blockMenu.getItemInSlot(BLANK_BLUEPRINT_SLOT);
                if (currentBlueprint == null || currentBlueprint.getType() == Material.AIR) {
                    if (recover != null) {
                        root.addItemStack0Async(menuLocation, recover);
                    }
                    return false;
                }

                currentBlueprint.setAmount(currentBlueprint.getAmount() - 1);
                if (recover != null) {
                    BlockMenuUtil.pushItem(blockMenu, recover, BLANK_BLUEPRINT_SLOT);
                }
                int j = 0;
                for (int recipeSlot : RECIPE_SLOTS) {
                    ItemStack slotItem = blockMenu.getItemInSlot(recipeSlot);
                    if (slotItem != null) {
                        slotItem.setAmount(slotItem.getAmount() - recipeInputs[j].getAmount());
                    }
                    j++;
                }
                BlockMenuUtil.pushItem(blockMenu, blueprintClone, OUTPUT_SLOT);
                sendFeedback(menuLocation, FeedbackType.SUCCESS);
                return true;
            }).thenCompose(success -> {
                if (!Boolean.TRUE.equals(success)) {
                    return CompletableFuture.completedFuture(false);
                }

                return root.removeRootPowerAsync(CHARGE_COST).thenApply(ignored -> true);
            }));
        } else {
            player.sendMessage(Lang.getString("messages.unsupported-operation.encoder.output_full"));
            sendFeedback(menuLocation, FeedbackType.OUTPUT_FULL);
            return CompletableFuture.completedFuture(false);
        }
    }

    public void blueprintSetter(ItemStack itemStack, ItemStack[] inputs, ItemStack crafted) {
        craftType().blueprintSetter(itemStack, inputs, crafted);
    }

    public boolean isValidBlueprint(SlimefunItem item) {
        return craftType().isValidBlueprint(item);
    }

    public Set<Map.Entry<ItemStack[], ItemStack>> getRecipeEntries() {
        return craftType().getRecipeEntries();
    }

    public boolean testRecipe(ItemStack[] inputs, ItemStack[] recipe) {
        return craftType().testRecipe(inputs, recipe);
    }
    public boolean canTestVanillaRecipe(ItemStack[] inputs) {
        return false;
    }

    @Override
    @NotNull
    public SlimefunItem getSlimefunItem() {
        return this;
    }
}
