package gkappa.chopdown.config.mods;

import gkappa.chopdown.config.TreeConfiguration;

public class BiomesOPlenty {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] {
			new TreeConfiguration(9,12,0,5).setLogs("biomesoplenty:redwood_log", "biomesoplenty:stripped_redwood_log").setLeaves("biomesoplenty:redwood_leaves"),
			new TreeConfiguration(9,12,4,1).setLogs("biomesoplenty:willow_log", "biomesoplenty:stripped_willow_log").setLeaves("biomesoplenty:willo_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:fir_log", "biomesoplenty:stripped_fir_log").setLeaves("biomesoplenty:fir_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:cherry_log", "biomesoplenty:stripped_cherry_log").setLeaves("biomesoplenty:pink_cherry_leaves", "biomesoplenty:white_cherry_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:mahogany_log", "biomesoplenty:stripped_mahogany_log").setLeaves("biomesoplenty:mahogany_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:jacaranda_log", "biomesoplenty:stripped_jacaranda_log").setLeaves("biomesoplenty:jacaranda_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:palm_log", "biomesoplenty:stripped_palm_log").setLeaves("biomesoplenty:palm_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:dead_log", "biomesoplenty:stripped_dead_log").setLeaves("biomesoplenty:dead_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:magic_log", "biomesoplenty:stripped_magic_log").setLeaves("biomesoplenty:magic_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:umbran_log", "biomesoplenty:stripped_umbran_log").setLeaves("biomesoplenty:umbran_leaves"),
			new TreeConfiguration().setLogs("biomesoplenty:hellbark_log", "biomesoplenty:stripped_hellbark_log").setLeaves("biomesoplenty:hellbark_leaves"),

			new TreeConfiguration().setLogs("minecraft:birch_log", "minecraft:stripped_birch_log").setLeaves("biomesoplenty:rainbow_birch_leaves", "biomesoplenty:yellow_autumn_leaves", "biomesoplenty:orange_autumn_leaves"),
			new TreeConfiguration().setLogs("minecraft:oak_log", "minecraft:stripped_oak_log").setLeaves("biomesoplenty:origin_leaves", "biomesoplenty:flowering_oak_leaves"),

			};
}
