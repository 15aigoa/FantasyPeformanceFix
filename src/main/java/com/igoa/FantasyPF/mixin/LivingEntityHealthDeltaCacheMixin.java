package com.igoa.FantasyPF.mixin;

import com.igoa.FantasyPF.health.HealthDeltaEC;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * すべての LivingEntity に「Fantasy Ending 体力デルタ」キャッシュ用のフィールドを追加する Mixin。
 * 既存の LivingEntity クラス自体には一切手を入れず、新しいフィールドと
 * HealthDeltaEC インターフェースの実装だけを追加している（既存のシャドウなし）。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthDeltaCacheMixin implements HealthDeltaEC {

    @Unique
    private float fantasypf$cachedHealthDelta = 0F;

    // エンティティ生成直後はまだ一度も取得していないので true からスタートする。
    @Unique
    private boolean fantasypf$healthDeltaDirty = true;

    @Override
    public float getCachedHealthDelta() {
        return this.fantasypf$cachedHealthDelta;
    }

    @Override
    public void setCachedHealthDelta(float value) {
        this.fantasypf$cachedHealthDelta = value;
        this.fantasypf$healthDeltaDirty = false;
    }

    @Override
    public boolean isHealthDeltaDirty() {
        return this.fantasypf$healthDeltaDirty;
    }

    @Override
    public void markHealthDeltaDirty() {
        this.fantasypf$healthDeltaDirty = true;
    }
}
