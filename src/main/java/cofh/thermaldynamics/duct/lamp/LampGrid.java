package cofh.thermaldynamics.duct.lamp;

import cofh.core.CoFHProps;
import cofh.core.network.PacketCoFHBase;
import cofh.core.network.PacketHandler;
import cofh.lib.util.position.ChunkCoord;
import cofh.thermaldynamics.core.WorldGridList;
import cofh.thermaldynamics.multiblock.IMultiBlock;
import cofh.thermaldynamics.multiblock.MultiBlockGrid;
import cpw.mods.fml.common.FMLCommonHandler;
import java.util.HashSet;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class LampGrid extends MultiBlockGrid {
    public HashSet<ChunkCoord> chunks;
    public boolean upToDate = false;

    public LampGrid(WorldGridList worldGrid) {
        super(worldGrid);
    }

    public LampGrid(World worldObj) {
        super(worldObj);
    }

    @Override
    public boolean canAddBlock(IMultiBlock aBlock) {
        return aBlock instanceof TileGlowDuct;
    }

    @Override
    public void onMajorGridChange() {
        upToDate = false;
        chunks = null;
    }

    @Override
    public void onMinorGridChange() {
        upToDate = false;
    }

    @Override
    public void tickGrid() {
        if (upToDate || worldGrid.worldObj.getTotalWorldTime() % 4 != 0)
            return;

        upToDate = true;

        boolean shouldBeLit = false;
        for (Object object : idleSet) {
            TileGlowDuct lamp = (TileGlowDuct) object;
            if (lamp.lit) {
                shouldBeLit = true;
                break;
            }
        }

        if (lit != shouldBeLit) {
            setLight(shouldBeLit);
        }
    }

    boolean lit = false;

    public void setLight(boolean lit) {
        this.lit = lit;

        if (chunks == null) {
            buildMap();
        }

        PacketCoFHBase packet = new PacketLamp(lit, this);
        int dimension = worldGrid.worldObj.provider.dimensionId;
        for (EntityPlayerMP player : (List<EntityPlayerMP>) FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().playerEntityList) {
            if (dimension == player.dimension) {
                for (ChunkCoord chunk : chunks) {
                    double d4 = chunk.chunkX - player.posX;
                    double d6 = chunk.chunkZ - player.posZ;

                    if (d4 * d4 + d6 * d6 < CoFHProps.NETWORK_UPDATE_RANGE * CoFHProps.NETWORK_UPDATE_RANGE) {
                        PacketHandler.sendTo(packet, player);
                        break;
                    }
                }
            }
        }

        for (Object block : idleSet) {
            ((TileGlowDuct) block).checkLight();
        }
    }


    public void buildMap() {
        chunks = new HashSet<ChunkCoord>();
        for (IMultiBlock iMultiBlock : idleSet) {
            buildMapEntry(iMultiBlock);
        }
    }

    private void buildMapEntry(IMultiBlock iMultiBlock) {
        chunks.add(new ChunkCoord(iMultiBlock.x() + 8, iMultiBlock.z() + 8));
    }
}
