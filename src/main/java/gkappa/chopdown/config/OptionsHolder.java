package gkappa.chopdown.config;

import com.google.gson.Gson;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class OptionsHolder
{

    public static class Common
    {
        public static PersonalConfig getPlayerConfig(UUID player) {
            PersonalConfig playerConfig;
            if (playerConfigs.containsKey(player)) {
                playerConfig = playerConfigs.get(player);
            } else {
                playerConfig = new PersonalConfig();
                playerConfigs.put(player, playerConfig);
            }
            return playerConfig;

        }
        public static boolean MatchesTool(String name) {
            for (String tool : COMMON.ignoreTools.get()) {
                if (tool.equals(name) || name.matches(tool)) {
                    return true;
                }
            }
            return false;
        }
        public static String CATEGORY = "General";
        public static String MOD_CATEGORY = "Mod Compatibility";

        public final ForgeConfigSpec.ConfigValue<Boolean> breakLeaves;
        public final ForgeConfigSpec.ConfigValue<Integer> maxDropsPerTickPerTree;
        public final ForgeConfigSpec.ConfigValue<Integer> maxFallingBlockBeforeManualMove;
        public final ForgeConfigSpec.ConfigValue<String[]> allowedPlayers;
        public final ForgeConfigSpec.ConfigValue<String[]> ignoreTools;

        public static HashMap<UUID, PersonalConfig> playerConfigs = new HashMap<UUID, PersonalConfig>();
        public static TreeConfiguration[] treeConfigurations;

        public static String[] leaves;
        public static String[] logs;
        public final ForgeConfigSpec.ConfigValue<String[]> sharedLeaves;

        public static ModTreeConfigurations mods = new ModTreeConfigurations();



        public Common(ForgeConfigSpec.Builder builder) throws Exception {
            builder.push(CATEGORY);
            this.maxDropsPerTickPerTree = builder.comment("Maximum number of blocks to drop per tick for each tree thats falling")
                    .defineInRange("maxDropsPerTickPerTree", 150, 1, 60);
            this.maxFallingBlockBeforeManualMove = builder.comment("If the total blocks in the tree is above this amount instead of creating entities then it will place the blocks directly on the floor, this is for really large trees like the natura Redwood")
                    .defineInRange("maxFallingBlockBeforeManualMove", 1500, 1, 1000000);
            this.breakLeaves = builder.comment("When you chop a tree down the leaves all fall off and do their drops instead of falling with the tree, this can be better as a) less load and b)The falling of trees gets less messy, you still need to chop the logs but the leaves don't get in the way")
                    .define("breakLeaves", false);
            this.sharedLeaves = builder.comment("Not necessarily leaves, objects that if seemingly attached to the tree should fall down with it, such as beehives")
                    .define("sharedLeaves", new String[] { "harvestcraft:beehive:0" });
            this.allowedPlayers = builder.comment("List of all the player classes allowed to chop down trees, used to distinguish fake and real players")
                    .define("allowedPlayers", new String[] { ServerPlayerEntity.class.getName(),
                            "micdoodle8.mods.galacticraft.core.entities.player.GCServerPlayerEntity",
                            "clayborn.universalremote.hooks.entity.HookedServerPlayerEntity" });
            this.ignoreTools = builder.comment("List of tools to ignore chop down on, such as tinkers lumberaxe, any tool that veinmines or similar should be ignored for chopdown")
                            .define("ignoreTools", new String[] { "tconstruct:lumberaxe:.*"});
            builder.push(MOD_CATEGORY);
            List<String> activeMods = new ArrayList<String>();
            if (builder.comment("Vanilla").define("Vanilla", true).get())
                activeMods.add("Vanilla");
            String[] availableMods = {
                    "AbyssalCraft",
                    "AetherLegacy",
                    "BetterWithAddons",
                    "BiomesOPlenty",
                    "Cuisine",
                    "DefiledLands",
                    "ExtraTrees",
                    "Forestry",
                    "IndustrialCraft2",
                    "IntegratedDynamics",
                    "JurassiCraft",
                    "Natura",
                    "NaturalPledge",
                    "PamsHarvestCraft",
                    "Plants",
                    "PrimalCore",
                    "Rustic",
                    "SugiForest",
                    "Terra",
                    "Terraqueous",
                    "Thaumcraft",
                    "TheBetweenLands",
                    "TheErebus",
                    "TheMidnight",
                    "TheTwilightForest",
                    "Traverse",
                    "Treasure2",
                    "Tropicraft",
                    "VibrantJourneys" };
            for (String mod : availableMods) {
                if (builder.comment(mod).define(mod, false).get())
                    activeMods.add(mod);
            }

            String[] tempTreeConfig = builder.comment("Allows you to add your own custom trees, use the following google sheet to design your own trees more easily (Make a copy): http://bit.ly/treeconfig")
                            .define("customTrees", new String[] {}).get();
            builder.pop();
            builder.pop();
            List<TreeConfiguration> tempTreeConfigurations = new ArrayList<TreeConfiguration>();
            for (String treeConfig : tempTreeConfig) {
                tempTreeConfigurations.add(new Gson().fromJson(treeConfig, TreeConfiguration.class));
            }
            TreeConfiguration[] tempCustomTrees = tempTreeConfigurations.toArray(new TreeConfiguration[tempTreeConfigurations.size()]);
            mods.setCustomTrees(tempCustomTrees);
            String[] array = new String[activeMods.size()];
            array = activeMods.toArray(array);
            mods.ActivateMods(array);
            treeConfigurations = mods.UnifiedTreeConfigs.toArray(new TreeConfiguration[mods.UnifiedTreeConfigs.size()]);
            GenerateLeavesAndLogs();
        }

        private static void GenerateLeavesAndLogs() {
            leaves = new String[] {};
            logs = new String[] {};
            for (TreeConfiguration treeConfig : treeConfigurations) {
                leaves = MergeArray(leaves, treeConfig.Leaves());
                logs = MergeArray(logs, ConvertListToArray(treeConfig.Logs()));
            }
        }

        static String[] MergeArray(String[] a, String[] b) {
            String[] d = a;
            for (String c : b) {
                if (!ArrayUtils.contains(d, c)) {
                    d = ArrayUtils.add(d, c);
                }
            }
            return d;
        }

        public static String[] ConvertListToArray(List<String> list) {
            return list.toArray(new String[list.size()]);
        }
    }



    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static //constructor
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        try {
            COMMON = new Common(builder);
            COMMON_SPEC = builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
