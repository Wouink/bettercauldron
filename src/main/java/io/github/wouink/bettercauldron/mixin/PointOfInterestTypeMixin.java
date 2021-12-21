package io.github.wouink.bettercauldron.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.village.PointOfInterestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(PointOfInterestType.class)
public interface PointOfInterestTypeMixin {

	@Accessor("LEATHERWORKER")
	static PointOfInterestType getLeatherWorker() {
		throw new AssertionError();
	}

	@Accessor
	Set<BlockState> getMatchingStates();

	@Accessor
	void setMatchingStates(Set<BlockState> states);
}
