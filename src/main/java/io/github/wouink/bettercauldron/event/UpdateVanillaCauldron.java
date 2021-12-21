package io.github.wouink.bettercauldron.event;

import io.github.wouink.bettercauldron.BetterCauldron;
import io.github.wouink.bettercauldron.block.tileentity.CauldronTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CauldronBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class UpdateVanillaCauldron {

	@SubscribeEvent
	public void onCauldronUsed(PlayerInteractEvent.RightClickBlock event) {
		BlockState state = event.getWorld().getBlockState(event.getPos());
		if(state.getBlock() == Blocks.CAULDRON) {
			int level = state.getValue(CauldronBlock.LEVEL);
			event.getWorld().setBlock(event.getPos(), BetterCauldron.Cauldron_Block.defaultBlockState(), 3);
			System.out.println("Updated Block");
			if(level > 0) {
				TileEntity tileEntity = event.getWorld().getBlockEntity(event.getPos());
				if(tileEntity instanceof CauldronTileEntity) {
					((CauldronTileEntity) tileEntity).setFluid(Fluids.WATER, level);
					System.out.println("Updated TileEntity");
					((CauldronTileEntity) tileEntity).handleUse(event.getPlayer(), event.getHand());
				}
			}
		}
	}
}
