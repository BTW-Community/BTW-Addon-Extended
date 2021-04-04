package net.minecraft.src;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class FCEntityVillager extends EntityVillager
{
    public static int m_iNumProfessionTypes = 5;
    protected static final int m_iInLoveDataWatcherID = 22;
    protected static final int m_iTradeLevelDataWatcherID = 23;
    protected static final int m_iTradeExperienceDataWatcherID = 25;
    protected static final int m_iDirtyPeasantDataWatcherID = 26;
    protected int m_iAIFullTickCountdown;
    protected int m_iUpdateTradesCountdown;
    
    //ADDON EXTENDED
    public static final int professionIDFarmer = 0;
    public static final int professionIDLibrarian = 1;
    public static final int professionIDPriest = 2;
    public static final int professionIDBlacksmith = 3;
    public static final int professionIDButcher = 4;
    
    public static Map<Integer, Class> professionMap = new HashMap<Integer, Class>();
    
    private NBTTagCompound tagForFormatConversion;

    static {
    	professionMap.put(professionIDFarmer, AddonEntityVillagerFarmer.class);
    	professionMap.put(professionIDLibrarian, AddonEntityVillagerLibrarian.class);
    	professionMap.put(professionIDPriest, AddonEntityVillagerPriest.class);
    	professionMap.put(professionIDBlacksmith, AddonEntityVillagerBlacksmith.class);
    	professionMap.put(professionIDButcher, AddonEntityVillagerButcher.class);
    }
    //ADDON EXTENDED
    
    public FCEntityVillager(World var1)
    {
        this(var1, 0);
    }

    public FCEntityVillager(World var1, int var2)
    {
        super(var1, var2);
        this.tasks.RemoveAllTasksOfClass(EntityAIAvoidEntity.class);
        this.tasks.addTask(1, new EntityAIAvoidEntity(this, FCEntityZombie.class, 8.0F, 0.3F, 0.35F));
        this.tasks.addTask(1, new EntityAIAvoidEntity(this, FCEntityWolf.class, 8.0F, 0.3F, 0.35F));
        this.tasks.RemoveAllTasksOfClass(EntityAIVillagerMate.class);
        this.tasks.addTask(1, new FCEntityAIVillagerMate(this));
        this.tasks.addTask(2, new EntityAITempt(this, 0.3F, Item.diamond.itemID, false));
        this.experienceValue = 50;
        this.m_iUpdateTradesCountdown = 0;
        this.m_iAIFullTickCountdown = 0;
    }
    
    public static FCEntityVillager createVillager(World world) {
    	return createVillagerFromProfession(world, 0);
    }
    
    public static FCEntityVillager createVillagerFromProfession(World world, int profession) {
    	Class villagerClass = professionMap.get(profession);
    	
    	try {
			FCEntityVillager villager = (FCEntityVillager) villagerClass.getConstructor(World.class).newInstance(world);
			
			return villager;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
    	
    	return null;
    }

    /**
     * main AI tick function, replaces updateEntityActionState
     */
    protected void updateAITick()
    {
        --this.m_iAIFullTickCountdown;

        if (this.m_iAIFullTickCountdown <= 0)
        {
            this.m_iAIFullTickCountdown = 70 + this.rand.nextInt(50);
            this.worldObj.villageCollectionObj.addVillagerPosition(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ));
            this.villageObj = this.worldObj.villageCollectionObj.findNearestVillage(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ), 32);

            if (this.villageObj == null)
            {
                this.detachHome();
            }
            else
            {
                ChunkCoordinates var1 = this.villageObj.getCenter();
                this.setHomeArea(var1.posX, var1.posY, var1.posZ, (int)((float)this.villageObj.getVillageRadius() * 0.6F));
            }
        }

        if (!this.isTrading())
        {
            if (this.GetCurrentTradeLevel() == 0)
            {
                this.SetTradeLevel(1);
                this.buyingList = null;
                this.m_iUpdateTradesCountdown = 0;
                this.CheckForNewTrades(1);
            }
            else if (this.m_iUpdateTradesCountdown > 0)
            {
                --this.m_iUpdateTradesCountdown;

                if (this.m_iUpdateTradesCountdown <= 0)
                {
                    Iterator var3 = this.buyingList.iterator();

                    while (var3.hasNext())
                    {
                        MerchantRecipe var2 = (MerchantRecipe)var3.next();

                        if (var2.func_82784_g())
                        {
                            var3.remove();
                        }
                    }

                    int var4 = this.GetCurrentMaxNumTrades();

                    if (this.buyingList.size() < var4)
                    {
                        this.CheckForNewTrades(var4 - this.buyingList.size());
                        this.worldObj.setEntityState(this, (byte)14);
                        this.addPotionEffect(new PotionEffect(Potion.regeneration.id, 200, 0));
                    }
                }
            }
            else
            {
                this.m_iUpdateTradesCountdown = 600 + this.rand.nextInt(600);
            }
        }
    }

    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    public boolean interact(EntityPlayer var1)
    {
        return this.CustomInteract(var1) ? true : (this.GetInLove() > 0 ? this.EntityAgeableInteract(var1) : super.interact(var1));
    }

    protected void entityInit()
    {
        super.entityInit();
        
    	//Dirty hacks to maintain compatibility with older saves
    	if (this.getClass().equals(FCEntityVillager.class)) {
    		FCEntityVillager villager = createVillagerFromProfession(this.worldObj, this.getProfession());
    		villager.readEntityFromNBT(tagForFormatConversion);
    		this.worldObj.spawnEntityInWorld(villager);
    		
    		this.setDead();
    		return;
    	}
    	
        this.dataWatcher.addObject(22, 0);
        this.dataWatcher.addObject(23, 0);
        this.dataWatcher.addObject(25, 0);
        this.dataWatcher.addObject(26, 0);
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound var1)
    {
        super.writeEntityToNBT(var1);
        var1.setInteger("FCInLove", this.GetInLove());
        var1.setInteger("FCTradeLevel", this.GetCurrentTradeLevel());
        var1.setInteger("FCTradeXP", this.GetCurrentTradeXP());
        var1.setInteger("FCDirty", this.GetDirtyPeasant());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound var1)
    {
        super.readEntityFromNBT(var1);

        if (var1.hasKey("FCInLove"))
        {
            this.SetInLove(var1.getInteger("FCInLove"));
        }

        if (var1.hasKey("FCTradeLevel"))
        {
            this.SetTradeLevel(var1.getInteger("FCTradeLevel"));
        }

        if (var1.hasKey("FCTradeXP"))
        {
            this.SetTradeExperience(var1.getInteger("FCTradeXP"));
        }

        if (var1.hasKey("FCDirty"))
        {
            this.SetDirtyPeasant(var1.getInteger("FCDirty"));
        }

        this.CheckForInvalidTrades();
        
        this.tagForFormatConversion = var1;
    }

    public void setRevengeTarget(EntityLiving var1)
    {
    	if (var1 != null)
        {
            this.isMating = true;

            if (this.villageObj != null)
            {
                this.villageObj.addOrRenewAgressor(var1);
            }

            if (this.isEntityAlive())
            {
                this.worldObj.setEntityState(this, (byte)13);
            }
        }
        else
        {
            this.isMating = false;
        }
    }

    public void useRecipe(MerchantRecipe var1)
    {
        var1.incrementToolUses();
        this.m_iUpdateTradesCountdown = 10;

        if (var1.getItemToBuy().itemID == FCBetterThanWolves.fcCompanionCube.blockID)
        {
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "mob.wolf.hurt", 5.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
        }
        else if (var1.getItemToBuy().itemID == FCBetterThanWolves.fcBlockLightningRod.blockID)
        {
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.classic_hurt", 1.0F, this.getSoundPitch() * 2.0F);
        }
        else if (var1.getItemToBuy().itemID == FCBetterThanWolves.fcItemSoap.itemID)
        {
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "mob.slime.attack", 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
            this.SetDirtyPeasant(0);
        }
        else if (var1.getItemToSell().itemID == FCBetterThanWolves.fcAnvil.blockID)
        {
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.anvil_land", 0.3F, this.rand.nextFloat() * 0.1F + 0.9F);
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "ambient.cave.cave4", 0.5F, this.rand.nextFloat() * 0.05F + 0.5F);
        }

        if (this.worldObj.getWorldInfo().getGameType() != EnumGameType.CREATIVE)
        {
            int var2;

            if (var1.m_iTradeLevel < 0)
            {
                var2 = this.GetCurrentTradeLevel();

                if (var2 < 5 && this.GetCurrentTradeXP() == this.GetCurrentTradeMaxXP() && this.GetCurrentTradeLevel() == -var1.m_iTradeLevel)
                {
                    ++var2;
                    this.SetTradeLevel(var2);
                    this.SetTradeExperience(0);

                    if (this.getProfession() == 2 && this.GetCurrentTradeLevel() >= 5)
                    {
                        this.worldObj.playSoundAtEntity(this, "mob.enderdragon.growl", 1.0F, 0.5F);
                        this.worldObj.playSoundAtEntity(this, "ambient.weather.thunder", 1.0F, this.rand.nextFloat() * 0.4F + 0.8F);
                        this.worldObj.playSoundAtEntity(this, "random.levelup", 0.75F + this.rand.nextFloat() * 0.25F, 0.5F);
                    }
                    else
                    {
                        this.worldObj.playSoundAtEntity(this, "random.levelup", 0.5F + this.rand.nextFloat() * 0.25F, 1.5F);
                    }
                }
            }
            else if (var1.m_iTradeLevel >= this.GetCurrentTradeLevel())
            {
                var2 = this.GetCurrentTradeXP() + 1;
                int var3 = this.GetCurrentTradeMaxXP();

                if (var2 > var3)
                {
                    var2 = var3;
                }

                this.SetTradeExperience(var2);
            }
        }
    }

    public MerchantRecipeList getRecipes(EntityPlayer var1)
    {
        if (this.buyingList == null)
        {
            this.CheckForNewTrades(1);
        }

        return this.buyingList;
    }

    /**
     * Initialize this creature.
     */
    public void initCreature()
    {
        this.setProfession(this.worldObj.rand.nextInt(m_iNumProfessionTypes));
    }

    /**
     * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
     * use this to react to sunlight and start to burn.
     */
    public void onLivingUpdate()
    {
        super.onLivingUpdate();

        if (!this.worldObj.isRemote)
        {
            if (this.isEntityAlive())
            {
                this.CheckForLooseMilk();
            }
        }
        else
        {
            this.UpdateStatusParticles();
        }
    }

    /**
     * Drop 0-2 items of this living's type. @param par1 - Whether this entity has recently been hit by a player. @param
     * par2 - Level of Looting used to kill this mob.
     */
    protected void dropFewItems(boolean var1, int var2)
    {
        if (!this.HasHeadCrabbedSquid())
        {
            int var3 = FCBetterThanWolves.fcItemRawMysteryMeat.itemID;

            if (this.isBurning())
            {
                var3 = FCBetterThanWolves.fcItemCookedMysteryMeat.itemID;
            }

            int var4 = this.rand.nextInt(2) + 1 + this.rand.nextInt(1 + var2);

            for (int var5 = 0; var5 < var4; ++var5)
            {
                this.dropItem(var3, 1);
            }
        }
    }

    /**
     * Gets the pitch of living sounds in living entities.
     */
    protected float getSoundPitch()
    {
        float var1 = super.getSoundPitch();

        if (this.IsPossessed() || this.getProfession() == 2 && this.GetCurrentTradeLevel() == 5)
        {
            var1 *= 0.6F;
        }

        return var1;
    }

    protected boolean GetCanCreatureTypeBePossessed()
    {
        return true;
    }

    protected void OnFullPossession()
    {
        this.worldObj.playAuxSFX(2260, MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ), 0);
        this.setDead();
        FCEntityWitch var1 = new FCEntityWitch(this.worldObj);
        var1.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
        var1.renderYawOffset = this.renderYawOffset;
        var1.SetPersistent(true);
        this.worldObj.spawnEntityInWorld(var1);
    }

    public boolean IsValidZombieSecondaryTarget(EntityZombie var1)
    {
        return true;
    }

    public boolean IsSecondaryTargetForSquid()
    {
        return true;
    }

    /**
     * Returns the Y offset from the entity's position for any entity riding this one.
     */
    public double getMountedYOffset()
    {
        return (double)this.height;
    }

    public FCEntityVillager func_90012_b(EntityAgeable var1)
    {
        FCEntityVillager var2 = createVillager(this.worldObj);
        var2.initCreature();
        return var2;
    }

    protected void CheckForNewTrades(int availableTrades)
    {
        if (availableTrades > 0)
        {
            if (this.GetCurrentTradeMaxXP() == this.GetCurrentTradeXP() && this.CheckForLevelUpTrade())
            {
                --availableTrades;

                if (availableTrades <= 0)
                {
                    return;
                }
            }

            availableTrades = this.checkForProfessionMandatoryTrades(availableTrades, this.GetCurrentTradeLevel());

            if (availableTrades > 0)
            {
                MerchantRecipeList recipeList = new MerchantRecipeList();

                this.checkForProfessionTrades(recipeList);

                if (recipeList.isEmpty())
                {
                    recipeList.add(this.getProfessionDefaultTrade());
                }
                else
                {
                    Collections.shuffle(recipeList);
                }

                if (this.buyingList == null)
                {
                    this.buyingList = new MerchantRecipeList();
                }

                for (int i = 0; i < availableTrades && i < recipeList.size(); ++i)
                {
                    this.buyingList.addToListWithCheck((MerchantRecipe)recipeList.get(i));
                }
            }
        }
    }

    private boolean CheckForLevelUpTrade()
    {
        MerchantRecipe recipe = this.getProfessionLevelUpTrade(this.GetCurrentTradeLevel());

        if (recipe != null && !this.DoesRecipeListAlreadyContainRecipe(recipe))
        {
            this.buyingList.add(recipe);
            return true;
        }
        else
        {
            return false;
        }
    }

    protected boolean AttemptToAddTradeToBuyingList(MerchantRecipe var1)
    {
        if (var1 != null && !this.DoesRecipeListAlreadyContainRecipe(var1))
        {
            this.buyingList.add(var1);
            return true;
        }
        else
        {
            return false;
        }
    }
    
    //ADDON EXTENDED
    /**
     * Check for all trades available to the profession
     * @param recipeList
     */
    protected abstract void checkForProfessionTrades(MerchantRecipeList recipeList);
    
    /**
     * Return the level up trade based on current villager level
     * @return The level up trade
     */
    protected abstract MerchantRecipe getProfessionLevelUpTrade(int level);
    
    /**
     * Return the default trade to use if no other trade is found
     * @return The default trade
     */
    protected abstract MerchantRecipe getProfessionDefaultTrade();
    
    /**
     * Adds the mandatory trades for the profession at the current level
     * Use AttemptToAddTradeToBuyingList() for each recipe and decrement remaining available recipes on success
     * @param availableTrades
     * @return
     */
    protected int checkForProfessionMandatoryTrades(int availableTrades, int level) {
    	return availableTrades;
    }
    
    /**
     * Used to clear any invalid trades e.g. that may be left over from previous versions
     * @param trade The trade to check
     * @return Whether the trade was invalid and should be removed
     */
    protected boolean isInvalidProfessionTrade(MerchantRecipe trade) {
    	return false;
    }
    //ADDON EXTENDED

    private float ComputeAdjustedChanceOfTrade(float var1, int var2)
    {
        float var3 = 1.0F;
        int var4 = this.GetCurrentTradeLevel();

        if (var2 > 0 && var4 > 0)
        {
            var3 = (float)var2 / (float)var4;
        }

        return var1 * var3;
    }

    protected void CheckForWishToBuyMultipleItemsTrade(MerchantRecipeList recipeList, int tradeID, float chance, int minCount, int maxCount, int tradeLevel) {
        this.CheckForWishToBuyMultipleItemsTrade(recipeList, tradeID, 0, chance, minCount, maxCount, tradeLevel);
    }

    protected void CheckForWishToBuyMultipleItemsTrade(MerchantRecipeList recipeList, int tradeID, int tradeDamage, float chance, int minCount, int maxCount, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(chance, tradeLevel)) {
            int tradeCount = MathHelper.getRandomIntegerInRange(this.rand, minCount, maxCount);
            this.AddWishToBuyTradeToList(recipeList, tradeID, tradeCount, tradeDamage, 1, tradeLevel);
        }
    }

    protected void CheckForWishToBuySingleItemTrade(MerchantRecipeList recipeList, int tradeID, float chance, int minCost, int maxCost, int tradeLevel) {
        this.CheckForWishToBuySingleItemTrade(recipeList, tradeID, 0, chance, minCost, maxCost, tradeLevel);
    }

    protected void CheckForWishToBuySingleItemTrade(MerchantRecipeList recipeList, int tradeID, int tradeDamage, float chance, int minCost, int maxCost, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(chance, tradeLevel)) {
            int cost = MathHelper.getRandomIntegerInRange(this.rand, minCost, maxCost);
            this.AddWishToBuyTradeToList(recipeList, tradeID, 1, tradeDamage, cost, tradeLevel);
        }
    }

    private void AddWishToBuyTradeToList(MerchantRecipeList recipeList, int tradeID, int tradeCount, int tradeDamage, int cost, int tradeLevel) {
        ItemStack emeraldStack = new ItemStack(Item.emerald.itemID, cost, 0);
        ItemStack tradeStack = new ItemStack(tradeID, tradeCount, tradeDamage);
        recipeList.add(new MerchantRecipe(tradeStack, emeraldStack, tradeLevel));
    }

    protected void CheckForWishToSellSingleItemTrade(MerchantRecipeList recipeList, int tradeID, float chance, int minCost, int maxCost, int tradeLevel) {
        this.CheckForWishToSellSingleItemTrade(recipeList, tradeID, 0, chance, minCost, maxCost, tradeLevel);
    }

    protected void CheckForWishToSellSingleItemTrade(MerchantRecipeList recipeList, int tradeID, int tradeDamage, float var4, int minCost, int maxCost, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(var4, tradeLevel)) {
            int cost = MathHelper.getRandomIntegerInRange(this.rand, minCost, maxCost);
            this.AddWishToSellTradeToList(recipeList, tradeID, 1, tradeDamage, cost, tradeLevel);
        }
    }

    protected void CheckForWishToSellMultipleItemsTrade(MerchantRecipeList recipeList, int tradeID, float chance, int minCount, int maxCount, int tradeLevel) {
        this.CheckForWishToSellMultipleItemsTrade(recipeList, tradeID, 0, chance, minCount, maxCount, tradeLevel);
    }

    protected void CheckForWishToSellMultipleItemsTrade(MerchantRecipeList recipeList, int tradeID, int tradeDamage, float chance, int minCount, int maxCount, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(chance, tradeLevel)) {
            int tradeCount = MathHelper.getRandomIntegerInRange(this.rand, minCount, maxCount);
            this.AddWishToSellTradeToList(recipeList, tradeID, tradeCount, tradeDamage, 1, tradeLevel);
        }
    }

    private void AddWishToSellTradeToList(MerchantRecipeList recipeList, int tradeID, int tradeCount, int tradeDamage, int cost, int tradeLevel) {
        ItemStack emeraldStack = new ItemStack(Item.emerald.itemID, cost, 0);
        ItemStack tradeStack = new ItemStack(tradeID, tradeCount, tradeDamage);
        recipeList.add(new MerchantRecipe(emeraldStack, tradeStack, tradeLevel));
    }

    protected void CheckForArcaneScrollTrade(MerchantRecipeList recipeList, int enchantID, float chance, int minCost, int maxCost, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(chance, tradeLevel)) {
            int cost = MathHelper.getRandomIntegerInRange(this.rand, minCost, maxCost);
            ItemStack var8 = new ItemStack(FCBetterThanWolves.fcItemArcaneScroll, 1, enchantID);
            recipeList.add(new MerchantRecipe(new ItemStack(Item.paper), new ItemStack(Item.emerald, cost), var8, tradeLevel));
        }
    }

    protected void CheckForItemEnchantmentForCostTrade(MerchantRecipeList recipeList, Item itemForEnchant, float chance, int minCost, int maxCost, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(chance, tradeLevel)) {
            int cost = MathHelper.getRandomIntegerInRange(this.rand, minCost, maxCost);
            recipeList.add(new MerchantRecipe(new ItemStack(itemForEnchant, 1, 0), new ItemStack(Item.emerald, cost, 0), EnchantmentHelper.addRandomEnchantment(this.rand, new ItemStack(itemForEnchant, 1, 0), 5 + this.rand.nextInt(15)), tradeLevel));
        }
    }

    protected void CheckForItemConversionForCostTrade(MerchantRecipeList recipeList, Item itemForConversion, Item convertedItem, float var4, int minCost, int maxCost, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(var4, tradeLevel)) {
            int cost = MathHelper.getRandomIntegerInRange(this.rand, minCost, maxCost);
            ItemStack stackForConversion = new ItemStack(itemForConversion);
            ItemStack convertedStack = new ItemStack(convertedItem);
            recipeList.add(new MerchantRecipe(stackForConversion, new ItemStack(Item.emerald, cost), convertedStack, tradeLevel));
        }
    }

    protected void CheckForSkullConversionForCostTrade(MerchantRecipeList recipeList, int skullMetaForConversion, int convertedSkullMeta, float chance, int minCost, int maxCost, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(chance, tradeLevel)) {
            int cost = MathHelper.getRandomIntegerInRange(this.rand, minCost, maxCost);
            ItemStack var9 = new ItemStack(Item.skull, 1, skullMetaForConversion);
            ItemStack var10 = new ItemStack(Item.skull, 1, convertedSkullMeta);
            recipeList.add(new MerchantRecipe(var9, new ItemStack(Item.emerald, cost), var10, tradeLevel));
        }
    }

    protected void CheckForComplexTrade(MerchantRecipeList recipeList, ItemStack buyStack1, ItemStack buyStack2, ItemStack sellStack, float chance, int tradeLevel) {
        if (this.GetCurrentTradeLevel() >= tradeLevel && this.rand.nextFloat() < this.ComputeAdjustedChanceOfTrade(chance, tradeLevel)) {
            recipeList.add(new MerchantRecipe(buyStack1, buyStack2, sellStack, tradeLevel));
        }
    }

    private boolean DoesRecipeListAlreadyContainRecipe(MerchantRecipe var1)
    {
        for (int var2 = 0; var2 < this.buyingList.size(); ++var2)
        {
            MerchantRecipe var3 = (MerchantRecipe)this.buyingList.get(var2);

            if (var1.hasSameIDsAs(var3))
            {
                return true;
            }
        }

        return false;
    }

    private boolean CustomInteract(EntityPlayer var1)
    {
        ItemStack var2 = var1.inventory.getCurrentItem();

        if (var2 != null && var2.getItem().itemID == Item.diamond.itemID && this.getGrowingAge() == 0 && this.GetInLove() == 0 && !this.IsPossessed())
        {
            if (!var1.capabilities.isCreativeMode)
            {
                --var2.stackSize;

                if (var2.stackSize <= 0)
                {
                    var1.inventory.setInventorySlotContents(var1.inventory.currentItem, (ItemStack)null);
                }
            }

            this.worldObj.playSoundAtEntity(this, "random.classic_hurt", 1.0F, this.getSoundPitch() * 2.0F);
            this.SetInLove(1);
            this.entityToAttack = null;
            return true;
        }
        else
        {
            return false;
        }
    }

    private void CheckForInvalidTrades() {
    	MerchantRecipe trade;
    	Iterator iterator = this.buyingList.iterator();

    	while (iterator.hasNext()) {
    		trade = (MerchantRecipe)iterator.next();

    		if (this.isInvalidProfessionTrade(trade)) {
    			iterator.remove();
    		}
    	}
    }

    private void UpdateStatusParticles()
    {
        if (this.getProfession() == 2 && this.GetCurrentTradeLevel() >= 5)
        {
            this.worldObj.spawnParticle("portal", this.posX + (this.rand.nextDouble() - 0.5D) * (double)this.width, this.posY + this.rand.nextDouble() * (double)this.height - 0.25D, this.posZ + (this.rand.nextDouble() - 0.5D) * (double)this.width, (this.rand.nextDouble() - 0.5D) * 2.0D, -this.rand.nextDouble(), (this.rand.nextDouble() - 0.5D) * 2.0D);
        }

        if (this.GetInLove() > 0)
        {
            this.GenerateRandomParticles("heart");
        }
    }

    protected void GenerateRandomParticles(String var1)
    {
        for (int var2 = 0; var2 < 5; ++var2)
        {
            double var3 = this.rand.nextGaussian() * 0.02D;
            double var5 = this.rand.nextGaussian() * 0.02D;
            double var7 = this.rand.nextGaussian() * 0.02D;
            this.worldObj.spawnParticle(var1, this.posX + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width, this.posY + 1.0D + (double)(this.rand.nextFloat() * this.height), this.posZ + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width, var3, var5, var7);
        }
    }

    public void CheckForLooseMilk()
    {
        List var1 = this.worldObj.getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getAABBPool().getAABB(this.posX - 1.0D, this.posY - 1.0D, this.posZ - 1.0D, this.posX + 1.0D, this.posY + 1.0D, this.posZ + 1.0D));

        if (!var1.isEmpty())
        {
            for (int var2 = 0; var2 < var1.size(); ++var2)
            {
                EntityItem var3 = (EntityItem)var1.get(var2);

                if (var3.delayBeforeCanPickup <= 0 && !var3.isDead)
                {
                    int var4 = var3.getEntityItem().itemID;
                    Item var5 = Item.itemsList[var4];

                    if (var5.itemID == Item.bucketMilk.itemID)
                    {
                        var3.setDead();
                        var3 = new EntityItem(this.worldObj, this.posX, this.posY - 0.30000001192092896D + (double)this.getEyeHeight(), this.posZ, new ItemStack(Item.bucketMilk, 1, 0));
                        float var6 = 0.2F;
                        var3.motionX = (double)(-MathHelper.sin(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * var6);
                        var3.motionZ = (double)(MathHelper.cos(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * var6);
                        var3.motionY = (double)(-MathHelper.sin(this.rotationPitch / 180.0F * (float)Math.PI) * var6 + 0.2F);
                        var6 = 0.02F;
                        float var7 = this.rand.nextFloat() * (float)Math.PI * 2.0F;
                        var6 *= this.rand.nextFloat();
                        var3.motionX += Math.cos((double)var7) * (double)var6;
                        var3.motionY += 0.25D;
                        var3.motionZ += Math.sin((double)var7) * (double)var6;
                        var3.delayBeforeCanPickup = 10;
                        this.worldObj.spawnEntityInWorld(var3);
                        int var8 = MathHelper.floor_double(var3.posX);
                        int var9 = MathHelper.floor_double(var3.posY);
                        int var10 = MathHelper.floor_double(var3.posZ);
                        byte var11 = 0;

                        if (this.IsPossessed() || this.getProfession() == 2 && this.GetCurrentTradeLevel() == 5)
                        {
                            var11 = 1;
                        }

                        this.worldObj.playAuxSFX(2265, var8, var9, var10, var11);
                    }
                }
            }
        }
    }

    public int GetInLove()
    {
        return this.dataWatcher.getWatchableObjectInt(22);
    }

    public void SetInLove(int var1)
    {
        this.dataWatcher.updateObject(22, var1);
    }

    public int GetDirtyPeasant()
    {
        return this.dataWatcher.getWatchableObjectInt(26);
    }

    public void SetDirtyPeasant(int var1)
    {
        this.dataWatcher.updateObject(26, var1);
    }

    public int GetCurrentTradeLevel()
    {
        return this.dataWatcher.getWatchableObjectInt(23);
    }

    public void SetTradeLevel(int var1)
    {
        this.dataWatcher.updateObject(23, var1);
    }

    public int GetCurrentTradeXP()
    {
        return this.dataWatcher.getWatchableObjectInt(25);
    }

    public void SetTradeExperience(int var1)
    {
        this.dataWatcher.updateObject(25, var1);
    }

    public int GetCurrentTradeMaxXP()
    {
        int var1 = this.GetCurrentTradeLevel();

        switch (var1)
        {
            case 1:
                return 5;

            case 2:
                return 7;

            case 3:
                return 10;

            case 4:
                return 15;

            default:
                return 20;
        }
    }

    public int GetCurrentMaxNumTrades()
    {
        int var1 = this.GetCurrentTradeLevel();
        int var2 = var1;

        switch (this.getProfession())
        {
            case 2:
                if (var1 >= 4)
                {
                    var2 = var1 + 1;
                }

            case 0:
            case 1:
            case 3:
            case 4:
            default:
                return var2;
        }
    }
}
