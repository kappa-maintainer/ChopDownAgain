package gkappa.chopdown.config.mods;

import gkappa.chopdown.config.TreeConfiguration;

public class TheTwilightForest {
	public static TreeConfiguration[] Trees = new TreeConfiguration[] { 
			new TreeConfiguration(20,12,0,1).setLogs("twilightforest:twilight_log:0").setLeaves("twilightforest:twilight_leaves:0","twilightforest:twilight_leaves:3"),
			new TreeConfiguration().setLogs("twilightforest:twilight_log:1").setLeaves("twilightforest:twilight_leaves:1"),
			new TreeConfiguration(9,12,4,5).setLogs("twilightforest:twilight_log:2").setLeaves("twilightforest:twilight_leaves:2","twilightforest:magic_leaves:0"),
			new TreeConfiguration().setLogs("twilightforest:twilight_log:3").setLeaves("twilightforest:dark_leaves:0"),
			new TreeConfiguration().setLogs("twilightforest:magic_log:0","twilightforest:magic_log_core:0").setLeaves("twilightforest:magic_leaves:0"),
			new TreeConfiguration().setLogs("twilightforest:magic_log:1","twilightforest:magic_log_core:1").setLeaves("twilightforest:magic_leaves:1"),
			new TreeConfiguration().setLogs("twilightforest:magic_log:2","twilightforest:magic_log_core:2").setLeaves("twilightforest:magic_leaves:2"),
			new TreeConfiguration().setLogs("twilightforest:magic_log:3","twilightforest:magic_log_core:3").setLeaves("twilightforest:magic_leaves:3"),
	};
}
