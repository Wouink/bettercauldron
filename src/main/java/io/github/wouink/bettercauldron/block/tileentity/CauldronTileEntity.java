package io.github.wouink.bettercauldron.block.tileentity;

import io.github.wouink.bettercauldron.BetterCauldron;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
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

	// replicates and improves the vanilla cauldron behavior
	// cleaning actions are hardcoded in vanilla... maybe I'll move them to a recipe
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
		} else if(stack.getItem() instanceof IDyeableArmorItem && fluid == Fluids.WATER && fluidLevel > 0) {
			IDyeableArmorItem armorItem = (IDyeableArmorItem) stack.getItem();
			if(armorItem.hasCustomColor(stack)) {
				armorItem.clearColor(stack);
				if(!playerEntity.abilities.instabuild) decrementLevel();
				playerEntity.awardStat(Stats.CLEAN_ARMOR);
				playerEntity.setItemInHand(hand, stack);
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
		} else if(stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock && ((BlockItem) stack.getItem()).getBlock() != Blocks.SHULKER_BOX && fluid == Fluids.WATER && fluidLevel > 0) {
			ItemStack newShulkerBox = new ItemStack(Blocks.SHULKER_BOX);
			if(stack.hasTag()) newShulkerBox.setTag(stack.getTag().copy());
			playerEntity.setItemInHand(hand, newShulkerBox);
			if(!playerEntity.abilities.instabuild) decrementLevel();
			playerEntity.awardStat(Stats.CLEAN_SHULKER_BOX);
		}
		else {
			playerEntity.setItemInHand(hand, useRecipe(stack));
		}
	}

	public void entityInside(Entity entity) {
		if(entity instanceof ItemEntity) {
			ItemStack stack = ((ItemEntity) entity).getItem();
			ItemStack result = soakRecipe(stack);
			if(!stack.sameItem(result)) ((ItemEntity) entity).setItem(result);
		} else if(this.fluid == Fluids.WATER) {
			if(entity.isOnFire() && !entity.fireImmune()) {
				entity.clearFire();
				this.fluidLevel--;
				if (this.fluidLevel == 0) this.fluid = Fluids.EMPTY;
			}
		}
	}

	public void fillFromRain() {
		if(fluid == Fluids.WATER) incrementLevel();
	}

	public ItemStack soakRecipe(ItemStack dropped) {
		// just an example recipe for now
		// will be a real recipe type in the future
		if(dropped.getItem() == Items.DIRT) return new ItemStack(Items.DIAMOND, dropped.getCount());
		return dropped;
	}

	public ItemStack useRecipe(ItemStack used) {
		return used;
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
