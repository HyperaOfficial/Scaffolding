package net.crystalgames.scaffolding.schematic.impl;

import net.crystalgames.scaffolding.schematic.Schematic;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.collections.ImmutableByteArray;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// https://github.com/EngineHub/WorldEdit/blob/version/5.x/src/main/java/com/sk89q/worldedit/schematic/MCEditSchematicFormat.java
public class MCEditSchematic extends Schematic {

    private static final HashMap<String, Short> STATE_ID_LOOKUP = new HashMap<>();

    static {
        try {
            // Load state IDS from lookup table
            InputStream is = MCEditSchematic.class.getClassLoader().getResourceAsStream("MCEditBlockStateLookup.txt");
            BufferedInputStream bis = new BufferedInputStream(Objects.requireNonNull(is));
            String raw = new String(bis.readAllBytes());
            for (String line : raw.split("\n")) {
                String[] split = line.split("=");
                STATE_ID_LOOKUP.put(split[0], Short.parseShort(split[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void read(@NotNull NBTCompound nbtTag) throws NBTException {
        if (!nbtTag.containsKey("Blocks")) throw new NBTException("Invalid Schematic: No Blocks");

        readSizes(nbtTag);
        readBlocksData(nbtTag);
        readOffsets(nbtTag);

        setLocked(false);
    }

    private void readOffsets(@NotNull NBTCompound nbtTag) throws NBTException {
        Integer weOffsetX = nbtTag.getInt("WEOffsetX");
        if (weOffsetX == null) throw new NBTException("Invalid Schematic: No WEOffsetX");

        Integer weOffsetY = nbtTag.getInt("WEOffsetY");
        if (weOffsetY == null) throw new NBTException("Invalid Schematic: No WEOffsetY");

        Integer weOffsetZ = nbtTag.getInt("WEOffsetZ");
        if (weOffsetZ == null) throw new NBTException("Invalid Schematic: No WEOffsetZ");

        setOffset(weOffsetX, weOffsetY, weOffsetZ);
    }

    private void readSizes(@NotNull NBTCompound nbtTag) throws NBTException {
        Short width = nbtTag.getShort("Width");
        if (width == null) throw new NBTException("Invalid Schematic: No Width");

        Short height = nbtTag.getShort("Height");
        if (height == null) throw new NBTException("Invalid Schematic: No Height");

        Short length = nbtTag.getShort("Length");
        if (length == null) throw new NBTException("Invalid Schematic: No Length");

        setSize(width, height, length);
    }


    private void readBlocksData(@NotNull NBTCompound nbtTag) throws NBTException {
        String materials = nbtTag.getString("Materials");
        if (materials == null || !materials.equals("Alpha"))
            throw new NBTException("Invalid Schematic: Invalid Materials");

        ImmutableByteArray blockIdPre = nbtTag.getByteArray("Blocks");
        if (blockIdPre == null) throw new NBTException("Invalid Schematic: No Blocks");
        byte[] blockId = blockIdPre.copyArray();

        ImmutableByteArray blocksData = nbtTag.getByteArray("Data");
        if (blocksData == null) throw new NBTException("Invalid Schematic: No Block Data");
        byte[] blockData = blocksData.copyArray();

        // Each "add block" contains the upper 4 bits for 2 blocks packed in one byte
        // addBlocks.length / 2 = number of blocks
        byte[] addBlocks = nbtTag.containsKey("AddBlocks") ? Objects.requireNonNull(nbtTag.getByteArray("AddBlocks")).copyArray() : new byte[0];

        short[] outdatedBlockIds = new short[getArea()];

        for (int index = 0; index < blockId.length; index++) {
            final int halfIndex = index >> 1; // same as 'index / 2'
            short addAmount = 0;

            if (halfIndex < addBlocks.length) {
                final short rawAdd = (short) (addBlocks[halfIndex] & 0b11111111);
                // If index is even, we want to shift 8 bits (a full byte) to the left, otherwise 4 since a single byte holds 2 blocks.
                // The MCEdit format is weird and uses the upper 4 bits for even blocks and the lower 4 bits for odd blocks
                final int leftShiftAmount = (index % 2 == 0) ? 8 : 4;
                addAmount = (short) (rawAdd << leftShiftAmount);
            }

            outdatedBlockIds[index] = (short) (addAmount + (blockId[index] & 0b11111111));
        }

        for (int x = 0; x < getWidth(); ++x) {
            for (int y = 0; y < getHeight(); ++y) {
                for (int z = 0; z < getLength(); ++z) {
                    int index = getIndex(x, y, z);
                    String legacyId = outdatedBlockIds[index] + ":" + blockData[index];

                    // Let's just ignore unknown blocks for now
                    // TODO: log when unknown blocks are encountered?
                    short stateId = STATE_ID_LOOKUP.get(legacyId);

                    setBlock(x, y, z, stateId);
                }
            }
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull OutputStream outputStream) {
        // TODO: Complete
        return null;
    }
}
