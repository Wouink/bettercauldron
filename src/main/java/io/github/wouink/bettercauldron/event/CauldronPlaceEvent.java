package io.github.wouink.bettercauldron.event;

import io.github.wouink.bettercauldron.BetterCauldron;
import net.minecraft.block.Blocks;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CauldronPlaceEvent {

	@SubscribeEvent
	public void onCauldronPlaced(BlockEvent.EntityPlaceEvent event) {
		if(event.getPlacedBlock().getBlock().is(Blocks.CAULDRON)) {
			event.getWorld().setBlock(event.getPos(), BetterCauldron.Cauldron_Block.defaultBlockState(), 3);
		}
	}
}
