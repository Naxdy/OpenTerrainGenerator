package com.khorn.terraincontrol.forge.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraft.client.gui.GuiWorldEdit;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.GuiOldSaveLoadConfirm;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.StartupQuery;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.gui.GuiListExtended;;

@SideOnly(Side.CLIENT)
public class TXGuiListWorldSelectionEntry implements GuiListExtended.IGuiListEntry
{
	// Taken from net.minecraft.client.gui.GuiListWorldSelectionEntry. Only added getSelectedWorldName().
	
	public String getSelectedWorldName()
	{
		return this.worldSummary != null ? this.worldSummary.getFileName() : null;
	}
	
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    private static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
    private static final ResourceLocation ICON_OVERLAY_LOCATION = new ResourceLocation("textures/gui/world_selection.png");
    private final Minecraft client;
    private final TXGuiWorldSelection worldSelScreen;
    private final WorldSummary worldSummary;
    private final ResourceLocation iconLocation;
    private final TXGuiListWorldSelection containingListSel;
    private File iconFile;
    private DynamicTexture icon;
    private long lastClickTime;

    public TXGuiListWorldSelectionEntry(TXGuiListWorldSelection listWorldSelIn, WorldSummary p_i46591_2_, ISaveFormat p_i46591_3_)
    {
        this.containingListSel = listWorldSelIn;
        this.worldSelScreen = listWorldSelIn.getGuiWorldSelection();
        this.worldSummary = p_i46591_2_;
        this.client = Minecraft.getMinecraft();
        this.iconLocation = new ResourceLocation("worlds/" + p_i46591_2_.getFileName() + "/icon");
        this.iconFile = p_i46591_3_.getFile(p_i46591_2_.getFileName(), "icon.png");

        if (!this.iconFile.isFile())
        {
            this.iconFile = null;
        }

        this.loadServerIcon();
    }

    @Override
    public void updatePosition(int p_192633_1_, int p_192633_2_, int p_192633_3_, float p_192633_4_) {

    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float particleTicks)
    {
        String s = this.worldSummary.getDisplayName();
        String s1 = this.worldSummary.getFileName() + " (" + DATE_FORMAT.format(new Date(this.worldSummary.getLastTimePlayed())) + ")";
        String s2 = "";

        if (StringUtils.isEmpty(s))
        {
            s = I18n.format("selectWorld.world", new Object[0]) + " " + (slotIndex + 1);
        }

        if (this.worldSummary.requiresConversion())
        {
            s2 = I18n.format("selectWorld.conversion", new Object[0]) + " " + s2;
        }
        else
        {
            s2 = I18n.format("gameMode." + this.worldSummary.getEnumGameType().getName(), new Object[0]);

            if (this.worldSummary.isHardcoreModeEnabled())
            {
                s2 = TextFormatting.DARK_RED + I18n.format("gameMode.hardcore", new Object[0]) + TextFormatting.RESET;
            }

            if (this.worldSummary.getCheatsEnabled())
            {
                s2 = s2 + ", " + I18n.format("selectWorld.cheats", new Object[0]);
            }

            String s3 = this.worldSummary.getVersionName();

            if (this.worldSummary.markVersionInList())
            {
                if (this.worldSummary.askToOpenWorld())
                {
                    s2 = s2 + ", " + I18n.format("selectWorld.version", new Object[0]) + " " + TextFormatting.RED + s3 + TextFormatting.RESET;
                }
                else
                {
                    s2 = s2 + ", " + I18n.format("selectWorld.version", new Object[0]) + " " + TextFormatting.ITALIC + s3 + TextFormatting.RESET;
                }
            }
            else
            {
                s2 = s2 + ", " + I18n.format("selectWorld.version", new Object[0]) + " " + s3;
            }
        }

        this.client.fontRenderer.drawString(s, x + 32 + 3, y + 1, 16777215);
        this.client.fontRenderer.drawString(s1, x + 32 + 3, y + this.client.fontRenderer.FONT_HEIGHT + 3, 8421504);
        this.client.fontRenderer.drawString(s2, x + 32 + 3, y + this.client.fontRenderer.FONT_HEIGHT + this.client.fontRenderer.FONT_HEIGHT + 3, 8421504);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.client.getTextureManager().bindTexture(this.icon != null ? this.iconLocation : ICON_MISSING);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 32, 32, 32.0F, 32.0F);
        GlStateManager.disableBlend();

        if (this.client.gameSettings.touchscreen || isSelected)
        {
            this.client.getTextureManager().bindTexture(ICON_OVERLAY_LOCATION);
            Gui.drawRect(x, y, x + 32, y + 32, -1601138544);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            int j = mouseX - x;
            int i = j < 32 ? 32 : 0;

            if (this.worldSummary.markVersionInList())
            {
                Gui.drawModalRectWithCustomSizedTexture(x, y, 32.0F, (float)i, 32, 32, 256.0F, 256.0F);

                if (this.worldSummary.askToOpenWorld())
                {
                    Gui.drawModalRectWithCustomSizedTexture(x, y, 96.0F, (float)i, 32, 32, 256.0F, 256.0F);

                    if (j < 32)
                    {
                        this.worldSelScreen.setVersionTooltip(TextFormatting.RED + I18n.format("selectWorld.tooltip.fromNewerVersion1", new Object[0]) + "\n" + TextFormatting.RED + I18n.format("selectWorld.tooltip.fromNewerVersion2", new Object[0]));
                    }
                }
                else
                {
                    Gui.drawModalRectWithCustomSizedTexture(x, y, 64.0F, (float)i, 32, 32, 256.0F, 256.0F);

                    if (j < 32)
                    {
                        this.worldSelScreen.setVersionTooltip(TextFormatting.GOLD + I18n.format("selectWorld.tooltip.snapshot1", new Object[0]) + "\n" + TextFormatting.GOLD + I18n.format("selectWorld.tooltip.snapshot2", new Object[0]));
                    }
                }
            }
            else
            {
                Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, (float)i, 32, 32, 256.0F, 256.0F);
            }
        }
    }

    /**
     * Called when the mouse is clicked within this entry. Returning true means that something within this entry was
     * clicked and the list should not be dragged.
     */
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY)
    {
        this.containingListSel.selectWorld(slotIndex);

        if (relativeX <= 32 && relativeX < 32)
        {
            this.joinWorld();
            return true;
        }
        else if (Minecraft.getSystemTime() - this.lastClickTime < 250L)
        {
            this.joinWorld();
            return true;
        }
        else
        {
            this.lastClickTime = Minecraft.getSystemTime();
            return false;
        }
    }

    public void joinWorld()
    {
        if (this.worldSummary.askToOpenWorld())
        {
            this.client.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback()
            {
                public void confirmClicked(boolean result, int id)
                {
                    if (result)
                    {
                        TXGuiListWorldSelectionEntry.this.loadWorld();
                    }
                    else
                    {
                    	TXGuiListWorldSelectionEntry.this.client.displayGuiScreen(TXGuiListWorldSelectionEntry.this.worldSelScreen);
                    }
                }
            }, I18n.format("selectWorld.versionQuestion", new Object[0]), I18n.format("selectWorld.versionWarning", new Object[] {this.worldSummary.getVersionName()}), I18n.format("selectWorld.versionJoinButton", new Object[0]), I18n.format("gui.cancel", new Object[0]), 0));
        }
        else
        {
            this.loadWorld();
        }
    }

    public void deleteWorld()
    {
        this.client.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback()
        {
            public void confirmClicked(boolean result, int id)
            {
                if (result)
                {
                	TXGuiListWorldSelectionEntry.this.client.displayGuiScreen(new GuiScreenWorking());
                    ISaveFormat isaveformat = TXGuiListWorldSelectionEntry.this.client.getSaveLoader();
                    isaveformat.flushCache();
                    isaveformat.deleteWorldDirectory(TXGuiListWorldSelectionEntry.this.worldSummary.getFileName());
                    TXGuiListWorldSelectionEntry.this.containingListSel.refreshList();
                }

                TXGuiListWorldSelectionEntry.this.client.displayGuiScreen(TXGuiListWorldSelectionEntry.this.worldSelScreen);
            }
        }, I18n.format("selectWorld.deleteQuestion", new Object[0]), "\'" + this.worldSummary.getDisplayName() + "\' " + I18n.format("selectWorld.deleteWarning", new Object[0]), I18n.format("selectWorld.deleteButton", new Object[0]), I18n.format("gui.cancel", new Object[0]), 0));
    }

    public void editWorld()
    {
        this.client.displayGuiScreen(new GuiWorldEdit(this.worldSelScreen, this.worldSummary.getFileName()));
    }

    public void recreateWorld()
    {
        this.client.displayGuiScreen(new GuiScreenWorking());
        GuiCreateWorld guicreateworld = new GuiCreateWorld(this.worldSelScreen);
        ISaveHandler isavehandler = this.client.getSaveLoader().getSaveLoader(this.worldSummary.getFileName(), false);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();
        isavehandler.flush();
        guicreateworld.recreateFromExistingWorld(worldinfo);
        this.client.displayGuiScreen(guicreateworld);
    }

    private void loadWorld()
    {
        this.client.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        if (this.client.getSaveLoader().canLoadWorld(this.worldSummary.getFileName()))
        {
            tryLoadExistingWorld(net.minecraftforge.fml.client.FMLClientHandler.instance(), worldSelScreen, this.worldSummary);
        }
    }

    // net.minecraftforge.fml.client.FMLClientHandler.tryLoadExistingWorld
    public void tryLoadExistingWorld(FMLClientHandler clientHandler, TXGuiWorldSelection selectWorldGUI, WorldSummary comparator)
    {
        File dir = new File(clientHandler.getSavesDir(), comparator.getFileName());
        NBTTagCompound leveldat;
        try
        {
            leveldat = CompressedStreamTools.readCompressed(new FileInputStream(new File(dir, "level.dat")));
        }
        catch (Exception e)
        {
            try
            {
                leveldat = CompressedStreamTools.readCompressed(new FileInputStream(new File(dir, "level.dat_old")));
            }
            catch (Exception e1)
            {
                FMLLog.warning("There appears to be a problem loading the save %s, both level files are unreadable.", comparator.getFileName());
                return;
            }
        }
        NBTTagCompound fmlData = leveldat.getCompoundTag("FML");
        if (fmlData.hasKey("ModItemData"))
        {
        	clientHandler.showGuiScreen(new GuiOldSaveLoadConfirm(comparator.getFileName(), comparator.getDisplayName(), selectWorldGUI));
        }
        else
        {
            try
            {
            	clientHandler.getClient().launchIntegratedServer(comparator.getFileName(), comparator.getDisplayName(), null);
            }
            catch (StartupQuery.AbortedException e)
            {
                // ignore
            }
        }
    }
    
    private void loadServerIcon()
    {
        boolean flag = this.iconFile != null && this.iconFile.isFile();

        if (flag)
        {
            BufferedImage bufferedimage;

            try
            {
                bufferedimage = ImageIO.read(this.iconFile);
                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
            }
            catch (Throwable throwable)
            {
                LOGGER.error("Invalid icon for world {}", new Object[] {this.worldSummary.getFileName(), throwable});
                this.iconFile = null;
                return;
            }

            if (this.icon == null)
            {
                this.icon = new DynamicTexture(bufferedimage.getWidth(), bufferedimage.getHeight());
                this.client.getTextureManager().loadTexture(this.iconLocation, this.icon);
            }

            bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), this.icon.getTextureData(), 0, bufferedimage.getWidth());
            this.icon.updateDynamicTexture();
        }
        else if (!flag)
        {
            this.client.getTextureManager().deleteTexture(this.iconLocation);
            this.icon = null;
        }
    }

    /**
     * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY
     */
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY)
    {
    }

    public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_)
    {
    }
}