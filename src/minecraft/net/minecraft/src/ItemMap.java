package net.minecraft.src;

import java.util.List;

public class ItemMap extends ItemMapBase
{
    protected ItemMap(int par1)
    {
        super(par1);
        this.setHasSubtypes(true);
    }

    public static MapData getMPMapData(short par0, World par1World)
    {
        String var2 = "map_" + par0;
        MapData var3 = (MapData)par1World.loadItemData(MapData.class, var2);

        if (var3 == null)
        {
            var3 = new MapData(var2);
            par1World.setItemData(var2, var3);
        }

        return var3;
    }

    public MapData getMapData(ItemStack par1ItemStack, World par2World)
    {
        String var3 = "map_" + par1ItemStack.getItemDamage();
        MapData var4 = (MapData)par2World.loadItemData(MapData.class, var3);

        if (var4 == null && !par2World.isRemote)
        {
            par1ItemStack.setItemDamage(par2World.getUniqueDataId("map"));
            var3 = "map_" + par1ItemStack.getItemDamage();
            var4 = new MapData(var3);
            var4.scale = 3;
            int var5 = 128 * (1 << var4.scale);
            var4.xCenter = Math.round((float)par2World.getWorldInfo().getSpawnX() / (float)var5) * var5;
            var4.zCenter = Math.round((float)(par2World.getWorldInfo().getSpawnZ() / var5)) * var5;
            var4.dimension = (byte)par2World.provider.dimensionId;
            var4.markDirty();
            par2World.setItemData(var3, var4);
        }

        return var4;
    }

    //ADDON EXTENDED
    public void updateMapData(World world, Entity entity, MapData mapData) {
        if (world.provider.dimensionId == mapData.dimension && entity instanceof EntityPlayer) {
            short baseSizeX = 128;
            short baseSizeZ = 128;
            int scale = 1 << mapData.scale;
            int xCenter = mapData.xCenter;
            int zCenter = mapData.zCenter;
            int entityXOnMap = MathHelper.floor_double(entity.posX - (double)xCenter) / scale + baseSizeX / 2;
            int entityZOnMap = MathHelper.floor_double(entity.posZ - (double)zCenter) / scale + baseSizeZ / 2;
            int offsetScale = 128 / scale;

            if (world.provider.hasNoSky) {
                offsetScale /= 2;
            }

            MapInfo mapInfo = mapData.func_82568_a((EntityPlayer)entity);
            mapInfo.field_82569_d++;

            for (int i = entityXOnMap - offsetScale + 1; i < entityXOnMap + offsetScale; ++i) {
                if ((i & 15) == (mapInfo.field_82569_d & 15)) {
                    int var14 = 255;
                    int var15 = 0;
                    double var16 = 0.0D;

                    for (int k = entityZOnMap - offsetScale - 1; k < entityZOnMap + offsetScale; ++k) {
                        if (i >= 0 && k >= -1 && i < baseSizeX && k < baseSizeZ) {
                            int xDistFromEntity = i - entityXOnMap;
                            int zDistFromEntity = k - entityZOnMap;
                            boolean isBlockInRangeOfEntity = xDistFromEntity * xDistFromEntity + zDistFromEntity * zDistFromEntity > (offsetScale - 2) * (offsetScale - 2);
                            int blockX = (xCenter / scale + i - baseSizeX / 2) * scale;
                            int blockZ = (zCenter / scale + k - baseSizeZ / 2) * scale;
                            int[] blockIDCountArray = new int[4096];
                            int[][] metaCountPerIDArray = new int[4096][16];
                            Chunk chunk = world.getChunkFromBlockCoords(blockX, blockZ);

                            if (!chunk.isEmpty()) {
                                int chunkX = blockX & 15;
                                int chunkZ = blockZ & 15;
                                int var28 = 0;
                                double avgY = 0.0D;

                                if (world.provider.hasNoSky) {
                                    int var31 = blockX + blockZ * 231871;
                                    var31 = var31 * var31 * 31287121 + var31 * 11;

                                    if ((var31 >> 20 & 1) == 0) {
                                        blockIDCountArray[Block.dirt.blockID] += 10;
                                    }
                                    else {
                                        blockIDCountArray[Block.stone.blockID] += 10;
                                    }

                                    avgY = 100.0D;
                                }
                                else {
                                    for (int localX = 0; localX < scale; ++localX) {
                                        for (int localZ = 0; localZ < scale; ++localZ) {
                                            int blockY = chunk.getHeightValue(localX + chunkX, localZ + chunkZ) + 1;
                                            int blockID = 0;
                                            int blockMetadata = 0;

                                            if (blockY > 1) {
                                                boolean isSolidBlock;

                                                do {
                                                    isSolidBlock = true;
                                                    blockID = chunk.getBlockID(localX + chunkX, blockY - 1, localZ + chunkZ);
                                                    blockMetadata = chunk.getBlockMetadata(localX + chunkX, blockY - 1, localZ + chunkZ);

                                                    if (blockID == 0) {
                                                        isSolidBlock = false;
                                                    }
                                                    else if (blockY > 0 && blockID > 0 && Block.blocksList[blockID].blockMaterial.materialMapColor == MapColor.airColor) {
                                                        isSolidBlock = false;
                                                    }

                                                    if (!isSolidBlock) {
                                                        blockY--;

                                                        if (blockY <= 0) {
                                                            break;
                                                        }

                                                        blockID = chunk.getBlockID(localX + chunkX, blockY - 1, localZ + chunkZ);
                                                    }
                                                }
                                                while (blockY > 0 && !isSolidBlock);

                                                if (blockY > 0 && blockID != 0 && Block.blocksList[blockID].blockMaterial.isLiquid()) {
                                                    int y = blockY - 1;
                                                    boolean var37 = false;
                                                    int var38;

                                                    do {
                                                        var38 = chunk.getBlockID(localX + chunkX, y, localZ + chunkZ);
                                                        y--;
                                                        var28++;
                                                    }
                                                    while (y > 0 && var38 != 0 && Block.blocksList[var38].blockMaterial.isLiquid());
                                                }
                                            }

                                            avgY += (double)blockY / (double)(scale * scale);
                                            blockIDCountArray[blockID]++;
                                            metaCountPerIDArray[blockID][blockMetadata]++;
                                        }
                                    }
                                }

                                var28 /= scale * scale;
                                int greatestBlockIDCount = 0;
                                int greatestBlockID = 0;

                                for (int a = 0; a < 4096; ++a) {
                                    if (blockIDCountArray[a] > greatestBlockIDCount) {
                                        greatestBlockID = a;
                                        greatestBlockIDCount = blockIDCountArray[a];
                                    }
                                }

                                int greatestMetaCount = 0;
                                int greatestMeta = 0;
                                
                                for (int a = 0; a < 16; a++) {
                                	if (metaCountPerIDArray[greatestBlockID][a] > greatestMetaCount) {
                                		greatestMeta = a;
                                		greatestMetaCount = metaCountPerIDArray[greatestBlockID][a];
                                    }
                                }

                                double var40 = (avgY - var16) * 4.0D / (double)(scale + 4) + ((double)(i + k & 1) - 0.5D) * 0.4D;
                                byte var41 = 1;

                                if (var40 > 0.6D) {
                                    var41 = 2;
                                }

                                if (var40 < -0.6D) {
                                    var41 = 0;
                                }

                                int colorIndex = 0;

                                if (greatestBlockID > 0) {
                                    MapColor color = Block.blocksList[greatestBlockID].getMapColor(greatestMeta);
                                    
                                    if (color == MapColor.waterColor) {
                                        var40 = (double)var28 * 0.1D + (double)(i + k & 1) * 0.2D;
                                        var41 = 1;

                                        if (var40 < 0.5D) {
                                            var41 = 2;
                                        }

                                        if (var40 > 0.9D) {
                                            var41 = 0;
                                        }
                                    }

                                    colorIndex = color.colorIndex;
                                }

                                var16 = avgY;

                                if (k >= 0 && xDistFromEntity * xDistFromEntity + zDistFromEntity * zDistFromEntity < offsetScale * offsetScale && (!isBlockInRangeOfEntity || (i + k & 1) != 0)) {
                                    byte originalColor = mapData.colors[i + k * baseSizeX];
                                    byte newColor = (byte)(colorIndex * 4 + var41);

                                    if (originalColor != newColor) {
                                        if (var14 > k) {
                                            var14 = k;
                                        }

                                        if (var15 < k) {
                                            var15 = k;
                                        }

                                        mapData.colors[i + k * baseSizeX] = newColor;
                                    }
                                }
                            }
                        }
                    }

                    if (var14 <= var15) {
                        mapData.setColumnDirty(i, var14, var15);
                    }
                }
            }
        }
    }
    //ADDON EXTENDED

    public void onUpdate(ItemStack var1, World var2, EntityPlayer var3, int var4, boolean var5)
    {
        if (!var2.isRemote)
        {
            MapData var6 = this.getMapData(var1, var2);

            if (var3 instanceof EntityPlayer)
            {
                var6.updateVisiblePlayers(var3, var1);
            }

            if (var5)
            {
                this.updateMapData(var2, var3, var6);
            }
        }
    }

    /**
     * returns null if no update is to be sent
     */
    public Packet createMapDataPacket(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        byte[] var4 = this.getMapData(par1ItemStack, par2World).getUpdatePacketData(par1ItemStack, par2World, par3EntityPlayer);
        return var4 == null ? null : new Packet131MapData((short)Item.map.itemID, (short)par1ItemStack.getItemDamage(), var4);
    }

    /**
     * Called when item is crafted/smelted. Used only by maps so far.
     */
    public void onCreated(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        if (par1ItemStack.hasTagCompound() && par1ItemStack.getTagCompound().getBoolean("map_is_scaling"))
        {
            MapData var4 = Item.map.getMapData(par1ItemStack, par2World);
            par1ItemStack.setItemDamage(par2World.getUniqueDataId("map"));
            MapData var5 = new MapData("map_" + par1ItemStack.getItemDamage());
            var5.scale = (byte)(var4.scale + 1);

            if (var5.scale > 4)
            {
                var5.scale = 4;
            }

            var5.xCenter = var4.xCenter;
            var5.zCenter = var4.zCenter;
            var5.dimension = var4.dimension;
            var5.markDirty();
            par2World.setItemData("map_" + par1ItemStack.getItemDamage(), var5);
        }
    }

    /**
     * allows items to add custom lines of information to the mouseover description
     */
    public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4)
    {
        MapData var5 = this.getMapData(par1ItemStack, par2EntityPlayer.worldObj);

        if (par4)
        {
            if (var5 == null)
            {
                par3List.add("Unknown map");
            }
            else
            {
                par3List.add("Scaling at 1:" + (1 << var5.scale));
                par3List.add("(Level " + var5.scale + "/" + 4 + ")");
            }
        }
    }
}
