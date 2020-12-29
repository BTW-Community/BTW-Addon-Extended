package net.minecraft.src;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;

public class DawnNetClientHandler extends NetClientHandler {
    /** Reference to the Minecraft object. */
    private Minecraft mc;
    private WorldClient worldClient;
    
	public DawnNetClientHandler(Minecraft par1Minecraft, IntegratedServer par2IntegratedServer) throws IOException {
		super(par1Minecraft, par2IntegratedServer);
        this.mc = par1Minecraft;
	}

	public DawnNetClientHandler(Minecraft par1Minecraft, String par2Str, int par3, GuiScreen par4GuiScreen) throws IOException {
		super(par1Minecraft, par2Str, par3, par4GuiScreen);
        this.mc = par1Minecraft;
	}

	public DawnNetClientHandler(Minecraft par1Minecraft, String par2Str, int par3) throws IOException {
		super(par1Minecraft, par2Str, par3);
        this.mc = par1Minecraft;
	}

    /**
     * sets netManager and worldClient to null
     */
    public void cleanup()
    {
    	super.cleanup();
        this.worldClient = null;
    }

    public void handleCustomPayload(Packet250CustomPayload par1Packet250CustomPayload)
    {
        if ("MC|TPack".equals(par1Packet250CustomPayload.channel))
        {
            String[] var2 = (new String(par1Packet250CustomPayload.data)).split("\u0000");
            String var3 = var2[0];

            if (var2[1].equals("16"))
            {
                if (this.mc.texturePackList.getAcceptsTextures())
                {
                    this.mc.texturePackList.requestDownloadOfTexture(var3);
                }
                else if (this.mc.texturePackList.func_77300_f())
                {
                    this.mc.displayGuiScreen(new GuiYesNo(new NetClientWebTextures(this, var3), StringTranslate.getInstance().translateKey("multiplayer.texturePrompt.line1"), StringTranslate.getInstance().translateKey("multiplayer.texturePrompt.line2"), 0));
                }
            }
        }
        else if ("MC|TrList".equals(par1Packet250CustomPayload.channel))
        {
            DataInputStream var8 = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));

            try
            {
                int var9 = var8.readInt();
                GuiScreen var4 = this.mc.currentScreen;

                if (var4 != null && var4 instanceof GuiMerchant && var9 == this.mc.thePlayer.openContainer.windowId)
                {
                    IMerchant var5 = ((GuiMerchant)var4).getIMerchant();
                    MerchantRecipeList var6 = MerchantRecipeList.readRecipiesFromStream(var8);
                    var5.setRecipes(var6);
                }
            }
            catch (IOException var7)
            {
                var7.printStackTrace();
            }
        }
        else if (!DawnAddonHandler.interceptCustomClientPacket(this.mc, par1Packet250CustomPayload))
        {
            FCAddOnHandler.ClientCustomPacketReceived(this.mc, par1Packet250CustomPayload);
        }
    }
}
