package gkappa.chopdown.tree;


import net.minecraft.block.BlockState;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

class TreeMovePair {
		public BlockPos to;
		public BlockPos from;
		public Tree tree;
		public Boolean leaves;
		public TileEntity tile;
		public BlockState state;
		public Boolean moved = false;

		public TreeMovePair(BlockPos from, BlockPos to, Tree tree) {
			this.from = from;
			this.to = to;
			this.tree= tree;
			leaves = tree.isLeaves(from);
			tile = tree.world.getBlockEntity(from);
			state = tree.world.getBlockState(from);
			if (tree.isLog(from)) {
				state = tree.rotateLog(tree.world, state);
			}


		}
		public void move() {
			BlockState state2 = tree.world.getBlockState(to);
			if (!tree.isAir(to)) {
				Tree.dropDrops(from, to, state2,tree.world);
			}
			tree.world.setBlock(to, state, 2);
			if (tile != null) {
				CompoundNBT tileEntityData = tile.save(new CompoundNBT());
				TileEntity tileentity = tree.world.getBlockEntity(to);
				if (tileentity != null) {
					CompoundNBT CompoundNBT = tileentity.save(new CompoundNBT());

					for (String s : tileEntityData.getAllKeys()) {
						INBT INBT = tileEntityData.getCompound(s);

						if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s)) {
							CompoundNBT.put(s, INBT.copy());
						}
					}
					tileentity.load(state, CompoundNBT);
					tileentity.setChanged();
				}
			}
		}
	}
