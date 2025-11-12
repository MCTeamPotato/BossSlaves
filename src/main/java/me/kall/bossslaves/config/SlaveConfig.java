package me.kall.bossslaves.config;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.bossslaves.BossSlaves;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = BossSlaves.MOD_ID)
public class SlaveConfig {
    public static final ForgeConfigSpec INSTANCE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SLAVES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push(BossSlaves.MOD_NAME);
        SLAVES = builder.comment("Entry format: bossRegistryName->slave1RegistryName,weight;slave2RegistryName,weight;...;slaveXRegistryName,weight->maxSlavesCount", "Weight is omittable.").defineListAllowEmpty("BossSlavesEntries", Lists.newArrayList("minecraft:wither->minecraft:wither_skeleton;minecraft:skeleton->4", "minecraft:ender_dragon->minecraft:enderman->4"), Predicates.alwaysTrue());
        builder.pop();
        INSTANCE = builder.build();
    }

    public static final Map<ResourceLocation, Slaves> BOSS_SLAVES = new Object2ObjectOpenHashMap<>();

    @SubscribeEvent
    public static void setup(@NotNull FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BOSS_SLAVES.clear();

            for (String entry : SLAVES.get()) {
                try {
                    String[] parts = entry.split("->");
                    if (parts.length != 3) {
                        BossSlaves.LOGGER.warn("[BossSlaves] Invalid entry format: {}", entry);
                        continue;
                    }

                    ResourceLocation bossID = ResourceLocation.parse(parts[0].trim());

                    int maxCount = Integer.parseInt(parts[2].trim());

                    String[] slaveEntries = parts[1].split(";");
                    SimpleWeightedRandomList.Builder<ResourceLocation> slaveList = SimpleWeightedRandomList.builder();

                    for (String s : slaveEntries) {
                        if (s.isBlank()) continue;
                        String[] slaveParts = s.split(",");
                        ResourceLocation slaveID = ResourceLocation.parse(slaveParts[0].trim());
                        int weight = 1;
                        if (slaveParts.length >= 2) weight = Integer.parseInt(slaveParts[1].trim());
                        slaveList.add(slaveID, weight);
                    }

                    if (slaveList.build().isEmpty()) {
                        BossSlaves.LOGGER.warn("[BossSlaves] Boss {} has no valid slaves defined.", bossID);
                        continue;
                    }

                    BOSS_SLAVES.put(bossID, new Slaves(slaveList.build(), maxCount));
                } catch (Exception e) {
                    BossSlaves.LOGGER.error("[BossSlaves] Failed to parse config entry: {}", entry, e);
                }
            }

            BossSlaves.LOGGER.info("[BossSlaves] Loaded {} boss-slave entries.", BOSS_SLAVES.size());
        });
    }

    public record Slaves(SimpleWeightedRandomList<ResourceLocation> slaves, int maxCount) {}
}
