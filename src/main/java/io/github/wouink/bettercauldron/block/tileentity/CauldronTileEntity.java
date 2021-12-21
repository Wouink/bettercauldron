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
import net.minecraft.world.World;
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

	private void addLevel(int amount) {
		fluidLevel += amount;
		if(fluidLevel > 3) fluidLevel = 3;
		else if(fluidLevel <= 0) {
			fluidLevel = 0;
			fluid = Fluids.EMPTY;
		}
	}

	private static boolean isWatterBottle(ItemStack stack) {
		return stack.getItem() == Items.POTION && PotionUtils.getPotion(stack) == Potions.WATER;
	}

	private static void updatePlayerInventory(PlayerEntity playerEntity) {
		if(playerEntity instanceof ServerPlayerEntity) ((ServerPlayerEntity) playerEntity).refreshContainer(playerEntity.inventoryMenu);
	}

	// replicates and improves the vanilla cauldron behavior
	public void handleUse(PlayerEntity playerEntity, Hand hand) {
		ItemStack stack = playerEntity.getItemInHand(hand);
		if(stack.getItem() instanceof BucketItem) {
			// fill or empty from bucket
			if(stack.getItem() == Items.BUCKET) {
				if(fluid != Fluids.EMPTY && fluidLevel == 3) {
					if(!level.isClientSide() && !playerEntity.abilities.instabuild) {
						if (stack.getCount() > 1) {
							playerEntity.addItem(new ItemStack(fluid.getBucket()));
						} else {
							playerEntity.setItemInHand(hand, new ItemStack(fluid.getBucket()));
						}
						playerEntity.awardStat(Stats.FILL_CAULDRON);
					}
					fluid = Fluids.EMPTY;
					fluidLevel = 0;
					level.playSound(playerEntity, worldPosition, fluid == Fluids.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			} else {
				if(isEmpty()) {
					fluid = ((BucketItem) stack.getItem()).getFluid();
					fluidLevel = 3;
					if(!level.isClientSide() && !playerEntity.abilities.instabuild){
						playerEntity.setItemInHand(hand, new ItemStack(Items.BUCKET));
						playerEntity.awardStat(Stats.USE_CAULDRON);
					}
					level.playSound(playerEntity, worldPosition, fluid == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
		} else if(isWatterBottle(stack) && (fluid == Fluids.WATER || fluid == Fluids.EMPTY)) {
			// fill from watter bottle
			if(fluid == Fluids.EMPTY) fluid = Fluids.WATER;
			if(fluidLevel < 3) {
				addLevel(1);
				if(!level.isClientSide() && !playerEntity.abilities.instabuild) {
					ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
					if (stack.getCount() == 1) playerEntity.setItemInHand(hand, glassBottle);
					else if(!playerEntity.addItem(glassBottle)) playerEntity.drop(glassBottle, false);
					else updatePlayerInventory(playerEntity);
					playerEntity.awardStat(Stats.USE_CAULDRON);
				}
				level.playSound(playerEntity, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
		} else if(!handleHardcodedVanillaActions(stack, playerEntity, hand)) {
			CauldronRecipe recipe = CauldronRecipe.findRecipe(stack, fluid.getRegistryName(), fluidLevel);
			if(recipe != null) {
				if(!playerEntity.abilities.instabuild) {
					addLevel(-recipe.getConsumedLevel());
				}
				if(!level.isClientSide()) {
					if (!recipe.appliesToStack()) {
						ItemStack[] results = recipe.getResults();
						for(ItemStack s : results) s.setCount(1);
						stack.shrink(1);
						if(stack.isEmpty()) playerEntity.setItemInHand(hand, results[0]);
						else if(!playerEntity.addItem(results[0])) playerEntity.drop(results[0], false);
						else updatePlayerInventory(playerEntity);

						for(int i = 1; i < results.length; i++) {
							if(!playerEntity.addItem(results[i])) playerEntity.drop(results[i], false);
						}
					} else {
						ItemStack[] results = recipe.getResults();
						for(ItemStack s : results) s.setCount(stack.getCount());
						playerEntity.setItemInHand(hand, results[0]);
						for(int i = 1; i < results.length; i++) {
							if(!playerEntity.addItem(results[i])) playerEntity.drop(results[i], false);
						}
					}
					playerEntity.awardStat(Stats.USE_CAULDRON);
				}
			}
		}
	}

	public boolean handleHardcodedVanillaActions(ItemStack stack, PlayerEntity playerEntity, Hand hand) {
		if(fluid == Fluids.WATER && fluidLevel > 0) {
			// vanilla actions only concern water
			if(stack.getItem() == Items.GLASS_BOTTLE) {
				// empty from bottle
				addLevel(-1);
				if(!level.isClientSide() && !playerEntity.abilities.instabuild) {
					stack.shrink(1);
					ItemStack waterBottle = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
					if(stack.isEmpty()) playerEntity.setItemInHand(hand, waterBottle);
					else if(!playerEntity.addItem(waterBottle)) playerEntity.drop(waterBottle, false);
					else updatePlayerInventory(playerEntity);
					playerEntity.awardStat(Stats.USE_CAULDRON);
				}
				level.playSound(playerEntity, worldPosition, SoundEvents.BOTTLE_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
				return true;
			} else if(stack.getItem() instanceof IDyeableArmorItem) {
				// wash armor
				IDyeableArmorItem armorItem = (IDyeableArmorItem) stack.getItem();
				if(armorItem.hasCustomColor(stack)) {
					if(!playerEntity.abilities.instabuild) addLevel(-1);
					if(!level.isClientSide()) {
						armorItem.clearColor(stack);
						playerEntity.awardStat(Stats.CLEAN_ARMOR);
					}
				}
				return true;
			} else if(stack.getItem() instanceof BannerItem) {
				// undo latest banner pattern
				if(BannerTileEntity.getPatternCount(stack) > 0) {
					if(!playerEntity.abilities.instabuild) addLevel(-1);
					if(!level.isClientSide()) {
						ItemStack newBanner = stack.copy();
						stack.setCount(1);
						BannerTileEntity.removeLastPattern(newBanner);
						if(!playerEntity.abilities.instabuild) {
							playerEntity.awardStat(Stats.CLEAN_BANNER);
							stack.shrink(1);
						}

						if(stack.isEmpty()) playerEntity.setItemInHand(hand, newBanner);
						else if(!playerEntity.addItem(newBanner)) playerEntity.drop(newBanner, false);
						else updatePlayerInventory(playerEntity);
					}
				}
				return true;
			} else if(stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock && ((BlockItem) stack.getItem()).getBlock() != Blocks.SHULKER_BOX) {
				// wash colored shulker box
				if(!playerEntity.abilities.instabuild){
					addLevel(-1);
				}
				if(!level.isClientSide()) {
					ItemStack newShulkerBox = new ItemStack(Blocks.SHULKER_BOX);
					if(stack.hasTag()) newShulkerBox.setTag(stack.getTag().copy());
					playerEntity.setItemInHand(hand, newShulkerBox);
					if(!playerEntity.abilities.instabuild) playerEntity.awardStat(Stats.CLEAN_SHULKER_BOX);
				}
				return true;
			}
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
						addLevel(-quantityCrafted * recipe.getConsumedLevel());
						ItemStack itemStack = item.getItem();
						itemStack.shrink(quantityCrafted);
						item.setItem(itemStack);
						for(ItemStack s : recipe.getResults()) {
							s.setCount(quantityCrafted);
							spawnItemInside(s);
						}
					}
				} else {
					addLevel(-recipe.getConsumedLevel());
					int index = 0;
					for(ItemStack s : recipe.getResults()) {
						s.setCount(item.getItem().getCount());
						if(index == 0) item.setItem(s);
						else spawnItemInside(s);
						index++;
					}
					item.setItem(recipe.getResults()[0]);
				}
			}
		} else if(this.fluid == Fluids.WATER) {
			if(entity.isOnFire() && !entity.fireImmune()) {
				entity.clearFire();
				addLevel(-1);
			}
		}
	}

	public void fillFromRain() {
		if(fluid == Fluids.EMPTY) fluid = Fluids.WATER;
		if(fluid == Fluids.WATER) addLevel(1);
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
