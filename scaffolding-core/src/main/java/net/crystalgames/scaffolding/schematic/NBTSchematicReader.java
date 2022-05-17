package net.crystalgames.scaffolding.schematic;

import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.collections.ImmutableByteArray;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;

import java.util.concurrent.CompletableFuture;

/**
 * A parser for schematics that uses NBT to store data.
 */
public abstract class NBTSchematicReader {

    /**
     * Parses data  from the provided NBT tag and stores it in the provided schematic.
     *
     * @param compound  The {@link NBTCompound} to read from
     * @param schematic The {@link Schematic} to read into
     * @return a {@link CompletableFuture<Schematic>} that will be completed with the {@link Schematic}
     * @throws NBTException If the provided NBT tag is invalid
     */
    public abstract CompletableFuture<Schematic> read(@NotNull final NBTCompound compound, @NotNull final Schematic schematic) throws NBTException;

    /**
     * @param compound         The {@link NBTCompound} to read from
     * @param key              The key to look for
     * @param exceptionMessage The exception message to throw if the key is not found
     * @return The value of the key
     * @throws NBTException If the provided NBT tag is invalid
     */
    protected int getInteger(@NotNull NBTCompound compound, @NotNull String key, String exceptionMessage) throws NBTException {
        Integer value = compound.getInt(key);
        if (value == null) throw new NBTException(exceptionMessage);

        return value;
    }

    /**
     * @param compound         The {@link NBTCompound} to read from
     * @param key              The key to look for
     * @param exceptionMessage The exception message to throw if the key is not found
     * @return The value of the key
     * @throws NBTException If the provided NBT tag is invalid
     */
    protected short getShort(@NotNull NBTCompound compound, @NotNull String key, String exceptionMessage) throws NBTException {
        Short value = compound.getShort(key);
        if (value == null) throw new NBTException(exceptionMessage);

        return value;
    }

    /**
     * @param compound         The {@link NBTCompound} to read from
     * @param key              The key to look for
     * @param exceptionMessage The exception message to throw if the key is not found
     * @return The value of the key
     * @throws NBTException If the provided NBT tag is invalid
     */
    protected NBTCompound getCompound(@NotNull NBTCompound compound, @NotNull String key, String exceptionMessage) throws NBTException {
        NBTCompound value = compound.getCompound(key);
        if (value == null) throw new NBTException(exceptionMessage);

        return value;
    }

    /**
     * @param compound         The {@link NBTCompound} to read from
     * @param key              The key to look for
     * @param exceptionMessage The exception message to throw if the key is not found
     * @return The value of the key
     * @throws NBTException If the provided NBT tag is invalid
     */
    protected byte getByte(@NotNull NBTCompound compound, @NotNull String key, String exceptionMessage) throws NBTException {
        Byte value = compound.getByte(key);
        if (value == null) throw new NBTException(exceptionMessage);

        return value;
    }

    /**
     * @param compound         The {@link NBTCompound} to read from
     * @param key              The key to look for
     * @param exceptionMessage The exception message to throw if the key is not found
     * @return The value of the key
     * @throws NBTException If the provided NBT tag is invalid
     */
    protected byte[] getByteArray(@NotNull NBTCompound compound, @NotNull String key, String exceptionMessage) throws NBTException {
        ImmutableByteArray value = compound.getByteArray(key);
        if (value == null) throw new NBTException(exceptionMessage);

        return value.copyArray();
    }

    /**
     * @param compound         The {@link NBTCompound} to read from
     * @param key              The key to look for
     * @param exceptionMessage The exception message to throw if the key is not found
     * @return The value of the key
     * @throws NBTException If the provided NBT tag is invalid
     */
    protected boolean getBoolean(@NotNull NBTCompound compound, @NotNull String key, String exceptionMessage) throws NBTException {
        Boolean value = compound.getBoolean(key);
        if (value == null) throw new NBTException(exceptionMessage);

        return value;
    }

    /**
     * @param compound         The {@link NBTCompound} to read from
     * @param key              The key to look for
     * @param exceptionMessage The exception message to throw if the key is not found
     * @return The value of the key
     * @throws NBTException If the provided NBT tag is invalid
     */
    protected String getString(@NotNull NBTCompound compound, @NotNull String key, String exceptionMessage) throws NBTException {
        String value = compound.getString(key);
        if (value == null) throw new NBTException(exceptionMessage);

        return value;
    }
}