package io.github.wouink.bettercauldron.block;

import io.github.wouink.bettercauldron.BetterCauldron;
import io.github.wouink.bettercauldron.block.tileentity.CauldronTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class Cauldron extends Block {
	private static final VoxelShape INSIDE = box(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D);
	protected static final VoxelShape SHAPE = VoxelShapes.join(VoxelShapes.block(), VoxelShapes.or(box(0.0D, 0.0D, 4.0D, 16.0D, 3.0D, 12.0D), box(4.0D, 0.0D, 0.0D, 12.0D, 3.0D, 16.0D), box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D), INSIDE), IBooleanFunction.ONLY_FIRST);

	public Cauldron() {
		super(Properties.copy(Blocks.CAULDRON));
		setRegistryName(BetterCauldron.MODID, "cauldron");
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext ctx) {
		return SHAPE;
	}

	@Override
	public VoxelShape getInteractionShape(BlockState state, IBlockReader world, BlockPos pos) {
		return INSIDE;
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new CauldronTileEntity();
	}

	@Override
	public void handleRain(World world, BlockPos pos) {
		if(world.random.nextInt(20) == 1) {
			float biomeTemp = world.getBiome(pos).getTemperature(pos);
			if(biomeTemp >= .15f) {
				TileEntity tileEntity = world.getBlockEntity(pos);
				if(tileEntity instanceof CauldronTileEntity) {
					((CauldronTileEntity) tileEntity).fillFromRain();
				}
			}
		}
	}

	@Override
	public void entityInside(BlockState state, World world, BlockPos pos, Entity entity) {
		TileEntity tileEntity = world.getBlockEntity(pos);
		if(tileEntity instanceof CauldronTileEntity) {
			((CauldronTileEntity) tileEntity).entityInside(entity);
		}
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, Hand hand, BlockRayTraceResult result) {
		((CauldronTileEntity) world.getBlockEntity(pos)).handleUse(playerEntity, hand);
		return ActionResultType.SUCCESS;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos) {
		TileEntity tileEntity = world.getBlockEntity(pos);
		if(tileEntity instanceof CauldronTileEntity) {
			return ((CauldronTileEntity) tileEntity).getFluidLevel();
		}
		return 0;
	}

	@Override
	public ItemStack getCloneItemStack(IBlockReader world, BlockPos pos, BlockState state) {
		return new ItemStack(Items.CAULDRON);
	}
}
