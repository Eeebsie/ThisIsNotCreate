package com.simibubi.create.foundation.gametest.tests;

import java.util.List;

import com.simibubi.create.foundation.gametest.infrastructure.CreateGameTestHelper;
import com.simibubi.create.foundation.gametest.infrastructure.GameTestGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraftforge.fluids.FluidStack;

import static com.simibubi.create.foundation.gametest.infrastructure.CreateGameTestHelper.TEN_SECONDS;
import static com.simibubi.create.foundation.gametest.infrastructure.CreateGameTestHelper.TWENTY_SECONDS;

@GameTestGroup(path = "contraptions")
public class TestContraptions {
	@GameTest(template = "arrow_dispenser", timeoutTicks = TEN_SECONDS)
	public static void arrowDispenser(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(2, 3, 1);
		helper.pullLever(lever);
		BlockPos pos1 = new BlockPos(0, 5, 0);
		BlockPos pos2 = new BlockPos(4, 5, 4);
		helper.succeedWhen(() -> {
			helper.assertSecondsPassed(7);
			List<Arrow> arrows = helper.getEntitiesBetween(EntityType.ARROW, pos1, pos2);
			if (arrows.size() != 4)
				helper.fail("Expected 4 arrows");
			helper.pullLever(lever); // disassemble contraption
			BlockPos dispenser = new BlockPos(2, 5, 2);
			// there should be 1 left over
			helper.assertContainerContains(dispenser, Items.ARROW);
		});
	}

	@GameTest(template = "crop_farming", timeoutTicks = TEN_SECONDS)
	public static void cropFarming(CreateGameTestHelper helper) {
		BlockPos lever = new BlockPos(4, 3, 1);
		helper.pullLever(lever);
		BlockPos output = new BlockPos(1, 3, 12);
		helper.succeedWhen(() -> {
			helper.assertAnyContained(output, Items.WHEAT, Items.POTATO, Items.CARROT);
		});
	}

	@GameTest(template = "mounted_item_extract", timeoutTicks = TWENTY_SECONDS)
	public static void mountedItemExtract(CreateGameTestHelper helper) {
		BlockPos barrel = new BlockPos(1, 3, 2);
		List<ItemStack> originalStacks = helper.getAllContainedStacks(barrel);
		BlockPos lever = new BlockPos(1, 5, 1);
		helper.pullLever(lever);
		BlockPos outputPos = new BlockPos(4, 2, 1);
		helper.succeedWhen(() -> {
			helper.assertAllStacksPresent(originalStacks, outputPos); // verify all extracted
			if (!helper.getBlockState(lever).getValue(LeverBlock.POWERED)) {
				helper.pullLever(lever); // disassemble contraption
			}
			helper.assertContainerEmpty(barrel); // verify nothing left
		});
	}

	@GameTest(template = "mounted_fluid_drain", timeoutTicks = TEN_SECONDS)
	public static void mountedFluidDrain(CreateGameTestHelper helper) {
		BlockPos tank = new BlockPos(1, 3, 2);
		FluidStack fluid = helper.getTankContents(tank);
		BlockPos lever = new BlockPos(1, 5, 1);
		helper.pullLever(lever);
		BlockPos output = new BlockPos(4, 2, 1);
		helper.succeedWhen(() -> {
			helper.assertFluidPresent(fluid, output); // verify all extracted
			if (!helper.getBlockState(lever).getValue(LeverBlock.POWERED)) {
				helper.pullLever(lever); // disassemble contraption
			}
			helper.assertTankEmpty(tank); // verify nothing left
		});
	}

	@GameTest(template = "ploughing")
	public static void ploughing(CreateGameTestHelper helper) {
		BlockPos dirt = new BlockPos(4, 2, 1);
		BlockPos lever = new BlockPos(3, 3, 2);
		helper.pullLever(lever);
		helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.FARMLAND, dirt));
	}

	@GameTest(template = "redstone_contacts")
	public static void redstoneContacts(CreateGameTestHelper helper) {
		BlockPos end = new BlockPos(5, 10, 1);
		BlockPos lever = new BlockPos(1, 3, 2);
		helper.pullLever(lever);
		helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, end));
	}

	// FIXME: trains do not enjoy being loaded in structures
	// https://gist.github.com/TropheusJ/f2d0a7df48360d2e078d0987c115c6ef
//	@GameTest(template = "train_observer")
//	public static void trainObserver(CreateGameTestHelper helper) {
//		helper.fail("NYI");
//	}
}
