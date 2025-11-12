package me.kall.bossslaves;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.bossslaves.config.SlaveConfig;
import me.kall.bossslaves.ext.Boss;
import me.kall.bossslaves.ext.Slave;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Mod(BossSlaves.MOD_ID)
public final class BossSlaves {
    public static final String MOD_ID = "bossslaves";
    public static final String MOD_NAME = "BossSlaves";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public BossSlaves(@NotNull FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SlaveConfig.INSTANCE);

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::tickBoss);
        forgeBus.addListener(EventPriority.LOWEST, this::slaveDamage);
        forgeBus.addListener(this::onBossLeave);
        forgeBus.addListener(EventPriority.LOWEST, this::bossTargetChange);
        forgeBus.addListener(EventPriority.LOW, this::slaveTargetDetection);
    }

    public void tickBoss(LivingEvent.@NotNull LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Boss boss && boss.bossSlaves$isBoss() && boss.boss$slaves().isEmpty() && entity.level() instanceof ServerLevel level && level.getServer().getTickCount() % 200 == 0) {
            SlaveConfig.Slaves configSlaves = SlaveConfig.BOSS_SLAVES.get(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
            if (configSlaves == null) return;
            SimpleWeightedRandomList<ResourceLocation> weightedSlaveList = configSlaves.slaves();
            if (weightedSlaveList.isEmpty()) return;

            List<EntityType<?>> slaves = new ObjectArrayList<>();

            for (int i = 0; i < configSlaves.maxCount(); i++) {
                slaves.add(weightedSlaveList.getRandomValue(level.getRandom()).map(ForgeRegistries.ENTITY_TYPES::getValue).orElseThrow());
            }

            for (EntityType<?> type : slaves) {
                Entity slave = type.create(level);
                if (slave == null) {
                    LOGGER.error("[BossSlaves] Failed to create slave for EntityType {}", type.toString());
                    continue;
                }

                slave.moveTo(entity.blockPosition(), entity.getYRot(), entity.getXRot());
                if (slave instanceof Mob && entity instanceof Mob) ((Mob) slave).setTarget(((Mob) entity).getTarget());

                level.addFreshEntity(slave);

                ((Slave)slave).boss$setSlave(true);
                ((Slave)slave).boss$setSlaveOwner(entity.getId());
                boss.boss$slaves().add(slave.getId());
            }
        }
    }

    public void slaveDamage(@NotNull LivingAttackEvent event) {
        if (event.isCanceled()) return;
        LivingEntity entity = event.getEntity();
        if (entity instanceof Slave slave && slave.boss$isSlave()) {
            Entity direct = event.getSource().getDirectEntity();
            Entity cause = event.getSource().getEntity();
            if ((cause instanceof Boss b1 && b1.bossSlaves$isBoss()) || (direct instanceof Boss b2 && b2.bossSlaves$isBoss())) {
                event.setCanceled(true);
            }
        }
    }

    public void bossTargetChange(@NotNull LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level && event.getNewTarget() != null) {
            if (entity instanceof Boss boss && boss.bossSlaves$isBoss() && !boss.boss$slaves().isEmpty()) {
                for (int id : boss.boss$slaves()) {
                    Entity slave = level.getEntity(id);
                    if (slave instanceof Mob) {
                        ((Mob) slave).setTarget(event.getNewTarget());
                    }
                }
            }
        }
    }

    public void slaveTargetDetection(@NotNull LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level && event.getNewTarget() != null) {
            if (entity instanceof Slave slave && slave.boss$isSlave() && level.getEntity(slave.boss$slaveOwner()) instanceof Mob boss) {
                if (boss.getTarget() == null) {
                    boss.setTarget(event.getNewTarget());
                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    public void onBossLeave(@NotNull EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Boss boss && event.getLevel() instanceof ServerLevel level) {
            boss.boss$slaves().forEach(id -> Optional.ofNullable(level.getEntity(id)).ifPresent(Entity::discard));
            boss.boss$slaves().clear();
        }
    }
}
