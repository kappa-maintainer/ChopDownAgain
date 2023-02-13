package gkappa.chopdown.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

public class TreeConfiguration {
	/*
	 * The horizontal radius from the trunk to check for tree members
	 */
	public int Radius() {
		return radius == 0 ? 9 : radius;
	}

	/*
	 * Maximum steps from a log a leaf can be
	 */
	public int Leaf_limit() {
		return leaf_limit == 0 ? 12 : leaf_limit;
	}

	/*
	 * Maximum steps from a log a leaf can be
	 */
	public int Trunk_Radius() {
		return trunk_radius == 0 ? 1 : trunk_radius;
	}

	public int Min_vertical_logs() {
		return min_vertical_logs;
	}

	private int radius = 9;
	private int leaf_limit = 12;
	private int trunk_radius = 1;
	private int min_vertical_logs = 0;
	private List<? extends String> logs;
	private List<? extends String> leaves;
	private List<? extends String> leaves_merged;
	private List<? extends String> blocks = null;

	public TreeConfiguration() {
	}
	public TreeConfiguration(int radius, int leaf_limit, int min_logs, int trunk_radius, List<? extends String> logs,
							 List<? extends String> leaves) {
		this.radius = radius;
		this.leaf_limit = leaf_limit;
		this.trunk_radius = trunk_radius;
		this.logs = logs;
		this.leaves = leaves;
		this.min_vertical_logs = min_logs;
	}
	public TreeConfiguration(int radius, int leaf_limit, int min_logs, int trunk_radius) {
		this.radius = radius;
		this.leaf_limit = leaf_limit;
		this.trunk_radius = trunk_radius;		
		this.min_vertical_logs = min_logs;
	}
	public TreeConfiguration setLogs(String... logs) {
		this.logs = new ArrayList<>(Arrays.asList(logs));
		return this;
	}
	public TreeConfiguration setLeaves(String... leaves) {
		this.leaves = new ArrayList<>(Arrays.asList(leaves));
		return this;
	}

	public boolean isLog(String name) {
		for (String block : logs) {
			if (block.equals(name) || name.matches(block)) {
				return true;
			}
		}
		return false;
	}

	public boolean isLeaf(String name) {
		for (String block : Leaves()) {
			if (block.equals(name) || name.matches(block)) {
				return true;
			}
		}
		return false;
	}

	public List<? extends String> Logs() {
		return logs;
	}

	//Gets all leaves after merging the shared leaves (beehives etc)
	public List<? extends String> Leaves() {
		if (leaves_merged == null) {
			leaves_merged = OptionsHolder.Common.MergeArray(leaves, OptionsHolder.COMMON.sharedLeaves.get());
		}
		return leaves_merged;
	}
	//Gets all blocks associated with this tree
	public List<? extends String> Blocks() {
		if (blocks == null) {
			logs = Stream.of(logs, Leaves()).flatMap(List::stream).distinct().collect(Collectors.toList());
			blocks = logs;
		}
		return blocks;
	}

	public void Merge(TreeConfiguration newTree) {
		// TODO Auto-generated method stub
		logs = Stream.of(logs, newTree.Logs()).flatMap(List::stream).distinct().collect(Collectors.toList());
		leaves = Stream.of(leaves, newTree.Leaves()).flatMap(List::stream).distinct().collect(Collectors.toList());
		leaves_merged = null;
	}
	public TreeConfiguration Clone() {
		return new TreeConfiguration(radius,leaf_limit,min_vertical_logs,trunk_radius, logs,leaves);
	}
}
