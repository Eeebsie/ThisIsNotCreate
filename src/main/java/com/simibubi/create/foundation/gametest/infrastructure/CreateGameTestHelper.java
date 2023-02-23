package com.simibubi.create.foundation.gametest.infrastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.foundation.mixin.accessor.GameTestHelperAccessor;

import net.minecraftforge.fluids.FluidStack;

import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import org.jetbrains.annotations.Contract;

import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.logistics.block.belts.tunnel.BrassTunnelTileEntity.SelectionMode;
import com.simibubi.create.content.logistics.block.redstone.NixieTubeTileEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.tileEntity.IMultiTileContainer;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * An extension to {@link GameTestHelper} with added utilities.
 */
public class CreateGameTestHelper extends GameTestHelper {
	public static final int TICKS_PER_SECOND = 20;
	public static final int TEN_SECONDS = 10 * TICKS_PER_SECOND;
	public static final int FIFTEEN_SECONDS = 15 * TICKS_PER_SECOND;
	public static final int TWENTY_SECONDS = 20 * TICKS_PER_SECOND;

	private CreateGameTestHelper(GameTestInfo testInfo) {
		super(testInfo);
	}

	public static CreateGameTestHelper of(GameTestHelper original) {
		GameTestHelperAccessor access = (GameTestHelperAccessor) original;
		CreateGameTestHelper helper = new CreateGameTestHelper(access.getTestInfo());
		//noinspection DataFlowIssue // accessor applied at runtime
		GameTestHelperAccessor newAccess = (GameTestHelperAccessor) helper;
		newAccess.setFinalCheckAdded(access.getFinalCheckAdded());
		return helper;
	}

	/**
	 * Flip the direction of any block with the {@link BlockStateProperties#FACING} property.
	 */
	public void flipBlock(BlockPos pos) {
		BlockState original = getBlockState(pos);
		if (!original.hasProperty(BlockStateProperties.FACING))
			fail("FACING property not in block: " + Registry.BLOCK.getId(original.getBlock()));
		Direction facing = original.getValue(BlockStateProperties.FACING);
		BlockState reversed = original.setValue(BlockStateProperties.FACING, facing.getOpposite());
		setBlock(pos, reversed);
	}

	public ItemEntity spawnItem(BlockPos pos, ItemStack stack) {
		Vec3 spawn = Vec3.atCenterOf(absolutePos(pos));
		ServerLevel level = getLevel();
		ItemEntity item = new ItemEntity(level, spawn.x, spawn.y, spawn.z, stack, 0, 0, 0);
		level.addFreshEntity(item);
		return item;
	}

	public FluidStack getTankContents(BlockPos tank) {
		IFluidHandler handler = fluidStorageAt(tank);
		return handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
	}

	public long getTankCapacity(BlockPos pos) {
		IFluidHandler handler = fluidStorageAt(pos);
		long total = 0;
		for (int i = 0; i < handler.getTanks(); i++) {
			total += handler.getTankCapacity(i);
		}
		return total;
	}

	/**
	 * Get the total fluid amount across all fluid tanks at the given positions.
	 */
	public long getFluidInTanks(BlockPos... tanks) {
		long total = 0;
		for (BlockPos tank : tanks) {
			total += getTankContents(tank).getAmount();
		}
		return total;
	}

	public IItemHandler itemStorageAt(BlockPos pos) {
		BlockEntity be = getBlockEntity(pos);
		if (be == null)
			fail("BlockEntity not present");
		Optional<IItemHandler> handler = be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
		if (handler.isEmpty())
			fail("handler not present");
		return handler.get();
	}

	public List<ItemStack> getAllContainedStacks(BlockPos pos) {
		IItemHandler handler = itemStorageAt(pos);
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack stack = handler.getStackInSlot(i);
			if (!stack.isEmpty())
				stacks.add(stack);
		}
		return stacks;
	}

	public long getTotalItems(BlockPos pos) {
		IItemHandler storage = itemStorageAt(pos);
		long total = 0;
		for (int i = 0; i < storage.getSlots(); i++) {
			total += storage.getStackInSlot(i).getCount();
		}
		return total;
	}

	public IFluidHandler fluidStorageAt(BlockPos pos) {
		BlockEntity be = getBlockEntity(pos);
		if (be == null)
			fail("BlockEntity not present");
		Optional<IFluidHandler> handler = be.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).resolve();
		if (handler.isEmpty())
			fail("handler not present");
		return handler.get();
	}

	public <T extends BlockEntity> T getBlockEntity(BlockEntityType<T> type, BlockPos pos) {
		BlockEntity be = getBlockEntity(pos);
		BlockEntityType<?> actualType = be == null ? null : be.getType();
		if (actualType != type) {
			String actualId = actualType == null ? "null" : RegisteredObjects.getKeyOrThrow(actualType).toString();
			String error = "Expected block entity at pos [%s] with type [%s], got [%s]".formatted(
					pos, RegisteredObjects.getKeyOrThrow(type), actualId
			);
			fail(error);
		}
		return (T) be;
	}

	public <T extends BlockEntity & IMultiTileContainer> T getControllerBlockEntity(BlockEntityType<T> type, BlockPos anySegment) {
		T be = getBlockEntity(type, anySegment).getControllerTE();
		if (be == null)
			fail("Could not get block entity controller with type [%s] from pos [%s]".formatted(RegisteredObjects.getKeyOrThrow(type), anySegment));
		return be;
	}

	public void assertSecondsPassed(int seconds) {
		if (getTick() < (long) seconds * TICKS_PER_SECOND)
			fail("Waiting for %s seconds to pass".formatted(seconds));
	}

	public long secondsPassed() {
		return getTick() % 20;
	}

	public void assertAnyContained(BlockPos pos, Item... items) {
		IItemHandler handler = itemStorageAt(pos);
		boolean noneFound = true;
		for (int i = 0; i < handler.getSlots(); i++) {
			for (Item item : items) {
				if (handler.getStackInSlot(i).is(item)) {
					noneFound = false;
					break;
				}
			}
		}
		if (noneFound)
			fail("No matching items " + Arrays.toString(items) + " found in handler at pos: " + pos);
	}

	public void assertAllStacksPresent(List<ItemStack> stacks, BlockPos pos) {
		IItemHandler handler = itemStorageAt(pos);
		List<ItemStack> toExtract = new ArrayList<>(); // compress stacks
		stacks.forEach(stack -> {
			for (ItemStack stackToExtract : toExtract) {
				if (ItemHandlerHelper.canItemStacksStack(stack, stackToExtract)) {
					stackToExtract.grow(stack.getCount());
					return;
				}
			}
			toExtract.add(stack);
		});
		for (ItemStack stack : toExtract) {
			ItemStack extracted = ItemHelper.extract(handler, s -> ItemHandlerHelper.canItemStacksStack(stack, s), stack.getCount(), true);
			if (extracted.isEmpty())
				fail("ItemStack not present: " + stack);
		}
	}

	public void assertFluidPresent(FluidStack fluid, BlockPos pos) {
		FluidStack contained = getTankContents(pos);
		if (!fluid.isFluidEqual(contained))
			fail("Different fluids");
		if (fluid.getAmount() != contained.getAmount())
			fail("Different amounts");
	}

	public void assertTankEmpty(BlockPos pos) {
		assertFluidPresent(FluidStack.EMPTY, pos);
	}

	public <T extends Entity> T getFirstEntity(EntityType<T> type, BlockPos pos) {
		List<T> list = getEntitiesBetween(type, pos.north().east().above(), pos.south().west().below());
		if (list.isEmpty())
			fail("No entities at pos: " + pos);
		return list.get(0);
	}

	public <T extends Entity> List<T> getEntitiesBetween(EntityType<T> type, BlockPos pos1, BlockPos pos2) {
		BoundingBox box = BoundingBox.fromCorners(absolutePos(pos1), absolutePos(pos2));
		List<? extends T> entities = getLevel().getEntities(type, e -> box.isInside(e.blockPosition()));
		return (List<T>) entities;
	}

	public void assertNixieRedstone(BlockPos pos, int strength) {
		NixieTubeTileEntity nixie = getBlockEntity(AllTileEntities.NIXIE_TUBE.get(), pos);
		int actualStrength = nixie.getRedstoneStrength();
		if (actualStrength != strength)
			fail("Expected nixie tube at %s to have power of %s, got %s".formatted(pos, strength, actualStrength));
	}

	public void assertCloseEnoughTo(double value, double expected) {
		assertInRange(value, expected - 1, expected + 1);
	}

	public void assertInRange(double value, double min, double max) {
		if (value < min)
			fail("Value %s below expected min of %s".formatted(value, min));
		if (value > max)
			fail("Value %s greater than expected max of %s".formatted(value, max));
	}

	public void whenSecondsPassed(int seconds, Runnable run) {
		runAfterDelay((long) seconds * TICKS_PER_SECOND, run);
	}

	public <T extends TileEntityBehaviour> T getBehavior(BlockPos pos, BehaviourType<T> type) {
		T behavior = TileEntityBehaviour.get(getLevel(), absolutePos(pos), type);
		if (behavior == null)
			fail("Behavior at " + pos + " missing, expected " + type.getName());
		return behavior;
	}

	public void setTunnelMode(BlockPos pos, SelectionMode mode) {
		ScrollValueBehaviour behavior = getBehavior(pos, ScrollOptionBehaviour.TYPE);
		behavior.setValue(mode.ordinal());
	}

	@Override
	public void assertContainerEmpty(@NotNull BlockPos pos) {
		IItemHandler storage = itemStorageAt(pos);
		for (int i = 0; i < storage.getSlots(); i++) {
			if (!storage.getStackInSlot(i).isEmpty())
				fail("Storage not empty");
		}
	}

	public void assertContainersEmpty(List<BlockPos> positions) {
		for (BlockPos pos : positions) {
			assertContainerEmpty(pos);
		}
	}

	@Contract("_->fail") // make IDEA happier
	@Override
	public void fail(@NotNull String exceptionMessage) {
		super.fail(exceptionMessage);
	}

	// support non-minecraft storages

	public void assertContainerContains(BlockPos pos, ItemLike item) {
		assertContainerContains(pos, item.asItem());
	}

	@Override
	public void assertContainerContains(@NotNull BlockPos pos, @NotNull Item item) {
		assertContainerContains(pos, new ItemStack(item));
	}

	public void assertContainerContains(BlockPos pos, ItemStack item) {
		IItemHandler storage = itemStorageAt(pos);
		ItemStack extracted = ItemHelper.extract(storage, stack -> ItemHandlerHelper.canItemStacksStack(stack, item), item.getCount(), true);
		if (extracted.isEmpty())
			fail("item not present: " + item);
	}
}
