package net.minecraft.src;

public class AddonVillagerHandler extends FCAddOn {
	private static AddonVillagerHandler instance;
	
	static {
		EntityList.addMapping(AddonEntityVillagerFarmer.class, "addonVillagerFarmer", 600, 5651507, 12422002);
		EntityList.addMapping(AddonEntityVillagerLibrarian.class, "addonVillagerLibrarian", 601, 14342874, 16179719);
		EntityList.addMapping(AddonEntityVillagerPriest.class, "addonVillagerPriest", 602, 8470879, 12422002);
		EntityList.addMapping(AddonEntityVillagerBlacksmith.class, "addonVillagerBlacksmith", 603, 4802889, 12422002);
		EntityList.addMapping(AddonEntityVillagerButcher.class, "addonVillagerButcher", 604, 11447982, 12422002);
		
		EntityList.entityEggs.remove(120);
	}

	@Override
	public void Initialize() {
		FCAddOnHandler.LogMessage("Initializing Villager Handler...");
	}
	
	public static AddonVillagerHandler getInstance() {
		if (instance == null) {
			instance = new AddonVillagerHandler();
		}
		
		return instance;
	}

	public String GetLanguageFilePrefix()
	{
		return "AddonVH";
	}
}