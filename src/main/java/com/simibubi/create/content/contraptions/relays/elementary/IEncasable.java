package com.simibubi.create.content.contraptions.relays.elementary;

import java.util.ArrayList;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.EncasedShaftBlock;

import com.simibubi.create.foundation.data.Encasable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Implement this interface to indicate that a block should be encasable
 */
public interface IEncasable {

	/**
	 * Handles how encasement should be done if tryEncase is successful
	 */
	void handleEncasing(BlockState state, Level level, BlockPos pos, Block encasedBlock);

	/**
	 * Implement this method into your overridden use method.
	 * @return If the Interaction result was a success, or pass
	 */
	default InteractionResult tryEncase(BlockState state, Level level, BlockPos pos,ItemStack heldItem) {
		ArrayList<Block> encasedBlocks = Encasable.encasableBlocks.get(state.getBlock());
		for (Block block : encasedBlocks) {
			if(block instanceof IEncased encased){
				if (encased.getCasing().asItem() != heldItem.getItem())
					continue;

				if (level.isClientSide)
					return InteractionResult.SUCCESS;

				handleEncasing(state, level, pos, block);
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}
}
