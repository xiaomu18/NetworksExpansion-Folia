package com.ytdd9527.networksexpansion.utils;

import io.github.sefiraat.networks.Networks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class FoliaSupport {
    private FoliaSupport() {
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static void ensureFolia(@NotNull Plugin plugin) {
        if (!isFolia()) {
            throw new IllegalStateException(
                plugin.getName() + " now targets Folia 1.21+ only. Please run it on a Folia server.");
        }
    }

    public static void runGlobal(@NotNull Runnable runnable) {
        Networks.getInstance()
            .getServer()
            .getGlobalRegionScheduler()
            .run(Networks.getInstance(), task -> runnable.run());
    }

    public static @NotNull ScheduledTask runGlobalRepeating(
        long initialDelayTicks,
        long periodTicks,
        @NotNull Runnable runnable) {
        return Networks.getInstance()
            .getServer()
            .getGlobalRegionScheduler()
            .runAtFixedRate(Networks.getInstance(), task -> runnable.run(), initialDelayTicks, periodTicks);
    }

    public static @NotNull ScheduledTask runGlobalDelayed(long delayTicks, @NotNull Runnable runnable) {
        return Networks.getInstance()
            .getServer()
            .getGlobalRegionScheduler()
            .runDelayed(Networks.getInstance(), task -> runnable.run(), delayTicks);
    }

    public static void runRegion(@NotNull Location location, @NotNull Runnable runnable) {
        Networks.getInstance()
            .getServer()
            .getRegionScheduler()
            .execute(Networks.getInstance(), location, runnable);
    }

    public static @NotNull ScheduledTask runRegionRepeating(
        @NotNull Location location,
        long initialDelayTicks,
        long periodTicks,
        @NotNull Runnable runnable) {
        return Networks.getInstance()
            .getServer()
            .getRegionScheduler()
            .runAtFixedRate(Networks.getInstance(), location, task -> runnable.run(), initialDelayTicks, periodTicks);
    }

    public static @NotNull ScheduledTask runRegionDelayed(
        @NotNull Location location,
        long delayTicks,
        @NotNull Runnable runnable) {
        return Networks.getInstance()
            .getServer()
            .getRegionScheduler()
            .runDelayed(Networks.getInstance(), location, task -> runnable.run(), delayTicks);
    }

    public static void runEntity(@NotNull Entity entity, @NotNull Runnable runnable) {
        entity.getScheduler().run(Networks.getInstance(), task -> runnable.run(), null);
    }

    public static void runEntity(@NotNull Entity entity, @NotNull Runnable runnable, @Nullable Runnable retired) {
        entity.getScheduler().run(Networks.getInstance(), task -> runnable.run(), retired);
    }

    public static void runEntityDelayed(
        @NotNull Entity entity,
        long delayTicks,
        @NotNull Runnable runnable,
        @Nullable Runnable retired) {
        entity.getScheduler().runDelayed(Networks.getInstance(), task -> runnable.run(), retired, delayTicks);
    }

    public static void runPlayer(@NotNull Player player, @NotNull Runnable runnable) {
        runEntity(player, runnable, null);
    }

    public static void runAsync(@NotNull Runnable runnable) {
        Networks.getInstance()
            .getServer()
            .getAsyncScheduler()
            .runNow(Networks.getInstance(), task -> runnable.run());
    }

    public static @NotNull ScheduledTask runAsyncDelayed(long delay, @NotNull TimeUnit unit, @NotNull Runnable runnable) {
        return Networks.getInstance()
            .getServer()
            .getAsyncScheduler()
            .runDelayed(Networks.getInstance(), task -> runnable.run(), delay, unit);
    }

    public static @NotNull ScheduledTask runAsyncRepeating(
        long initialDelay,
        long period,
        @NotNull TimeUnit unit,
        @NotNull Runnable runnable) {
        return Networks.getInstance()
            .getServer()
            .getAsyncScheduler()
            .runAtFixedRate(Networks.getInstance(), task -> runnable.run(), initialDelay, period, unit);
    }

    public static <T> @NotNull CompletableFuture<T> supplyRegion(@NotNull Location location, @NotNull Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runRegion(location, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public static boolean isOwnedByCurrentRegion(@NotNull Location location) {
        World world = location.getWorld();
        return world != null && location.isWorldLoaded() && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
            && Networks.getInstance().getServer().isOwnedByCurrentRegion(location);
    }
}
