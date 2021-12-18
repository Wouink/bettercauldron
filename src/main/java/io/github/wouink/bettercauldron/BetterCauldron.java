package io.github.wouink.bettercauldron;

import io.github.wouink.bettercauldron.block.Cauldron;
import io.github.wouink.bettercauldron.block.tileentity.CauldronTileEntity;
import io.github.wouink.bettercauldron.client.CauldronRenderer;
import io.github.wouink.bettercauldron.event.CauldronPlaceEvent;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BetterCauldron.MODID)
public class BetterCauldron {
	public static final String MODID = "bettercauldron";

	private static final Logger LOGGER = LogManager.getLogger();

	public static final Block Cauldron_Block = new Cauldron();

	public BetterCauldron() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		RegistryEvents.Tile_Entities.register(bus);
		bus.addListener(this::setup);
		bus.addListener(this::clientSetup);
	}

	public void setup(final FMLCommonSetupEvent event) {
		MinecraftForge.EVENT_BUS.register(new CauldronPlaceEvent());
		LOGGER.info("Better Cauldron Setup.");
	}

	public void clientSetup(final FMLClientSetupEvent event) {
		ClientRegistry.bindTileEntityRenderer(RegistryEvents.Cauldron_TileEntity.get(), CauldronRenderer::new);
		LOGGER.info("Better Cauldron Client Setup.");
	}

	@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents {
		public static final DeferredRegister<TileEntityType<?>> Tile_Entities = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MODID);
		public static final RegistryObject<TileEntityType<CauldronTileEntity>> Cauldron_TileEntity = Tile_Entities.register("cauldron", () -> TileEntityType.Builder.of(CauldronTileEntity::new, Cauldron_Block).build(null));

		@SubscribeEvent
		public static void onBlockRegistry(final RegistryEvent.Register<Block> event) {
			LOGGER.info("Better Cauldron Block Registry.");
			event.getRegistry().register(Cauldron_Block);
		}
	}
}
