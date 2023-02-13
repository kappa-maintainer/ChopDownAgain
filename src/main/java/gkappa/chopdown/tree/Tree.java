package gkappa.chopdown.tree;

import gkappa.chopdown.config.OptionsHolder;
import gkappa.chopdown.config.PersonalConfig;
import gkappa.chopdown.config.TreeConfiguration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;

public class Tree implements Runnable {

	BlockPos base;
	public ServerWorld world;
	public PlayerEntity player;
	Boolean main = false;
	LinkedList<BlockPos> queue = new LinkedList<BlockPos>();

	HashMap<BlockPos, Integer> estimatedTree = new HashMap<BlockPos, Integer>();
	LinkedList<BlockPos> estimatedTreeQueue = new LinkedList<BlockPos>();

	LinkedList<BlockPos> realisticTree = new LinkedList<BlockPos>();

	HashMap<BlockPos, TreeMovePair> fallingBlocks = new HashMap<BlockPos, TreeMovePair>();

	LinkedList<BlockPos> fallingBlocksList;

	int fallX = 1;
	int fallZ = 0;
	int fallOffset = 0;

	EnumFallAxis axis = EnumFallAxis.X;

	TreeConfiguration config;

	int radius = 8;
	int leafLimit = 7;

	boolean wentUp = false;

	public Boolean finishedCalculation = false;
	public Boolean failedToBuild = false;

	LinkedList<Tree> nearbyTrees = new LinkedList<Tree>();

	/*
	 * Get a tree estimate, used in forests to calculate if leaves should belong to
	 * this tree or the tree we are chopping down
	 */
	public Tree(BlockPos pos, ServerWorld world) throws Exception {
		initTree(pos, world);
		while (isLog(pos.offset(0, -1, 0))) {
			pos = pos.offset(0, -1, 0);
		}
		base = pos;
		getPossibleTree();
	}

	/*
	 * Add a tree that can be chopped down, this is one we are targeting to chop as
	 * opposed to one we just want to get an estimate of blocks from
	 */
	public Tree(BlockPos pos, ServerWorld world, PlayerEntity player) throws Exception {
		main = true;
		this.player = player;
		initTree(pos, world);
		getFallDirection(player);
	}

	public static TreeConfiguration findConfig(World world, BlockPos pos) {
		for (TreeConfiguration treeConfig : OptionsHolder.Common.treeConfigurations) {
			if (treeConfig.isLog(blockName(pos, world))) {
				return treeConfig;
			}
		}
		return null;
	}

	/*
	 * Setup the basic settings of the tree
	 */
	private void initTree(BlockPos pos, ServerWorld world) throws Exception {
		base = pos;
		this.world = world;
		addEstimateBlock(base, 0);
		this.config = findConfig(world, pos);
		if (this.config == null) {
			System.out.println(blockName(base, world) + " block has no tree configuration");
			throw new Exception("The chopped log type is unknown and not setup");
		}
		this.radius = this.config.Radius();
		this.leafLimit = this.config.Leaf_limit();

	}

	/*
	 * Calculate which direction the tree should fall in
	 */
	private void getFallDirection(PlayerEntity player) {
		double x = ((base.getX() + 0.5) - player.position().x);
		double z = (base.getZ() + 0.5) - player.position().z;
		double abX = Math.abs(x);
		double abZ = Math.abs(z);
		fallX = (int) Math.floor(abX / x);
		fallZ = (int) Math.floor(abZ / z);
		if (abX > abZ) {
			fallZ = 0;
			axis = EnumFallAxis.Z;
		} else {
			fallX = 0;
			axis = EnumFallAxis.X;
		}
	}

	public boolean isLog(BlockPos pos) {
		return isLog(blockName(pos, world));
	}

	private boolean isLog(String name) {
		return config.isLog(name);
	}

	public boolean isLeaf(BlockPos pos) {
		return isLeaf(blockName(pos, world));
	}

	private boolean isLeaf(String name) {
		return config.isLeaf(name);
	}

	/*
	 * Gets a possible tree, but only if it thinks the trunk is completely cut
	 * through
	 */
	private void getPossibleTree() throws Exception {
		BuilderQueueComparer comp = new BuilderQueueComparer(estimatedTree);
		try {
			while (!queue.isEmpty()) {
				queue.sort(comp);
				BlockPos blockStep = queue.pollFirst();
				for (int dy = -1; dy <= 1; ++dy) {
					for (int dx = -1; dx <= 1; ++dx) {
						for (int dz = -1; dz <= 1; ++dz) {
							int dzA = dz * dz, dxA = dx * dx, dyA = dy * dy;
							int stepInc = (dzA + dxA + dyA);
							BlockPos inspectPos = blockStep.offset(dx, dy, dz);
							String blockName = blockName(inspectPos, world);

							boolean log = isLog(blockName);
							boolean leaf = false;
							if (!log) {
								leaf = isLeaf(blockName);
							}
							if (!(log || leaf)) {
								continue;
							}

							boolean logAbove = isLog(inspectPos.offset(0, 1, 0));
							int y = inspectPos.getY();
							boolean isTrunk = isTrunk(inspectPos, world, config);
							boolean yMatch = (y == base.getY());
							if (y > base.getY()) {
								wentUp = true;
							}
							Integer leafStep = getEstimate(blockStep);
							if (leafStep == null) {
								leafStep = 0;
							}

							leafStep = leafStep + (leaf ? stepInc : 0);

							// Don't chop below the chop point, nor if this is the base point, nor if
							// leafStep reached, nor if radius limit reaches, nor if this block is our main
							// block
							if (inspectPos.compareTo(base) == 0 || y < base.getY() || leafStep >= leafLimit
									|| horizontalDistance(base, inspectPos) > radius) {
								continue;
							}
							// If not directly connected to the tree search down for a base
							if (log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos) && isTrunk
									&& (Math.abs(inspectPos.getX() - base.getX()) > config.Trunk_Radius()
											|| Math.abs(inspectPos.getZ() - base.getZ()) > config.Trunk_Radius())

							) {
								// Its the trunk of another tree, check to see if we already have this tree in
								// the list, or add it.
								if (main) {
									Boolean treeFound = false;
									for (Tree tree : nearbyTrees) {
										if (tree.getEstimate(inspectPos) != null && tree.getEstimate(inspectPos) == 0) {
											treeFound = true;
										}
									}
									if (!treeFound) {
										Tree otherTree = new Tree(inspectPos, world);
										nearbyTrees.add(otherTree);
									}
								}
								continue;
							} else if (main && log && (leafStep > 0 || dy < 0) && !estimatedTree.containsKey(inspectPos)
									&& isTrunk && isLog(inspectPos.offset(0, 1, 0))) {
								estimatedTree.clear();
								queue.clear();
								return;
							}

							/*
							 * If a log but next to a solid none tree block then fail to chop (avoids 99% of
							 * cases of issues building with logs in houses)
							 * 
							 */
							if (main && log && ((cantDrag(world, inspectPos, config) && !yMatch)
									|| (yMatch && logAbove && !wentUp)) && leafStep == 0) {
								estimatedTree.clear();
								queue.clear();
								return;
							}
							if (!yMatch || !cantDrag(world, inspectPos, config)) {
								addEstimateBlock(inspectPos, leafStep);
							} else {
								continue;
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			throw ex;
		}

	}

	/*
	 * The overall calculation of where the tree should end up, does not actually
	 * drop the blocks, just creates a list of movements needed to be done
	 */
	public void getDropBlocks() throws Exception {
		getPossibleTree();
		getRealisticTree();
		this.finishedCalculation = true;
	}

	/*
	 * Calculate where this block should end up in our fallen tree (pre log in leaf
	 * fall)
	 */
	private BlockPos repositionBlock(BlockPos pos) {
		int y = pos.getY() - base.getY();

		int x = pos.getX() - (base.getX() + fallOffset);
		int z = pos.getZ() - (base.getZ() + fallOffset);

		int changeX = fallZ * z;
		int changeZ = fallX * x;

		int normPosX = (y * fallX);
		int normPosZ = (y * fallZ);

		return pos.offset(normPosX - (changeZ * fallX), -(changeX + changeZ), normPosZ - (changeX * fallZ));
	}

	/*
	 * Gets the block that would end up in the position below this one
	 */
	private TreeMovePair getLowerTargetBlock(BlockPos pos) {
		BlockPos lower = pos.offset(0, -1, 0);
		if (fallingBlocks.containsKey(lower)) {
			return fallingBlocks.get(lower);
		}
		return null;
	}

	/*
	 * Adds a block to the queue unless the queue already processed the block with
	 * this step value and its not still pending in the queue. Updates the blocks
	 * step value if it is lower than the currently stored value.
	 */
	public void addEstimateBlock(BlockPos pos, int step) {
		if (estimatedTree.containsKey(pos) && estimatedTree.get(pos) <= step) {
			return;
		}
		if (!queue.contains(pos)) {
			queue.add(pos);
		}
		estimatedTree.put(pos, step);
	}

	/*
	 * Get the leaf step value from an estimated tree block
	 */
	private Integer getEstimate(BlockPos pos) {
		return estimatedTree.get(pos);
	}

	@SuppressWarnings("deprecation")
	public static String blockName(BlockPos pos, IWorld world) {
		ItemStack stack = null;
		try {
			stack = world.getBlockState(pos).getBlock().getPickBlock(world.getBlockState(pos), null, world, pos, null);
		} catch (Exception ex) {
			try {
				stack = world.getBlockState(pos).getBlock().asItem().getDefaultInstance();
			} catch (Exception ignored) {
			}
		}
		if (stack == null) {
			return "unknown, getPickBlock and getItem not set";
		}
		return stackName(stack);
	}

	public static String stackName(ItemStack stack) {
		try {
			ResourceLocation loc = stack.getItem().getRegistryName();
			int damageValue = stack.getItem().getDamage(stack);
			return loc.getNamespace() + ":" + loc.getPath() + ":" + String.valueOf(damageValue);
		} catch (Exception ex) {
			return "";
		}
	}

	/*
	 * Checks the blocks in the estimated tree against other trees that were found
	 * to determine if the block more likely belongs to this tree or another
	 */
	private void getRealisticTree() {
		estimatedTreeQueue = new LinkedList<BlockPos>(estimatedTree.keySet());
		LinkedList<BlockPos> realisticTree = new LinkedList<BlockPos>();
		while (!estimatedTreeQueue.isEmpty()) {

			BlockPos from = estimatedTreeQueue.pollFirst();
			boolean mine = true;
			int leafStep = estimatedTree.get(from);
			double distance = horizontalDistance(base, from);
			if (distance > config.Radius() || leafStep >= config.Leaf_limit()) {
				continue;
			}
			for (Tree otherTree : nearbyTrees) {
				if (otherTree.myBlock(from, distance, leafStep)) {
					mine = false;
					break;
				}
			}
			if (mine && base != from) {
				if (isLog(from) && (from.getY() == base.getY() + 1 || from.getY() == base.getY() + 2)
						&& ((fallZ != 0 && (isLog(from.offset(1, 0, 0)) || isLog(from.offset(-1, 0, 0))))
								|| (fallX != 0 && (isLog(from.offset(0, 0, 1)) || isLog(from.offset(0, 0, -1)))))) {
					if (from.getX() * fallX > (fallOffset + base.getX()) * fallX) {
						fallOffset = from.getX() - base.getX();
					} else if (from.getZ() * fallZ > (fallOffset + base.getZ()) * fallZ) {
						fallOffset = from.getZ() - base.getZ();
					}
				}
				realisticTree.add(from);
			}
		}
		while (!realisticTree.isEmpty()) {
			BlockPos from = realisticTree.pollFirst();
			BlockPos to = repositionBlock(from);
			TreeMovePair pair = new TreeMovePair(from, to, this);
			fallingBlocks.put(pair.to, pair);
		}
		fallingBlocksList = new LinkedList<BlockPos>(fallingBlocks.keySet());
		fallingBlocksList.sort(new AxisComparer(DirectionSort.UP));
	}

	@Override
	public void run() {
		try {
			this.getDropBlocks();
		} catch (Exception e) {
			this.failedToBuild = true;
		}
	}

	/*
	 * Iterates through blocks waiting to drop
	 */
	public boolean dropBlocks() {
		int blocksRemaining = OptionsHolder.COMMON.maxDropsPerTickPerTree.get();
		BlockPos pos;
		int size = fallingBlocksList.size();
		for (int i = 0; i < size; i++) {
			pos = fallingBlocksList.getFirst();
			TreeMovePair pair = fallingBlocks.get(pos);
			fallingBlocksList.removeFirst();
			if (!drop(pair, fallingBlocks.size() > OptionsHolder.COMMON.maxFallingBlockBeforeManualMove.get())) {
				// not finished moving
				fallingBlocksList.add(pos);
			}
			blocksRemaining--;
			if (blocksRemaining <= 0 && !fallingBlocksList.isEmpty()) {
				return false;
			}
		}
		if (!fallingBlocksList.isEmpty()) {
			return false;
		}
		return true;
	}

	/*
	 * Is the block more likely to be yours or mine?
	 */
	public Boolean myBlock(BlockPos pos, double yourDistance, int yourStepValue) {
		// TODO check if block type matches main types

		Integer step = estimatedTree.get(pos);
		if (step == null || step > yourStepValue) {
			return false;
		}
		if (step == yourStepValue) {
			return horizontalDistance(base, pos) < yourDistance;
		}
		return true;
	}

	/*
	 * Checks to see if the block is on the given axis
	 */
	private Boolean isAxis(BlockState state, Property<?> property, String axis) {
		return ((Enum<?>) (state.getValue(property))).name().equalsIgnoreCase(axis);
	}

	/*
	 * Sets the blocks axis by iterating through the property values.
	 */
	private BlockState setAxis(BlockState state, Property<?> property, String axis) {
		int i = 10;
		while (i > 0 && !isAxis(state, property, axis)) {
			i--;
			state = state.cycle(property);
		}
		return state;
	}

	/*
	 * Trys to rotate the log along the axis given
	 */
	public BlockState rotateLog(World world, BlockState state) {
		Property<?> foundProp = null;
		for (net.minecraft.state.Property<?> prop : state.getProperties()) {
			if (prop.getName().equals("axis")) {
				foundProp = prop;
			}
		}
		if (foundProp == null) {
			return state;
		}
		if (axis == EnumFallAxis.X) {
			if (isAxis(state, foundProp, "Y")) {
				state = setAxis(state, foundProp, "Z");
			} else if (isAxis(state, foundProp, "Z")) {
				state = setAxis(state, foundProp, "Y");
			}
		} else {
			if (isAxis(state, foundProp, "Y")) {
				state = setAxis(state, foundProp, "X");
			} else if (isAxis(state, foundProp, "X")) {
				state = setAxis(state, foundProp, "Y");
			}
		}
		return state;
	}

	public static void dropDrops(BlockPos pos, BlockPos dropPos, BlockState state, ServerWorld world) {
		// Do drops at location)
		for (ItemStack stacky : Block.getDrops(state, world, pos,  world.getBlockEntity(pos))) {
			ItemEntity itemEntity = new ItemEntity(world, dropPos.getX(), dropPos.getY(), dropPos.getZ(), stacky);
			itemEntity.setDefaultPickUpDelay();
			world.tryAddFreshEntityWithPassengers(itemEntity);
		}
	}

	/*
	 * Drops a block in the world (basically moves it if it can, does block drop if
	 * it can't, handles falling entity and calculated drop) Also handles debug
	 * configs.
	 */
	private boolean drop(TreeMovePair pair, Boolean UseSolid) {
		if (!(isLog(pair.from) || isLeaf(pair.from))) {
			return true;
		}
		PersonalConfig playerConfig = OptionsHolder.COMMON.getPlayerConfig(player.getUUID());
		// Turn the tree in to glass if set as don't drop;
		if (playerConfig.makeGlass && playerConfig.dontFell) {
			if (isLog(pair.from)) {
				world.setBlock(pair.from, Blocks.ORANGE_STAINED_GLASS.defaultBlockState(), 2);
			} else {
				world.setBlock(pair.from, Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(), 2);
			}
			return true;
		}
		// Get the state of the tree block (rotate the log if first time moving)
		BlockState state = world.getBlockState(pair.from);
		BlockState originalState = state;
		if (!pair.moved && isLog(pair.from)) {
			state = rotateLog(world, state);
		}
		// If the target block is not passable or the source block is leaves and the
		// config is set to break leaves then do drops and state finished
		if ((!CanMoveTo(pair.to,!pair.leaves) && !pair.moved) || (isLeaf(pair.from) && OptionsHolder.COMMON.breakLeaves.get())) {
			// Do drops at location
			dropDrops(pair.from, pair.to, state, world);
			world.setBlock(pair.from, Blocks.AIR.defaultBlockState(), 2);
			return true;
		} else if (!CanMoveTo(pair.to,!pair.leaves)) {
			return true;
		}
		// Can move to this block, set the source block to air, set the from block as to
		// and state that we moved
		world.setBlock(pair.from, Blocks.AIR.defaultBlockState(), 2);
		pair.from = pair.to;
		pair.moved = true;

		if (playerConfig.dontFell) {
			pair.move();
		} else {
			if (!UseSolid) {
				// Use falling entities
				EntityFallingBlock fallingBlock = new EntityFallingBlock(world, pair.to.getX() + 0.5,
						pair.to.getY() + 0.5, pair.to.getZ() + 0.5, state, pair.tile, !pair.leaves);
				fallingBlock.setBoundingBox(new AxisAlignedBB(pair.to.offset(0, 0, 0), pair.to.offset(1, 1, 1)));
				fallingBlock.time = 1;
				world.tryAddFreshEntityWithPassengers(fallingBlock);
			} else {
				ManuallyDrop(pair, state);
			}
		}
		return true;
	}

	private void ManuallyDrop(TreeMovePair pair, BlockState state) {
		// Move large trees to final resting place
		while (CanMoveTo(pair.to.offset(0, -1, 0),!pair.leaves)) {
			pair.to = pair.to.offset(0, -1, 0);
			if(!isAir(pair.to)) {
				BlockState state2 = world.getBlockState(pair.to);
				Tree.dropDrops(pair.from, pair.to, world.getBlockState(pair.to),world);
				world.setBlock(pair.to,Blocks.AIR.defaultBlockState(), 2);
			}
		}
		pair.move();
	}

	private boolean CanMoveTo(BlockPos pos, Boolean log) {
		return (isAir(pos) || isPassable(pos) || (log && Tree.isLeaves(pos, world))) && pos.getY() > 0;
	}

	/*
	 * Gets the distance on the x-z plane only
	 */
	private double horizontalDistance(BlockPos pos1, BlockPos pos2) {
		int diffX = Math.abs(pos1.getX() - pos2.getX());
		int diffZ = Math.abs(pos1.getZ() - pos2.getZ());
		return Math.floor(Math.sqrt((Math.pow(diffX, 2) + Math.pow(diffZ, 2))));
	}

	/*
	 * If min vertical logs is 0 it only checks for the log being on a solid block,
	 * otherwise it also checks the log is vertically surrounded by the given number
	 * of blocks, this is useful for some BOP trees that have hollow centres or that
	 * get built floating in water.
	 */
	public static Boolean isTrunk(BlockPos pos, World world, TreeConfiguration config) {

		// Normal tree check, requires the tree to be sat on a solid block
		boolean log = true;
		while (log) {
			pos = pos.offset(0, -1, 0);
			if (!config.isLog(blockName(pos, world))) {
				log = false;
				if (!isDraggable(world, pos, config)) {
					return true;
				}
			}
		}

		if (config.Min_vertical_logs() == 0) {
			return false;
		} else {
			// Instead check for at least 4 vertical log blocks above and below
			int below = 0;
			for (int i = 1; i < config.Min_vertical_logs(); i++) {
				if (!config.isLog(blockName(pos.offset(0, -i, 0), world))) {
					break;
				}
				below++;
			}
			int above = 0;
			for (int i = 1; i < config.Min_vertical_logs(); i++) {
				if (!config.isLog(blockName(pos.offset(0, i, 0), world))) {
					break;
				}
				above++;
			}
			return (1 + below + above) >= config.Min_vertical_logs();
		}
	}

	/*
	 * Is the block touching either air, a tree block or a passable block only on
	 * all 6 sides
	 */
	private static boolean cantDrag(World world, BlockPos pos, TreeConfiguration tree) {
		return !isDraggable(world, pos.offset(1, 0, 0), tree) || !isDraggable(world, pos.offset(-1, 0, 0), tree)
				|| !isDraggable(world, pos.offset(0, 1, 0), tree) || !isDraggable(world, pos.offset(0, -1, 0), tree)
				|| !isDraggable(world, pos.offset(0, 0, 1), tree) || !isDraggable(world, pos.offset(0, 0, -1), tree);
	}

	/*
	 * Is this specific block either a tree block, air or a passable block
	 */
	private static boolean isDraggable(World world, BlockPos pos, TreeConfiguration tree) {

		BlockState state = world.getBlockState(pos);

		if (state.getBlock().isAir(state, world, pos) || state.getBlock().isScaffolding(state, world, pos, null)) {
			return true;
		}

		if (tree != null) {
			String name = blockName(pos, world);
			if (tree.isLog(name) || tree.isLeaf(name)) {
				return true;
			}
		}
		return isWood(pos, world) || isLeaves(pos, world);
	}

	/*
	 * Is the block at this position an air block;
	 */
	public Boolean isAir(BlockPos pos) {
		return world.getBlockState(pos).getBlock().isAir(world.getBlockState(pos), world, pos);
	}

	/*
	 * /* Is the block at this position an air block;
	 */
	private Boolean isPassable(BlockPos pos) {
		return world.getBlockState(pos).getBlock().isPathfindable(world.getBlockState(pos), world, pos, PathType.LAND);
	}

	/*
	 * Is the block at this position a log
	 */
	public static boolean isWood(BlockPos pos, IWorld world) {
		String blockName = blockName(pos, world);
		for (String block : OptionsHolder.Common.logs) {
			if (block.equals(blockName) || blockName.matches(block)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Is the block at this position a log
	 */
	public static boolean isLeaves(BlockPos pos, World world) {
		String blockName = blockName(pos, world);
		for (String block : OptionsHolder.Common.leaves) {
			if (block.equals(blockName) || blockName.matches(block)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Is the block at this position leaves
	 */
	public boolean isLeaves(BlockPos pos) {
		return isLeaves(pos, world);
	}

	/*
	 * Class to house the falling logs and leaves
	 */
	public static class EntityFallingBlock extends net.minecraft.entity.item.FallingBlockEntity {

		EntityFallingBlock(World worldIn, double x, double y, double z, BlockState fallingBlockState, TileEntity tile,
						   Boolean isLog) {
			super(worldIn, x, y, z, fallingBlockState);
			this.isLog = isLog;
			setHurtsEntities(true);
			if (tile != null) {
				blockData = tile.save(new CompoundNBT());
			}

		}

		private boolean isLog = true;

		/**
		 * Called to update the entity's position/logic.
		 */
		@Override
		public void tick() {
			Block block = this.getBlockState().getBlock();

			if (this.getBlockState().getMaterial() == Material.AIR) {
				this.remove();
			} else {
				this.xOld = this.xo;
				this.yOld = this.yo;
				this.zOld = this.zo;

				if (this.time++ == 0) {
					BlockPos blockpos = this.blockPosition();

					if (this.level.getBlockState(blockpos).getBlock() == block) {
						this.level.removeBlock(blockpos, false);
					} else if (!this.level.isClientSide) {
						this.remove();
						return;
					}
				}

				if (!this.isNoGravity()) {
					this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
				}
				BlockPos targetBlock = new BlockPos(this.position().add(this.getDeltaMovement()));
				if (isLog) {
					for (int i = 0; i < 100; i++) {

						if (Tree.isLeaves(targetBlock, level)) {
							Tree.dropDrops(targetBlock, targetBlock, level.getBlockState(targetBlock), (ServerWorld) level);
							level.setBlock(targetBlock, Blocks.AIR.defaultBlockState(), 2);

						}
						targetBlock = targetBlock.offset(0, -0.5, 0);
					}
				}
				this.move(MoverType.SELF, this.getDeltaMovement());
				this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));

				if (!this.level.isClientSide) {
					BlockPos blockpos1 = this.blockPosition();

					if (this.onGround) {
						BlockState blockstate = this.level.getBlockState(blockpos1);
						targetBlock = new BlockPos(this.xo, this.yo - 0.01D, this.zo);
						if ((FallingBlock.isFree(this.level.getBlockState(targetBlock))
								&& Tree.blockName(targetBlock.offset(0, -1, 0), this.level).matches("fence"))) {
							this.onGround = false;
							return;
						}
						if (!isLog && Tree.isLeaves(targetBlock, level)) {
							Tree.dropDrops(targetBlock, targetBlock, level.getBlockState(targetBlock), (ServerWorld) level);
							level.setBlock(targetBlock, Blocks.AIR.defaultBlockState(), 2);
							this.onGround = false;
							return;

						}

						this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
						if (!blockstate.is(Blocks.MOVING_PISTON)) {
							this.remove();

							BlockState blockState = this.getBlockState();
							boolean flag2 = blockstate.canBeReplaced(new DirectionalPlaceContext(this.level, blockpos1, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
							boolean flag3 = FallingBlock.isFree(this.level.getBlockState(blockpos1.below()));
							boolean flag4 = blockState.canSurvive(this.level, blockpos1) && !flag3;
							if (flag2 && flag4) {
								if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && this.level.getFluidState(blockpos1).getType() == Fluids.WATER) {
									blockState = blockState.setValue(BlockStateProperties.WATERLOGGED, Boolean.TRUE);
								}

								if (this.level.setBlock(blockpos1, blockState, 3)) {
									if (block instanceof FallingBlock) {
										((FallingBlock)block).onLand(this.level, blockpos1, blockState, blockstate, this);
									}

									if (this.blockData != null && blockState.hasTileEntity()) {
										TileEntity tileentity = this.level.getBlockEntity(blockpos1);
										if (tileentity != null) {
											CompoundNBT compoundnbt = tileentity.save(new CompoundNBT());

											for(String s : this.blockData.getAllKeys()) {
												INBT inbt = this.blockData.get(s);
												if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
													compoundnbt.put(s, inbt.copy());
												}
											}

											tileentity.load(blockState, compoundnbt);
											tileentity.setChanged();
										}
									}
								} else if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
									this.spawnAtLocation(block);
								}
							} else if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
								this.spawnAtLocation(block);
							}

						}
					} else if (this.time > 100 && !this.level.isClientSide
							&& (blockpos1.getY() < 1 || blockpos1.getY() > 256) || this.time > 600) {
						if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
							this.spawnAtLocation(new ItemStack(block, 1), 0.0F);
						}

						this.remove();
					}
				}
			}
		}

		@Nullable
		@Override
		public ItemEntity spawnAtLocation(ItemStack stack, float offsetY) {

			BlockState state = getBlockState();
			Block block = this.getBlockStateOn().getBlock();
			BlockPos pos = this.blockPosition();
			BlockState toState = level.getBlockState(pos);

			boolean isPassable = toState.getBlock().isPathfindable(toState, level, pos, PathType.AIR);
			while (!isPassable && pos.getY() < 256) {
				pos = pos.offset(0, 1, 0);
				toState = level.getBlockState(pos);
				isPassable = toState.getBlock().isPathfindable(toState, level, pos, PathType.AIR);
			}
			if (pos.getY() > 255) {
				return null;
			}
			Tree.dropDrops(pos, pos, toState, (ServerWorld) level);
			level.setBlock(pos, state, 2);
			if (this.blockData != null && block.hasTileEntity(state)) {
				TileEntity tileentity = this.level.getBlockEntity(pos);

				if (tileentity != null) {
					CompoundNBT CompoundNBT = tileentity.save(new CompoundNBT());

					for (String s : this.blockData.getAllKeys()) {
						INBT INBT = this.blockData.getCompound(s);

						if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
							CompoundNBT.put(s, INBT.copy());
						}
					}

					tileentity.load(state, CompoundNBT);
					tileentity.setChanged();
				}
			}
			return null;
		}
	}

}