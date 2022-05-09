package net.crystalgames.scaffolding.schematic;

import net.crystalgames.scaffolding.region.Region;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.UnitModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public abstract class Schematic {

    private short[] blocks;

    private int width, height, length;
    private int offsetX, offsetY, offsetZ;

    private int area;

    private boolean locked;

    public Schematic() {
        reset();
    }

    private @NotNull CompletableFuture<Void> loadChunks(@NotNull Instance instance, @NotNull Region region) {
        final int lengthX = region.upperChunkX() - region.lowerChunkX() + 1;
        final int lengthZ = region.upperChunkZ() - region.lowerChunkZ() + 1;

        final CompletableFuture<?>[] futures = new CompletableFuture[lengthX * lengthZ];
        int index = 0;

        for (int x = region.lowerChunkX(); x <= region.upperChunkX(); ++x) {
            for (int z = region.lowerChunkZ(); z <= region.upperChunkZ(); ++z) {
                futures[index++] = instance.loadChunk(x, z);
            }
        }

        return CompletableFuture.allOf(futures);
    }

    public @NotNull CompletableFuture<Void> copy(@NotNull Instance instance, Region region) {
        reset();

        return CompletableFuture.runAsync(() -> {
            CompletableFuture<Void> loadChunksFuture = loadChunks(instance, region);

            setSize(width, height, length);

            loadChunksFuture.join();

            for (int x = 0; x < region.sizeX(); ++x) {
                for (int y = 0; y < region.sizeY(); ++y) {
                    for (int z = 0; z < region.sizeZ(); ++z) {
                        final int blockX = region.lower().blockX() + x;
                        final int blockY = region.lower().blockY() + y;
                        final int blockZ = region.lower().blockZ() + z;

                        Block block = instance.getBlock(blockX, blockY, blockZ, Block.Getter.Condition.TYPE);
                        if (block == null) return;

                        blocks[getIndex(x, y, z)] = block.stateId();
                    }
                }
            }

            locked = false;
        });
    }

    public abstract void read(@NotNull NBTCompound nbtTag) throws NBTException;

    public abstract @NotNull CompletableFuture<Void> write(@NotNull OutputStream outputStream);

    public @NotNull CompletableFuture<Region> build(@NotNull Instance instance, @NotNull Pos position, boolean flipX, boolean flipY, boolean flipZ) {
        if (locked) throw new IllegalStateException("Cannot build a locked blueprint.");

        final Pos lower = position.add(offsetX, offsetY, offsetZ);
        final Pos upper = lower.add(width, height, length);

        Region region = new Region(instance, lower, upper);

        final CompletableFuture<Void> loadChunks = loadChunks(instance, region);
        final AbsoluteBlockBatch blockBatch = new AbsoluteBlockBatch();

        apply(lower, flipX, flipY, flipZ, blockBatch);

        CompletableFuture<Region> future = new CompletableFuture<>();
        loadChunks.thenRun(() -> {
            try {
                blockBatch.apply(instance, () -> future.complete(region));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public @NotNull CompletableFuture<Region> build(@NotNull Instance instance, @NotNull Pos position) {
        return build(instance, position, false, false, false);
    }

    private void apply(@NotNull Pos start, boolean flipX, boolean flipY, boolean flipZ, @NotNull Block.Setter setter) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    // Will the JVM optimize out the ternary operator? I hope so.
                    final int indexX = flipX ? width - x - 1 : x;
                    final int indexY = flipY ? height - y - 1 : y;
                    final int indexZ = flipZ ? length - z - 1 : z;

                    int blockX = start.blockX() + x;
                    int blockY = start.blockY() + y;
                    int blockZ = start.blockZ() + z;

                    Block block = getBlock(indexX, indexY, indexZ);

                    if (block != null) setter.setBlock(blockX, blockY, blockZ, block);
                }
            }
        }
    }

    public void fork(@NotNull GenerationUnit unit, @NotNull Pos position, boolean flipX, boolean flipY, boolean flipZ) {
        if (locked) throw new IllegalStateException("Cannot fork a locked blueprint.");

        final Pos start = position.sub(offsetX, offsetY, offsetZ);
        final Pos end = start.add(width, height, length);

        System.out.println(end.sub(start));

        UnitModifier forkModifier = unit.fork(start, end).modifier();

        apply(start, flipX, flipY, flipZ, forkModifier);
    }

    private void reset() {
        width = height = length = 0;
        offsetX = offsetY = offsetZ = 0;

        blocks = null; // Does this actually have to be nulled out? This looks a bit too much like a deconstructor.
        locked = true;
    }

    public short getStateId(int x, int y, int z) {
        return blocks[getIndex(x, y, z)];
    }

    @Nullable
    public Block getBlock(int indexX, int indexY, int indexZ) {
        short stateId = getStateId(indexX, indexY, indexZ);
        return Block.fromStateId(stateId);
    }

    protected int getIndex(int x, int y, int z) {
        return y * width * length + z * width + x;
    }

    protected void setSize(int sizeX, int sizeY, int sizeZ) {
        this.width = sizeX;
        this.height = sizeY;
        this.length = sizeZ;

        area = sizeX * sizeY * sizeZ;
        blocks = new short[area];
    }

    public void setBlock(int x, int y, int z, short stateId) {
        blocks[getIndex(x, y, z)] = stateId;
    }

    public void setBlock(int x, int y, int z, @NotNull Block block) {
        setBlock(x, y, z, block.stateId());
    }

    public void setBlock(@NotNull Pos position, @NotNull Block block) {
        setBlock(position.blockX(), position.blockY(), position.blockZ(), block);
    }

    public void setBlock(@NotNull Pos position, short stateId) {
        setBlock(position.blockX(), position.blockY(), position.blockZ(), stateId);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public int getArea() {
        return area;
    }

    public boolean isLocked() {
        return locked;
    }

    protected void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setOffset(int x, int y, int z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
    }

    public void setOffset(@NotNull Pos position) {
        setOffset(position.blockX(), position.blockY(), position.blockZ());
    }

    /**
     * Applies the schematic to the given block setter.
     *
     * @param setter the block setter
     */
    @Deprecated
    public void apply(@NotNull Block.Setter setter) {
        apply(Pos.ZERO, false, false, false, setter);
    }
}
