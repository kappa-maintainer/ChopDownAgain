package gkappa.chopdown.config.mods;

import gkappa.chopdown.config.TreeConfiguration;

public class Vanilla {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] {
			new TreeConfiguration().setLogs("minecraft:oak_log", "minecraft:stripped_oak_log").setLeaves("minecraft:oak_leaves"),
			new TreeConfiguration().setLogs("minecraft:spruce_log", "minecraft:stripped_spruce_log").setLeaves("minecraft:spruce_leaves"),
			new TreeConfiguration().setLogs("minecraft:brich_log", "minecraft:stripped_brich_log").setLeaves("minecraft:brich_leaves"),
			new TreeConfiguration().setLogs("minecraft:jungle_log", "minecraft:stripped_jungle_log").setLeaves("minecraft:jungle_leaves"),
			new TreeConfiguration().setLogs("minecraft:acacia_log", "minecraft:stripped_acacia_log").setLeaves("minecraft:acacia_leaves"),
			new TreeConfiguration().setLogs("minecraft:dark_oak_log", "minecraft:stripped_dark_oak_log").setLeaves("minecraft:dark_oak_leaves"),
			new TreeConfiguration().setLogs("minecraft:crimson_stem", "minecraft:stripped_crimson_stem").setLeaves("minecraft:nether_wart_block"),
			new TreeConfiguration().setLogs("minecraft:warped_stem", "minecraft:stripped_warped_stem").setLeaves("minecraft:warped_wart_block"),
			new TreeConfiguration().setLogs("minecraft:mushroom_stem").setLeaves("minecraft:red_mushroom_block"),
			new TreeConfiguration().setLogs("minecraft:mushroom_stem").setLeaves("minecraft:brown_mushroom_block"),
			};
}
