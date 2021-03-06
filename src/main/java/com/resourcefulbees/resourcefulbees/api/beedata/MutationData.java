package com.resourcefulbees.resourcefulbees.api.beedata;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.resourcefulbees.resourcefulbees.lib.BeeConstants;
import com.resourcefulbees.resourcefulbees.lib.MutationTypes;
import com.resourcefulbees.resourcefulbees.utils.BeeInfoUtils;
import com.resourcefulbees.resourcefulbees.utils.RandomCollection;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class MutationData extends AbstractBeeData {

    public static final Logger LOGGER = LogManager.getLogger();
    /**
     * The Input that is getting mutated
     */
    @Deprecated
    private final String mutationInput;

    /**
     * The new muted output
     */
    @Deprecated
    private final String mutationOutput;

    /**
     * What type of mutation it is
     */
    @Deprecated
    private final MutationTypes mutationType;

    /**
     * If the bee has a mutation
     */
    private boolean hasMutation;

    /**
     * how many inputs in a radius get mutated
     */
    private final int mutationCount;

    /**
     * List of block mutations
     */
    private List<Mutation> mutations = new LinkedList<>();

    public transient Map<Block, IBlockMutation> iBlockMutations = new HashMap<>();
    public transient Map<String, IBlockMutation> iBlockTagMutations = new HashMap<>();
    public transient Map<EntityType, IEntityMutation> iEntityMutations = new HashMap<>();
    public transient Map<Block, IItemMutation> iBlockItemMutations = new HashMap<>();
    public transient Map<String, IItemMutation> iBlockItemTagMutations = new HashMap<>();


    private MutationData(String mutationInput, String mutationOutput, int mutationCount, boolean hasMutation, MutationTypes mutationType) {
        this.mutationInput = mutationInput;
        this.mutationOutput = mutationOutput;
        this.mutationCount = mutationCount;
        this.hasMutation = hasMutation;
        this.mutationType = mutationType;
    }

    public int getMutationCount() {
        return mutationCount <= 0 ? 10 : mutationCount;
    }

    public boolean hasMutation() {
        return hasMutation;
    }

    public void setHasMutation(boolean hasMutation) {
        this.hasMutation = hasMutation;
    }

    public static class Builder {
        private String mutationInput;
        private String mutationOutput;
        private int mutationCount;
        private final boolean hasMutation;
        private final MutationTypes mutationType;

        public Builder(boolean hasMutation, MutationTypes mutationType) {
            this.hasMutation = hasMutation;
            this.mutationType = mutationType;
        }

        public Builder setMutationInput(String mutationInput) {
            this.mutationInput = mutationInput;
            return this;
        }

        public Builder setMutationOutput(String mutationOutput) {
            this.mutationOutput = mutationOutput;
            return this;
        }

        public Builder setMutationCount(int mutationCount) {
            this.mutationCount = mutationCount;
            return this;
        }

        public MutationData createMutationData() {
            return new MutationData(mutationInput, mutationOutput, mutationCount, hasMutation, mutationType);
        }
    }

    public static MutationData createDefault() {
        return new Builder(false, MutationTypes.NONE).createMutationData();
    }

    public void initMutations(CustomBeeData b) {
        iBlockTagMutations = new HashMap<>();
        iBlockMutations = new HashMap<>();
        iBlockItemTagMutations = new HashMap<>();
        iBlockItemMutations = new HashMap<>();
        iEntityMutations = new HashMap<>();
        if (mutations != null) {
            Iterator<Mutation> iterator = mutations.iterator();
            while (iterator.hasNext()) {
                if (!iterator.next().validateMutation(b)) iterator.remove();
            }
        }
        initMutationList();
        initBaseMutationData();
    }

    private void initBaseMutationData() {
        if (hasMutation) {
            Mutation mutation = new Mutation(mutationType, mutationInput, new MutationOutput(mutationOutput, 1, 1));
            if (mutationType == null || mutationInput == null || mutationOutput == null) return;
            switch (mutationType) {
                case FLUID_TO_FLUID:
                case BLOCK_TO_FLUID:
                case FLUID_TO_BLOCK:
                case BLOCK_TO_BLOCK:
                    if (mutationInput.startsWith(BeeConstants.TAG_PREFIX)) {
                        addBlockTagMutation(mutation);
                    } else {
                        addBlockMutation(mutation);
                    }
                    break;
                case BLOCK_TO_ITEM:
                    if (mutationInput.startsWith(BeeConstants.TAG_PREFIX)) {
                        addBlockItemTagMutation(mutation);
                    } else {
                        addBlockItemMutation(mutation);
                    }
                case ENTITY_TO_ENTITY:
                    if (mutationInput.startsWith(BeeConstants.ENTITY_PREFIX) && mutationOutput.startsWith(BeeConstants.ENTITY_PREFIX)) {
                        addEntityMutation(mutation);
                    }
                    break;
            }
        }
    }

    private void addBlockMutation(Mutation mutation) {
        Block input = BeeInfoUtils.getBlock(mutation.inputID);
        IBlockMutation blockMutation = new IBlockMutation(mutation.type, mutation);
        for (MutationOutput m : mutation.outputs) {
            Block output = BeeInfoUtils.getBlock(m.outputID);
            if (output != null && output != Blocks.AIR) {
                blockMutation.addBlock(output, m.weight, m.chance, m.getNbt());
            }
        }
        if (input != null && input != Blocks.AIR && !blockMutation.outputs.isEmpty() && mutation.type != null) {
            iBlockMutations.put(input, blockMutation);
        } else {
            printWarning(mutation);
        }
    }

    private void addBlockTagMutation(Mutation mutation) {
        String tag = mutation.inputID.toLowerCase().replace(BeeConstants.TAG_PREFIX, "");
        ITag input = BeeInfoUtils.getBlockTag(tag);
        if (input == null) {
            input = BeeInfoUtils.getFluidTag(tag);
        }
        IBlockMutation blockMutation = new IBlockMutation(mutation.type, mutation);
        for (MutationOutput m : mutation.outputs) {
            Block output = BeeInfoUtils.getBlock(m.outputID);
            if (output != null && output != Blocks.AIR) {
                blockMutation.addBlock(output, m.weight, m.chance, m.getNbt());
            }
        }
        if (input != null && !blockMutation.outputs.isEmpty() && mutation.type != null) {
            iBlockTagMutations.put(tag, blockMutation);
        } else {
            printWarning(mutation);
        }
    }

    private void addBlockItemMutation(Mutation mutation) {
        Block input = BeeInfoUtils.getBlock(mutation.inputID);
        IItemMutation itemMutation = new IItemMutation(mutation.type, mutation);
        for (MutationOutput m : mutation.outputs) {
            Item output = BeeInfoUtils.getItem(m.outputID);
            if (output != null) {
                itemMutation.addItem(output, m.weight, m.chance, m.getNbt());
            }
        }
        if (input != null && input != Blocks.AIR && !itemMutation.outputs.isEmpty() && mutation.type != null) {
            iBlockItemMutations.put(input, itemMutation);
        } else {
            printWarning(mutation);
        }
    }

    private void addBlockItemTagMutation(Mutation mutation) {
        String tag = mutation.inputID.toLowerCase().replace(BeeConstants.TAG_PREFIX, "");
        ITag input = BeeInfoUtils.getBlockTag(tag);
        if (input == null) {
            input = BeeInfoUtils.getFluidTag(tag);
        }
        IItemMutation itemMutation = new IItemMutation(mutation.type, mutation);
        for (MutationOutput m : mutation.outputs) {
            Item output = BeeInfoUtils.getItem(m.outputID);
            if (output != null) {
                itemMutation.addItem(output, m.weight, m.chance, m.getNbt());
            }
        }
        if (input != null && !itemMutation.outputs.isEmpty() && mutation.type != null) {
            iBlockItemTagMutations.put(tag, itemMutation);
        } else {
            printWarning(mutation);
        }
    }

    private void addEntityMutation(Mutation mutation) {
        EntityType input = BeeInfoUtils.getEntityType(mutation.inputID.toLowerCase().replace(BeeConstants.ENTITY_PREFIX, ""));
        IEntityMutation entityMutation = new IEntityMutation(mutation.type, mutation);
        for (MutationOutput m : mutation.outputs) {
            EntityType output = BeeInfoUtils.getEntityType(m.outputID.replace(BeeConstants.ENTITY_PREFIX, ""));
            if (output != null) {
                entityMutation.addEntity(output, m.weight, m.chance, m.getNbt());
            }
        }
        if (input != null && !entityMutation.outputs.isEmpty() && mutation.type != null) {
            iEntityMutations.put(input, entityMutation);
        } else {
            printWarning(mutation);
        }
    }

    private void initMutationList() {
        if (mutations == null || mutations.isEmpty()) return;
        mutations.stream().forEach(b -> {
            if (b.type == MutationTypes.BLOCK_TO_ITEM && b.inputID.toLowerCase().startsWith(BeeConstants.TAG_PREFIX)) {
                addBlockItemTagMutation(b);
            } else if (b.inputID.toLowerCase().startsWith(BeeConstants.TAG_PREFIX)) {
                addBlockTagMutation(b);
            } else if (isValidEntityMutation(b.inputID, b.type)) {
                addEntityMutation(b);
            } else if (b.type == MutationTypes.BLOCK_TO_ITEM) {
                addBlockItemMutation(b);
            } else {
                addBlockMutation(b);
            }
        });
    }

    private boolean isValidEntityMutation(String inputID, MutationTypes type) {
        return inputID.toLowerCase().startsWith(BeeConstants.ENTITY_PREFIX) && type == MutationTypes.ENTITY_TO_ENTITY;
    }

    private void printWarning(Mutation mutation) {
        LOGGER.warn(String.format("Could not validate mutation: [\"type\": \"%s\", \"inputID\": \"%s\", \"outputID\": \"%s\", \"chance\": %f]", mutation.type, mutation.inputID, mutation.outputs, mutation.getDefaultChance()));
    }

    /**
     * Used to determine how the recipe will look
     */

    public class Mutation {
        public MutationTypes type;
        public String inputID;
        public List<MutationOutput> outputs;
        public double defaultWeight = 1;
        private double defaultChance = 1;

        public Mutation(MutationTypes type, String inputID, MutationOutput... outputs) {
            this.type = type;
            this.inputID = inputID;
            this.outputs = new ArrayList<>();
            this.outputs.addAll(Arrays.asList(outputs));
        }

        public double getDefaultWeight() {
            return defaultWeight <= 0 ? 1 : defaultWeight;
        }

        public double getDefaultChance() {
            return defaultChance <= 0 ? 1 : defaultChance;
        }

        public boolean validateMutation(CustomBeeData b) {
            String name = b.getName();
            if (type == null) {
                LOGGER.warn(String.format("\"type\" could not be validated for %s's mutation.", name));
                return false;
            }
            if (inputID == null) {
                LOGGER.warn(String.format("\"inputID\" does not exist for %s's mutation.", name));
                return false;
            }
            if (outputs == null) {
                LOGGER.warn(String.format("\"outputs\" does not exist for %s's mutation.", name));
                return false;
            }
            Iterator<MutationOutput> iterator = outputs.iterator();
            while (iterator.hasNext()) {
                MutationOutput i = iterator.next();
                if (i.outputID == null) {
                    LOGGER.warn(String.format("an instance of \"outputID\" does not exist for %s's mutation.", name));
                    iterator.remove();
                }
            }
            if (outputs.isEmpty()) {
                LOGGER.warn(String.format("No valid outputs could be found for %s's mutation.", name));
                return false;
            }
            return true;
        }
    }

    public class MutationOutput {
        public String outputID;
        private JsonElement nbtData;
        private transient CompoundNBT nbt;
        private double weight = 1;
        private double chance = 1;

        public MutationOutput(String outputID, double weight, double chance) {
            this.outputID = outputID;
            this.weight = weight;
            this.chance = chance;
        }

        public CompoundNBT getNbt() {
            return nbt == null ? initNbt() : nbt;
        }

        private CompoundNBT initNbt() {
            if (nbtData == null) {
                nbt = new CompoundNBT();
            } else {
                nbt = CompoundNBT.CODEC.parse(JsonOps.INSTANCE, nbtData).resultOrPartial(e -> LOGGER.warn(String.format("Could not deserialize NBT: [%s]", nbtData.toString()))).get();
            }
            return nbt;
        }

        public double getWeight() {
            return weight <= 0 ? 1 : weight;
        }

        public double getChance() {
            return chance <= 0 ? 1 : chance;
        }
    }

    public class IBlockMutation {
        public MutationTypes type;
        public RandomCollection<Pair<Block, MutationOutputData>> outputs;
        public Mutation mutationData;

        public IBlockMutation(MutationTypes type, Mutation mutationData) {
            this.type = type;
            this.outputs = new RandomCollection<>();
            this.mutationData = mutationData;
        }

        public IBlockMutation addBlock(Block block, double weight, double chance, CompoundNBT nbt) {
            if (weight <= 0) weight = mutationData.getDefaultWeight();
            if (chance <= 0) chance = mutationData.getDefaultChance();
            outputs.add(weight, Pair.of(block, new MutationOutputData(chance, weight, nbt)));
            return this;
        }
    }

    public class IItemMutation {
        public MutationTypes type;
        public RandomCollection<Pair<Item, MutationOutputData>> outputs;
        public Mutation mutationData;

        public IItemMutation(MutationTypes type, Mutation mutationData) {
            this.type = type;
            this.outputs = new RandomCollection<>();
            this.mutationData = mutationData;
        }

        public IItemMutation addItem(Item item, double weight, double chance, CompoundNBT nbt) {
            if (weight <= 0) weight = mutationData.getDefaultWeight();
            if (chance <= 0) chance = mutationData.getDefaultChance();
            outputs.add(weight, Pair.of(item, new MutationOutputData(chance, weight, nbt)));
            return this;
        }
    }

    public class IEntityMutation {
        public MutationTypes type;
        public RandomCollection<Pair<EntityType, MutationOutputData>> outputs;
        public float chance;
        public Mutation mutationData;

        public IEntityMutation(MutationTypes type, Mutation mutationData) {
            this.type = type;
            this.outputs = new RandomCollection<>();
            this.mutationData = mutationData;
        }

        public IEntityMutation addEntity(EntityType entity, double weight, double chance, CompoundNBT nbt) {
            if (weight <= 0) weight = mutationData.getDefaultWeight();
            if (chance <= 0) chance = mutationData.getDefaultChance();
            outputs.add(weight, Pair.of(entity, new MutationOutputData(chance, weight, nbt)));
            return this;
        }
    }

    public class MutationOutputData {
        public double weight;
        public double chance;
        public CompoundNBT nbt;

        public MutationOutputData(double weight, double chance, CompoundNBT nbt) {
            this.weight = weight;
            this.chance = chance;
            this.nbt = nbt;
        }
    }
}

