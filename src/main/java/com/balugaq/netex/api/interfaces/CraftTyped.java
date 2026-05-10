package com.balugaq.netex.api.interfaces;

import com.balugaq.netex.api.enums.CraftType;
import org.jspecify.annotations.NullMarked;

/**
 * @author balugaq
 */
@NullMarked
public interface CraftTyped {
    default CraftType craftType() {
        return CraftType.CRAFTING;
    }
}
