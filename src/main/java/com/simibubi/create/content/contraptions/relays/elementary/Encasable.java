package com.simibubi.create.content.contraptions.relays.elementary;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Implement this interface to indicate that a block should be encasable
 */
public interface Encasable {

	/**
	 * Implement this method into your overridden use method.
	 * @return If the Interaction result was a success, or pass
	 */
	default InteractionResult tryEncase(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand,
		BlockHitResult ray) {
		List<Block> encasedBlocks = EncasableRegistry.getValidEncasedBlocks(state.getBlock());
		for (Block block : encasedBlocks) {
			if (block instanceof Encased encased) {
				if (encased.getCasing().asItem() != heldItem.getItem())
					continue;

				if (level.isClientSide)
					return InteractionResult.SUCCESS;

				encased.handleEncasing(state, level, pos, block, hand, heldItem, player, ray);
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}
}
