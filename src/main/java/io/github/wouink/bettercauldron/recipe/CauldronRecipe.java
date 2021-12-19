package io.github.wouink.bettercauldron.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.wouink.bettercauldron.BetterCauldron;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;

public class CauldronRecipe implements IRecipe<IInventory> {
	public static final IRecipeSerializer<CauldronRecipe> SERIALIZER = new Serializer();

	private final Ingredient ingredient;
	private final ItemStack[] results;
	private final ResourceLocation fluid;
	private final int requiresLevel;
	private final int consumesLevel;
	private final ResourceLocation recipeLoc;

	public CauldronRecipe(ResourceLocation loc, Ingredient ingredient, ItemStack[] results, ResourceLocation fluid, int requiresLevel, int consumesLevel) {
		this.recipeLoc = loc;
		this.ingredient = ingredient;
		this.results = results;
		this.fluid = fluid;
		this.requiresLevel = requiresLevel;
		this.consumesLevel = consumesLevel;
	}

	public int getRequiredLevel() {
		return requiresLevel;
	}

	public int getConsumedLevel() {
		return consumesLevel;
	}

	public ItemStack[] getResults() {
		return results;
	}

	public boolean matches(ItemStack stack, ResourceLocation fluid, int level) {
		return this.ingredient.test(stack) && this.fluid.equals(fluid) && this.requiresLevel <= level;
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public IRecipeType<?> getType() {
		return BetterCauldron.RegistryEvents.Cauldron_Recipe;
	}

	@Override
	public boolean matches(IInventory inv, World world) {
		// not used
		return false;
	}

	@Override
	public ItemStack assemble(IInventory inv) {
		// not used
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		// not used
		return false;
	}

	@Override
	public ItemStack getResultItem() {
		// not used
		return ItemStack.EMPTY;
	}

	@Override
	public ResourceLocation getId() {
		// not used
		return this.recipeLoc;
	}

	public static CauldronRecipe findRecipe(ItemStack input, ResourceLocation fluid, int level) {
		RecipeManager manager = ServerLifecycleHooks.getCurrentServer().getRecipeManager();
		for(final CauldronRecipe recipe : manager.getAllRecipesFor(BetterCauldron.RegistryEvents.Cauldron_Recipe)) {
			if(recipe.matches(input, fluid, level)) return recipe;
		}
		return null;
	}

	public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<CauldronRecipe> {
		public static ItemStack readItemStack(JsonElement json, boolean acceptsNBT) {
			if(json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
				final String identifier = json.getAsString();
				final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(identifier));
				if(item != null) return new ItemStack(item);
				else throw new JsonSyntaxException(String.format("Could not find item with id %s", identifier));
			} else if(json.isJsonObject()) return CraftingHelper.getItemStack(json.getAsJsonObject(), acceptsNBT);
			else throw new JsonSyntaxException("Could not read ItemStack from JSON Element. Must be String or Object.");
		}

		public static List<ItemStack> readItemStacks(JsonElement json, boolean acceptsNBT) {
			final List<ItemStack> items = NonNullList.create();
			if(json.isJsonArray()) {
				for(final JsonElement element : json.getAsJsonArray()) items.add(readItemStack(element, acceptsNBT));
			} else items.add(readItemStack(json, acceptsNBT));
			return items;
		}

		public static ItemStack[] readItemStackArray(PacketBuffer packetBuffer) {
			final ItemStack[] items = new ItemStack[packetBuffer.readInt()];
			for(int i = 0; i < items.length; i++) items[i] = packetBuffer.readItem();
			return items;
		}

		public static void writeItemStackArray(PacketBuffer packetBuffer, ItemStack[] items) {
			packetBuffer.writeInt(items.length);
			for(final ItemStack stack : items) {
				packetBuffer.writeItem(stack);
			}
		}

		@Override
		public CauldronRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			final Ingredient input = Ingredient.fromJson(json.get("input"));
			final ItemStack[] outputs = json.has("result") ? readItemStacks(json.get("result"), true).toArray(new ItemStack[0]) : new ItemStack[0];
			final ResourceLocation fluid = new ResourceLocation(JSONUtils.getAsString(json, "fluid", "bettercauldron:no_fluid"));
			final int requiredLevel = JSONUtils.getAsInt(json, "requires_level", 1);
			final int consumedLevel = JSONUtils.getAsInt(json, "consumes_level", requiredLevel);
			return new CauldronRecipe(recipeId, input, outputs, fluid, requiredLevel, consumedLevel);
		}

		@Nullable
		@Override
		public CauldronRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer packetBuffer) {
			System.out.println("fromNetwork: " + recipeId.toString());
			final Ingredient input = Ingredient.fromNetwork(packetBuffer);
			final ItemStack[] outputs = readItemStackArray(packetBuffer);
			final ResourceLocation fluid = packetBuffer.readResourceLocation();
			final int requiredLevel = packetBuffer.readInt();
			final int consumedLevel = packetBuffer.readInt();
			return new CauldronRecipe(recipeId, input, outputs, fluid, requiredLevel, consumedLevel);
		}

		@Override
		public void toNetwork(PacketBuffer packetBuffer, CauldronRecipe recipe) {
			recipe.ingredient.toNetwork(packetBuffer);
			writeItemStackArray(packetBuffer, recipe.results);
			packetBuffer.writeResourceLocation(recipe.fluid);
			packetBuffer.writeInt(recipe.requiresLevel);
			packetBuffer.writeInt(recipe.consumesLevel);
		}
	}
}