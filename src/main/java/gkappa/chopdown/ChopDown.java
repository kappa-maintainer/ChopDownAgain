package gkappa.chopdown;

import gkappa.chopdown.command.CDCommand;
import gkappa.chopdown.config.OptionsHolder;
import gkappa.chopdown.config.TreeConfiguration;
import gkappa.chopdown.tree.Tree;
import net.minecraft.block.Block;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.commons.lang3.ArrayUtils;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("chopdown")
public class ChopDown
{
    public static final Logger LOGGER = LogManager.getLogger("chopdown");
    ExecutorService executor;
    // Directly reference a log4j logger.
    public static LinkedList<Tree> FallingTrees = new LinkedList<Tree>();
    public ChopDown() throws Exception {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OptionsHolder.COMMON_SPEC, "chopdown.toml");
        OptionsHolder.Common.postConfig();
    }

    @SubscribeEvent
    public void registerCommand(RegisterCommandsEvent event) {
        CDCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {

        IWorld world = event.getWorld();
        BlockPos pos = event.getPos();

        if (!Tree.isWood(pos, world)
                || !OptionsHolder.COMMON.allowedPlayers.get().contains(event.getPlayer().getClass().getName())) {
            return;
        }
        event.getPlayer().getMainHandItem();
        if (OptionsHolder.Common.MatchesTool(Tree.stackName(event.getPlayer().getMainHandItem()))) {
            return;
        }
        TreeConfiguration config = Tree.findConfig((World) world, pos);
        BlockPos playerStanding = event.getPlayer().blockPosition();
        if (config == null || !Tree.isTrunk(pos, (World) world, config) || !Tree.isWood(pos.offset(0, 1, 0), world)
                || (playerStanding.getX() == 0 && playerStanding.getZ() == 0)) {
            return;
        }

        // Check to see if this player has already started a tree chop event.
        for (Tree tree : FallingTrees) {
            if (tree.player == event.getPlayer()) {
                event.getPlayer().sendMessage(new StringTextComponent("Still chopping down the last tree"), event.getPlayer().getUUID());
                event.setCanceled(true);
                return;
            }
        }
        //Initialise the tree and add it to the list, get the executor to start chopping it down;;
        Tree tree;
        try {
            tree = new Tree(pos, (ServerWorld) world, event.getPlayer());
            FallingTrees.add(tree);
            executor.submit(tree);
        } catch (Exception e) {
            event.getPlayer().sendMessage(new StringTextComponent("Can't find a tree configuration for this log."), event.getPlayer().getUUID());
        }

    }

    static int tick = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        try {
            tick++;
            if (tick % 4 == 0) {
                tick = 0;
                for (Tree tree : FallingTrees) {
                    if (tree.finishedCalculation) {
                        if (tree.dropBlocks()) {
                            FallingTrees.remove(tree);
                        }
                    }
                    if (tree.failedToBuild) {
                        FallingTrees.remove(tree);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Error while continuing to chop trees");
        }
    }

    @SubscribeEvent
    public void clickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        if (OptionsHolder.Common.getPlayerConfig(event.getPlayer().getUUID()).showBlockName) {
            World world = event.getWorld();
            BlockPos pos = event.getPos();
            UUID uuid = event.getPlayer().getUUID();
            event.getPlayer().sendMessage(new StringTextComponent("Block:" + Tree.blockName(pos, world)), uuid);
            event.getPlayer().getMainHandItem();
            event.getPlayer().sendMessage(new StringTextComponent(
                    "Tool:" + Tree.stackName(event.getPlayer().getMainHandItem())), uuid);
            event.getPlayer().sendMessage(
                    new StringTextComponent("Player Class:" + event.getPlayer().getClass().getName()), uuid);
        }
    }

    private void setup(final FMLCommonSetupEvent event)
    {
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
    }

    private void processIMC(final InterModProcessEvent event)
    {
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        executor = Executors.newFixedThreadPool(2);
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
        }
    }
}
