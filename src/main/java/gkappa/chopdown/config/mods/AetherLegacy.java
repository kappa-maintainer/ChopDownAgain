package gkappa.chopdown.config.mods;

import gkappa.chopdown.config.TreeConfiguration;

public class AetherLegacy {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] { 
			new TreeConfiguration().setLogs("aether_legacy:aether_log:.*").setLeaves("aether_legacy:aether_leaves:.*","aether_legacy:crystal_leaves:.*","aether_legacy:holiday_leaves:.*"),
	};
}
