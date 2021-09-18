package com.simibubi.create.foundation.command;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import com.simibubi.create.foundation.utility.BoundingBoxHelper;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.world.Clearable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.server.level.ServerLevel;

public class CloneCommand {

	private static final Dynamic2CommandExceptionType CLONE_TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType(
		(arg1, arg2) -> new TranslatableComponent("commands.clone.toobig", arg1, arg2));

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("clone")
			.requires(cs -> cs.hasPermission(2))
			.then(Commands.argument("begin", BlockPosArgument.blockPos())
				.then(Commands.argument("end", BlockPosArgument.blockPos())
					.then(Commands.argument("destination", BlockPosArgument.blockPos())
						.then(Commands.literal("skipBlocks")
							.executes(ctx -> doClone(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "begin"),
								BlockPosArgument.getLoadedBlockPos(ctx, "end"),
								BlockPosArgument.getLoadedBlockPos(ctx, "destination"), false)))
						.executes(ctx -> doClone(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "begin"),
							BlockPosArgument.getLoadedBlockPos(ctx, "end"),
							BlockPosArgument.getLoadedBlockPos(ctx, "destination"), true)))))
			.executes(ctx -> {
				ctx.getSource()
					.sendSuccess(new TextComponent(
						"Clones all blocks as well as super glue from the specified area to the target destination"),
						true);

				return Command.SINGLE_SUCCESS;
			});

	}

	private static int doClone(CommandSourceStack source, BlockPos begin, BlockPos end, BlockPos destination,
		boolean cloneBlocks) throws CommandSyntaxException {
		BoundingBox sourceArea = BoundingBoxHelper.of(begin, end);
		BlockPos destinationEnd = destination.offset(sourceArea.getLength());
		BoundingBox destinationArea = BoundingBoxHelper.of(destination, destinationEnd);

		int i = sourceArea.getXSpan() * sourceArea.getYSpan() * sourceArea.getZSpan();
		if (i > 32768)
			throw CLONE_TOO_BIG_EXCEPTION.create(32768, i);

		ServerLevel world = source.getLevel();

		if (!world.hasChunksAt(begin, end) || !world.hasChunksAt(destination, destinationEnd))
			throw BlockPosArgument.ERROR_NOT_LOADED.create();

		BlockPos diffToTarget = new BlockPos(destinationArea.minX - sourceArea.minX,
			destinationArea.minY - sourceArea.minY, destinationArea.minZ - sourceArea.minZ);

		int blockPastes = cloneBlocks ? cloneBlocks(sourceArea, world, diffToTarget) : 0;
		int gluePastes = cloneGlue(sourceArea, world, diffToTarget);

		if (cloneBlocks)
			source.sendSuccess(new TextComponent("Successfully cloned " + blockPastes + " Blocks"), true);

		source.sendSuccess(new TextComponent("Successfully applied glue " + gluePastes + " times"), true);
		return blockPastes + gluePastes;

	}

	private static int cloneGlue(BoundingBox sourceArea, ServerLevel world, BlockPos diffToTarget) {
		int gluePastes = 0;

		List<SuperGlueEntity> glue =
			world.getEntitiesOfClass(SuperGlueEntity.class, AABB.of(sourceArea));
		List<Pair<BlockPos, Direction>> newGlue = Lists.newArrayList();

		for (SuperGlueEntity g : glue) {
			BlockPos pos = g.getHangingPosition();
			Direction direction = g.getFacingDirection();
			newGlue.add(Pair.of(pos.offset(diffToTarget), direction));
		}

		for (Pair<BlockPos, Direction> p : newGlue) {
			SuperGlueEntity g = new SuperGlueEntity(world, p.getFirst(), p.getSecond());
			if (g.onValidSurface()) {
				world.addFreshEntity(g);
				gluePastes++;
			}
		}
		return gluePastes;
	}

	private static int cloneBlocks(BoundingBox sourceArea, ServerLevel world, BlockPos diffToTarget) {
		int blockPastes = 0;

		List<StructureTemplate.StructureBlockInfo> blocks = Lists.newArrayList();
		List<StructureTemplate.StructureBlockInfo> tileBlocks = Lists.newArrayList();

		for (int z = sourceArea.minZ; z <= sourceArea.maxZ; ++z) {
			for (int y = sourceArea.minY; y <= sourceArea.maxY; ++y) {
				for (int x = sourceArea.minX; x <= sourceArea.maxX; ++x) {
					BlockPos currentPos = new BlockPos(x, y, z);
					BlockPos newPos = currentPos.offset(diffToTarget);
					BlockInWorld cached = new BlockInWorld(world, currentPos, false);
					BlockState state = cached.getState();
					BlockEntity te = world.getBlockEntity(currentPos);
					if (te != null) {
						CompoundTag nbt = te.save(new CompoundTag());
						tileBlocks.add(new StructureTemplate.StructureBlockInfo(newPos, state, nbt));
					} else {
						blocks.add(new StructureTemplate.StructureBlockInfo(newPos, state, null));
					}
				}
			}
		}

		List<StructureTemplate.StructureBlockInfo> allBlocks = Lists.newArrayList();
		allBlocks.addAll(blocks);
		allBlocks.addAll(tileBlocks);

		List<StructureTemplate.StructureBlockInfo> reverse = Lists.reverse(allBlocks);

		for (StructureTemplate.StructureBlockInfo info : reverse) {
			BlockEntity te = world.getBlockEntity(info.pos);
			Clearable.tryClear(te);
			world.setBlock(info.pos, Blocks.BARRIER.defaultBlockState(), 2);
		}

		for (StructureTemplate.StructureBlockInfo info : allBlocks) {
			if (world.setBlock(info.pos, info.state, 2))
				blockPastes++;
		}

		for (StructureTemplate.StructureBlockInfo info : tileBlocks) {
			BlockEntity te = world.getBlockEntity(info.pos);
			if (te != null && info.nbt != null) {
				info.nbt.putInt("x", info.pos.getX());
				info.nbt.putInt("y", info.pos.getY());
				info.nbt.putInt("z", info.pos.getZ());
				te.load(info.nbt);
				te.setChanged();
			}

			// idk why the state is set twice for a te, but its done like this in the original clone command
			world.setBlock(info.pos, info.state, 2);
		}

		for (StructureTemplate.StructureBlockInfo info : reverse) {
			world.blockUpdated(info.pos, info.state.getBlock());
		}

		world.getBlockTicks()
			.copy(sourceArea, diffToTarget);

		return blockPastes;
	}

}
