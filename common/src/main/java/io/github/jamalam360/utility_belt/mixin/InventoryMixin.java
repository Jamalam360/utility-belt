package io.github.jamalam360.utility_belt.mixin;

import io.github.jamalam360.utility_belt.StateManager;
import io.github.jamalam360.utility_belt.UtilityBeltInventory;
import io.github.jamalam360.utility_belt.UtilityBeltItem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public class InventoryMixin {
	@Shadow
	@Final
	public Player player;

	@Inject(
			method = "getSelected",
			at = @At("HEAD"),
			cancellable = true
	)
	private void utilitybelt$getSelectedInUtilityBelt(CallbackInfoReturnable<ItemStack> cir) {
		StateManager stateManager = StateManager.getServerInstance();
		if (stateManager.isInBelt(this.player)) {
			ItemStack belt = UtilityBeltItem.getBelt(this.player);
			assert belt != null;
			UtilityBeltInventory inv = stateManager.getInventory(this.player);
			cir.setReturnValue(inv.getItem(stateManager.getSelectedBeltSlot(this.player)));
		}
	}

	/**
	 * Necessary because Mojank does not use getSelected in this method.
	 */
	@Inject(
			method = "getDestroySpeed",
			at = @At("HEAD"),
			cancellable = true
	)
	private void utilitybelt$getDestroySpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
		StateManager stateManager = StateManager.getServerInstance();
		if (stateManager.isInBelt(this.player)) {
			ItemStack belt = UtilityBeltItem.getBelt(this.player);
			assert belt != null;
			UtilityBeltInventory inv = stateManager.getInventory(this.player);
			cir.setReturnValue(inv.getItem(stateManager.getSelectedBeltSlot(this.player)).getDestroySpeed(blockState));
		}
	}

	@Inject(
			method = "tick",
			at = @At("RETURN")
	)
	private void utilitybelt$tick(CallbackInfo ci) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		StateManager stateManager = StateManager.getServerInstance();
		if (stateManager.isInBelt(this.player)) {
			assert belt != null;
			UtilityBeltInventory inv = stateManager.getInventory(this.player);
			int selectedSlot = stateManager.getSelectedBeltSlot(this.player);

			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack stack = inv.getItem(i);
				if (!stack.isEmpty()) {
					stack.inventoryTick(this.player.level(), this.player, i, i == selectedSlot);
				}
			}
		}
	}

	@Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
	private void utilitybelt$patchRemoveOneForHeldItems(ItemStack stack, CallbackInfo ci) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		if (belt != null) {
			UtilityBeltInventory inv = StateManager.getServerInstance().getInventory(this.player);
			int found = -1;

			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (ItemStack.matches(stack, inv.getItem(i))) {
					found = i;
					break;
				}
			}

			if (found != -1) {
				inv.setItem(found, ItemStack.EMPTY);
				ci.cancel();
			}
		}
	}

	@Inject(method = "fillStackedContents", at = @At("HEAD"))
	private void utilitybelt$recipeFinderPatch(StackedContents contents, CallbackInfo ci) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		if (belt != null) {
			UtilityBeltInventory inv = StateManager.getServerInstance().getInventory(this.player);

			for (int i = 0; i < inv.getContainerSize(); i++) {
				contents.accountSimpleStack(inv.getItem(i));
			}
		}
	}

	@Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
	private void utilitybelt$dropStackIfUsingUtilityBelt(boolean entireStack, CallbackInfoReturnable<ItemStack> cir) {
		StateManager stateManager = StateManager.getServerInstance();
		if (stateManager.isInBelt(this.player)) {
			int slot = stateManager.getSelectedBeltSlot(this.player);
			UtilityBeltInventory inv = stateManager.getInventory(this.player);
			ItemStack removed = inv.removeItemNoUpdate(slot);
			cir.setReturnValue(removed);
		}
	}

	@Inject(method = "clearContent", at = @At("HEAD"))
	private void utilitybelt$clearUtilityBelt(CallbackInfo ci) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		if (belt != null) {
			UtilityBeltInventory inv = StateManager.getServerInstance().getInventory(this.player);
			inv.clearContent();
		}
	}

	@Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void utilitybelt$patchContainsStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		if (belt != null) {
			UtilityBeltInventory inv = StateManager.getServerInstance().getInventory(this.player);

			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (!inv.getItem(i).isEmpty() && ItemStack.matches(inv.getItem(i), stack)) {
					cir.setReturnValue(true);
				}
			}
		}
	}

	@Inject(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
	private void utilitybelt$patchContainsStack(TagKey<Item> key, CallbackInfoReturnable<Boolean> cir) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		if (belt != null) {
			UtilityBeltInventory inv = StateManager.getServerInstance().getInventory(this.player);

			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (!inv.getItem(i).isEmpty() && inv.getItem(i).is(key)) {
					cir.setReturnValue(true);
				}
			}
		}
	}

	@Inject(method = "dropAll", at = @At("HEAD"))
	private void utilitybelt$dropAllFromUtilityBelt(CallbackInfo ci) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		if (belt != null) {
			UtilityBeltInventory inv = StateManager.getServerInstance().getInventory(this.player);

			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack itemStack = inv.getItem(i);
				if (!itemStack.isEmpty()) {
					this.player.drop(itemStack, true, false);
					inv.setItem(i, ItemStack.EMPTY);
				}
			}
		}
	}

	@Inject(
			method = "isEmpty",
			at = @At("HEAD"),
			cancellable = true
	)
	private void utilitybelt$patchIsEmpty(CallbackInfoReturnable<Boolean> cir) {
		ItemStack belt = UtilityBeltItem.getBelt(this.player);
		if (belt != null) {
			UtilityBeltInventory inv = StateManager.getServerInstance().getInventory(this.player);

			for (int i = 0; i < inv.getContainerSize(); i++) {
				if (!inv.getItem(i).isEmpty()) {
					cir.setReturnValue(false);
				}
			}
		}
	}
}
