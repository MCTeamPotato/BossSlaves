package me.kall.bossslaves.mixin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import me.kall.bossslaves.ext.Boss;
import me.kall.bossslaves.ext.Slave;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements Boss, Slave {
    @Unique private boolean bossSlaves$isBoss;
    @Unique private @Nullable IntSet boss$slaves;
    @Unique private boolean boss$isSlave;
    @Unique private int boss$slaveOwner = -1;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(@NotNull EntityType<?> entityType, Level level, CallbackInfo ci) {
        this.bossSlaves$isBoss = entityType.is(Tags.EntityTypes.BOSSES);
        if (this.bossSlaves$isBoss) this.boss$slaves = IntSets.synchronize(new IntOpenHashSet());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void write(@NotNull CompoundTag compound, CallbackInfo ci) {
        compound.putBoolean("BossSlavesIsBoss", this.bossSlaves$isBoss());
        compound.putBoolean("BossSlavesIsSlave", this.boss$isSlave());
        compound.putIntArray("BossSlaves", this.boss$slaves().toIntArray());
        compound.putInt("BossSlaveOwner", this.boss$slaveOwner());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void read(@NotNull CompoundTag compound, CallbackInfo ci) {
        this.bossSlaves$isBoss = compound.getBoolean("BossSlavesIsBoss");
        this.boss$slaves = compound.contains("BossSlaves") ? IntSets.synchronize(new IntOpenHashSet(compound.getIntArray("BossSlaves"))) : null;
        this.boss$setSlave(compound.getBoolean("BossSlavesIsSlave"));
        this.boss$setSlaveOwner(compound.contains("BossSlaveOwner") ? compound.getInt("BossSlaveOwner") : -1);
    }

    @Override
    public IntSet boss$slaves() {
        return this.boss$slaves == null ? IntSets.emptySet() : this.boss$slaves;
    }

    @Override
    public boolean bossSlaves$isBoss() {
        return this.bossSlaves$isBoss;
    }

    @Override
    public boolean boss$isSlave() {
        return this.boss$isSlave;
    }

    @Override
    public void boss$setSlave(boolean isSlave) {
        this.boss$isSlave = isSlave;
    }

    @Override
    public int boss$slaveOwner() {
        return this.boss$slaveOwner;
    }

    @Override
    public void boss$setSlaveOwner(int boss) {
        this.boss$slaveOwner = boss;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void tickBoss(CallbackInfo ci) {
        if (this.bossSlaves$isBoss() && !this.boss$slaves().isEmpty()) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity.level() instanceof ServerLevel level && level.getServer().getTickCount() % 100 == 0) {
                this.boss$slaves().removeIf(id -> {
                    Entity slave = level.getEntity(id);
                    return slave == null || slave.isRemoved() || !slave.isAlive();
                });
            }
        }
    }
}