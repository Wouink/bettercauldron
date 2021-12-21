package io.github.wouink.bettercauldron.block.tileentity;

import io.github.wouink.bettercauldron.BetterCauldron;
import io.github.wouink.bettercauldron.recipe.CauldronRecipe;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.BannerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class CauldronTileEntity extends TileEntity {

	private Fluid fluid;
	private int fluidLevel;

	public CauldronTileEntity() {
		super(BetterCauldron.RegistryEvents.Cauldron_TileEntity.get());
		this.fluid = Fluids.EMPTY;
	}

	@Override
	public void load(BlockState state, CompoundNBT nbt) {
		super.load(state, nbt);
		this.fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(nbt.getString("Fluid")));
		this.fluidLevel = nbt.getInt("FluidLevel");
	}

	@Override
	public CompoundNBT save(CompoundNBT nbt) {
		super.save(nbt);
		nbt.putInt("FluidLevel", fluidLevel);
		nbt.putString("Fluid", fluid.getRegistryName().toString());
		return nbt;
	}

	public boolean isEmpty() {
		return this.fluid == Fluids.EMPTY || this.fluidLevel < 1;
	}

	public int getFluidLevel() {
		return this.fluidLevel;
	}

	public Fluid getFluid() {
		return fluid;
	}

	public void setFluid(Fluid f, int l) {
		this.fluid = f;
		this.fluidLevel = l;
	}

	private boolean incrementLevel() {
		if(fluidLevel < 3) {
			fluidLevel++;
			return true;
		}
		return false;
	}

	private boolean decrementLevel() {
		if(fluidLevel > 0) {
			fluidLevel--;
			if(fluidLevel == 0) fluid = Fluids.EMPTY;
			return true;
		}
		return false;
	}

	private void updateFluid() {
		if(fluidLevel <= 0) {
			fluidLevel = 0;
			fluid = Fluids.EMPTY;
		}
	}

	// replicates and improves the vanilla cauldron behavior
	public void handleUse(PlayerEntity playerEntity, Hand hand) {
		ItemStack stack = playerEntity.getItemInHand(hand);
		if(stack.getItem() instanceof BucketItem) {
			if(stack.getItem() == Items.BUCKET) {
				if(fluid != Fluids.EMPTY && fluidLevel == 3) {
					if(!playerEntity.abilities.instabuild) {
						if (stack.getCount() > 1) {
							playerEntity.addItem(new ItemStack(fluid.getBucket()));
						} else {
							playerEntity.setItemInHand(hand, new ItemStack(fluid.getBucket()));
						}
					}
					fluid = Fluids.EMPTY;
					fluidLevel = 0;
					level.playSound(playerEntity, worldPosition, fluid == Fluids.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
					playerEntity.awardStat(Stats.FILL_CAULDRON);
				}
			} else {
				if(isEmpty()) {
					fluid = ((BucketItem) stack.getItem()).getFluid();
					fluidLevel = 3;
					if(!playerEntity.abilities.instabuild) playerEntity.setItemInHand(hand, new ItemStack(Items.BUCKET));
					level.playSound(playerEntity, worldPosition, fluid == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);
					playerEntity.awardStat(Stats.USE_CAULDRON);
				}
			}
		} else if(stack.getItem() == Items.POTION) {
			if(PotionUtils.getPotion(stack) == Potions.WATER && (fluid == Fluids.WATER || fluid == Fluids.EMPTY)) {
				if(fluid == Fluids.EMPTY) fluid = Fluids.WATER;
				if(incrementLevel()) {
					if(!playerEntity.abilities.instabuild) {
						if (stack.getCount() == 1) playerEntity.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
						else playerEntity.addItem(new ItemStack(Items.GLASS_BOTTLE));
					}
					level.playSound(playerEntity, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);
					playerEntity.awardStat(Stats.USE_CAULDRON);
				}
			}
		} else if(stack.getItem() == Items.GLASS_BOTTLE) {
			if(fluid == Fluids.WATER && decrementLevel()) {
				if(!playerEntity.abilities.instabuild) {
					if (stack.getCount() == 1)
						playerEntity.setItemInHand(hand, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
					else playerEntity.addItem(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
				}
				level.playSound(playerEntity, worldPosition, SoundEvents.BOTTLE_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
				playerEntity.awardStat(Stats.USE_CAULDRON);
			}
		} else if(!handleHardcodedActions(stack, playerEntity, hand)) {
			CauldronRecipe recipe = CauldronRecipe.findRecipe(stack, fluid.getRegistryName(), fluidLevel);
			if(recipe != null) {
				if(!playerEntity.abilities.instabuild) {
					fluidLevel -= recipe.getConsumedLevel();
					updateFluid();
				}
				if(!recipe.appliesToStack()) {
					stack.setCount(stack.getCount() - 1);
					playerEntity.setItemInHand(hand, stack);
					for(ItemStack s : recipe.getResults()) {
						s.setCount(1);
						if(!playerEntity.addItem(s)) playerEntity.drop(s, false);
					}
				} else {
					for(ItemStack s : recipe.getResults()) s.setCount(stack.getCount());
					playerEntity.setItemInHand(hand, recipe.getResults()[0]);
					for(int i = 1; i < recipe.getResults().length; i++) {
						if(!playerEntity.addItem(recipe.getResults()[i])) playerEntity.drop(recipe.getResults()[i], false);
					}
				}
				if(playerEntity instanceof ServerPlayerEntity) ((ServerPlayerEntity) playerEntity).refreshContainer(playerEntity.inventoryMenu);
			}
		}
	}

	public boolean handleHardcodedActions(ItemStack stack, PlayerEntity playerEntity, Hand hand) {
		if(stack.getItem() instanceof IDyeableArmorItem && fluid == Fluids.WATER && fluidLevel > 0) {
			IDyeableArmorItem armorItem = (IDyeableArmorItem) stack.getItem();
			if(armorItem.hasCustomColor(stack)) {
				armorItem.clearColor(stack);
				if(!playerEntity.abilities.instabuild) decrementLevel();
				playerEntity.awardStat(Stats.CLEAN_ARMOR);
				playerEntity.setItemInHand(hand, stack);
				return true;
			}
		} else if(stack.getItem() instanceof BannerItem && fluid == Fluids.WATER && fluidLevel > 0) {
			ItemStack newBanner = stack.copy();
			BannerTileEntity.removeLastPattern(newBanner);
			playerEntity.awardStat(Stats.CLEAN_BANNER);
			if(!playerEntity.abilities.instabuild) {
				stack.shrink(1);
				decrementLevel();
			}
			if(stack.isEmpty()) playerEntity.setItemInHand(hand, newBanner);
			else if(!playerEntity.addItem(newBanner)) playerEntity.drop(newBanner, false);
			return true;
		} else if(stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock && ((BlockItem) stack.getItem()).getBlock() != Blocks.SHULKER_BOX && fluid == Fluids.WATER && fluidLevel > 0) {
			ItemStack newShulkerBox = new ItemStack(Blocks.SHULKER_BOX);
			if(stack.hasTag()) newShulkerBox.setTag(stack.getTag().copy());
			playerEntity.setItemInHand(hand, newShulkerBox);
			if(!playerEntity.abilities.instabuild) decrementLevel();
			playerEntity.awardStat(Stats.CLEAN_SHULKER_BOX);
			return true;
		}
		return false;
	}

	private void spawnItemInside(ItemStack stack) {
		level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, stack));
	}

	public void entityInside(Entity entity) {
		if(entity instanceof ItemEntity) {
			ItemEntity item = ((ItemEntity) entity);
			CauldronRecipe recipe = CauldronRecipe.findRecipe(item.getItem(), fluid.getRegistryName(), fluidLevel);
			if(recipe != null) {
				if(!recipe.appliesToStack()) {
					int quantityCrafted = Math.min(item.getItem().getCount(), Math.floorDiv(fluidLevel, recipe.getConsumedLevel()));
					if (quantityCrafted > 0) {
						fluidLevel -= quantityCrafted * recipe.getConsumedLevel();
						updateFluid();
						ItemStack itemStack = item.getItem();
						itemStack.setCount(itemStack.getCount() - quantityCrafted);
						item.setItem(itemStack);
						for(ItemStack s : recipe.getResults()) {
							s.setCount(quantityCrafted);
							spawnItemInside(s);
						}
					}
				} else {
					fluidLevel -= recipe.getConsumedLevel();
					updateFluid();
					int index = 0;
					for(ItemStack s : recipe.getResults()) {
						s.setCount(item.getItem().getCount());
						if(index > 0) spawnItemInside(s);
						index++;
					}
					item.setItem(recipe.getResults()[0]);
				}
			}
		} else if(this.fluid == Fluids.WATER) {
			if(entity.isOnFire() && !entity.fireImmune()) {
				entity.clearFire();
				this.fluidLevel--;
				updateFluid();
			}
		}
	}

	public void fillFromRain() {
		if(fluid == Fluids.WATER) incrementLevel();
	}

	// sync between client/server
	// otherwise, when loading the TileEntity on the server side, no data is sent to the client for rendering

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = new CompoundNBT();
		save(nbt);
		return new SUpdateTileEntityPacket(this.worldPosition, 1234, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		BlockState state = this.getBlockState();
		load(state, pkt.getTag());
	}

	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT nbt = new CompoundNBT();
		save(nbt);
		return nbt;
	}

	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT tag) {
		load(state, tag);
	}
}
