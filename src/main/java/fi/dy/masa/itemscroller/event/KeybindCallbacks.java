package fi.dy.masa.itemscroller.event;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.gui.GuiConfigs;
import fi.dy.masa.itemscroller.mixin.recipe.IMixinCraftingResultSlot;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.util.*;

public class KeybindCallbacks implements IHotkeyCallback
{
    private static final KeybindCallbacks INSTANCE = new KeybindCallbacks();
    protected int massCraftTicker;
    private boolean recipeBookClicks = false;
    public static KeybindCallbacks getInstance()
    {
        return INSTANCE;
    }

    private KeybindCallbacks()
    {
    }

    public void setCallbacks()
    {
        for (ConfigHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            hotkey.getKeybind().setCallback(this);
        }
    }

    public boolean functionalityEnabled()
    {
        return Configs.Generic.MOD_MAIN_TOGGLE.getBooleanValue();
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key)
    {
        if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
        {
            ClickPacketBuffer.setShouldBufferClickPackets(true);
        }

        boolean cancel = this.onKeyActionImpl(action, key);

        ClickPacketBuffer.setShouldBufferClickPackets(false);

        return cancel;
    }

    private boolean onKeyActionImpl(KeyAction action, IKeybind key)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null)
        {
            return false;
        }

        if (key == Hotkeys.TOGGLE_MOD_ON_OFF.getKeybind())
        {
            Configs.Generic.MOD_MAIN_TOGGLE.toggleBooleanValue();
            String msg = this.functionalityEnabled() ? "itemscroller.message.toggled_mod_on" : "itemscroller.message.toggled_mod_off";
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, msg);
            return true;
        }
        else if (key == Hotkeys.OPEN_CONFIG_GUI.getKeybind())
        {
            GuiBase.openGui(new GuiConfigs());
            return true;
        }

        if (this.functionalityEnabled() == false ||
            (GuiUtils.getCurrentScreen() instanceof AbstractContainerScreen) == false ||
            Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName()))
        {
            return false;
        }

        AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) GuiUtils.getCurrentScreen();
        Slot slot = AccessorUtils.getSlotUnderMouse(gui);
        RecipeStorage recipes = RecipeStorage.getInstance();
        MoveAction moveAction = InputUtils.getDragMoveAction(key);

        if (slot != null)
        {
            if (moveAction != MoveAction.NONE)
            {
                final int mouseX = fi.dy.masa.malilib.util.InputUtils.getMouseX();
                final int mouseY = fi.dy.masa.malilib.util.InputUtils.getMouseY();
                return InventoryUtils.dragMoveItems(gui, moveAction, mouseX, mouseY, true);
            }
            else if (key == Hotkeys.DROP_ALL_MATCHING.getKeybind())
            {
                if (Configs.Toggles.DROP_MATCHING.getBooleanValue() &&
                    Configs.GUI_BLACKLIST.contains(gui.getClass().getName()) == false &&
                    slot.hasItem())
                {
                    InventoryUtils.dropStacks(gui, slot.getItem(), slot, true);
                    return true;
                }
            }
        }

        if (key == Hotkeys.MOVE_CRAFT_RESULTS.getKeybind())
        {
            InventoryUtils.moveAllCraftingResultsToOtherInventory(recipes.getSelectedRecipe(), gui);
            return true;
        }
        else if (key == Hotkeys.VILLAGER_TRADE_FAVORITES.getKeybind())
        {
            return InventoryUtils.villagerTradeEverythingPossibleWithAllFavoritedTrades();
        }
        else if (key == Hotkeys.SLOT_DEBUG.getKeybind())
        {
            if (slot != null)
            {
                InventoryUtils.debugPrintSlotInfo(gui, slot);
            }
            else
            {
                ItemScroller.LOGGER.info("GUI class: {}", gui.getClass().getName());
            }

            return true;
        }
        else if (key == Hotkeys.SORT_INVENTORY.getKeybind())
        {
            if (Configs.Generic.SORT_INVENTORY_TOGGLE.getBooleanValue())
            {
                InventoryUtils.sortInventory(gui);
                return true;
            }
        }

        return false;
    }

//    public void onPacket(ScreenHandlerSlotUpdateS2CPacket packet)
//    {
////        var mc = MinecraftClient.getInstance();
//    }
}
