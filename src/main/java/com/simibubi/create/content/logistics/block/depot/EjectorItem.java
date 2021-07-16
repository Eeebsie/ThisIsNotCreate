package com.simibubi.create.content.logistics.block.depot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import net.minecraft.item.Item.Properties;

@EventBusSubscriber
public class EjectorItem extends BlockItem {

	public EjectorItem(Block p_i48527_1_, Properties p_i48527_2_) {
		super(p_i48527_1_, p_i48527_2_);
	}

	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if (player != null && player.isShiftKeyDown())
			return ActionResultType.SUCCESS;
		return super.useOn(ctx);
	}

	@Override
	protected BlockState getPlacementState(BlockItemUseContext pContext) {
		BlockState stateForPlacement = super.getPlacementState(pContext);
		return stateForPlacement;
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, World world, PlayerEntity pPlayer, ItemStack pStack,
		BlockState pState) {
		if (world.isClientSide)
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EjectorTargetHandler.flushSettings(pos));
		return super.updateCustomBlockEntityTag(pos, world, pPlayer, pStack, pState);
	}

	@Override
	public boolean canAttackBlock(BlockState state, World world, BlockPos pos,
		PlayerEntity pPlayer) {
		return !pPlayer.isShiftKeyDown();
	}

}
