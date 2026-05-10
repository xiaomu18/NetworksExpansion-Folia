package io.github.sefiraat.networks.network.stackcaches;

import io.github.sefiraat.networks.network.barrel.BarrelCore;
import io.github.sefiraat.networks.network.barrel.BarrelType;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@Setter
public abstract class BarrelIdentity extends ItemStackCache implements BarrelCore {

    private Location location;
    private long amount;
    private long limit;
    private BarrelType type;

    @ParametersAreNonnullByDefault
    protected BarrelIdentity(Location location, @Nullable ItemStack itemStack, long amount, long limit, BarrelType type) {
        super(itemStack);
        this.location = location;
        this.amount = amount;
        this.limit = limit;
        this.type = type;
    }

    protected final boolean canDirectlyAccess() {
        return this.location.getWorld() == null || FoliaSupport.isOwnedByCurrentRegion(this.location);
    }

    protected final void requireDirectAccess() {
        if (!canDirectlyAccess()) {
            throw new IllegalStateException("Cross-region barrel access at " + this.location);
        }
    }
}
