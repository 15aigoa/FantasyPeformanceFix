package com.igoa.FantasyPF.health;

/**
 * LivingEntity に Mixin で生やす「Fantasy Ending の体力デルタ値」のキャッシュ拡張インターフェース。
 * <p>
 * Fantasy Ending の {@code EntityASMUtil.getHealthDelta(LivingEntity)} は、
 * 呼ばれるたびに SynchedEntityData の ReadWriteLock を取得して値を読みに行く。
 * この値は「読まれる頻度は非常に高いが、書き換わる頻度は低い」性質のデータなので、
 * ここでキャッシュして無駄なロック取得を減らす。
 * <p>
 * 実装は {@link com.igoa.FantasyPF.mixin.LivingEntityHealthDeltaCacheMixin} を参照。
 */
public interface HealthDeltaEC {

    /**
     * 現在キャッシュされているdelta値を返す。
     */
    float getCachedHealthDelta();

    /**
     * delta値をキャッシュに書き込み、dirtyフラグを下ろす。
     */
    void setCachedHealthDelta(float value);

    /**
     * キャッシュが無効(再取得が必要)かどうか。
     * エンティティ生成直後はまだ一度も取得していないため true がデフォルト。
     */
    boolean isHealthDeltaDirty();

    /**
     * FE_GET_HEALTH_DATA が更新された(ローカルでの書き込み、またはネットワーク同期の
     * どちらでも)タイミングで呼び、次回の getHealthDelta 呼び出し時に再取得させる。
     */
    void markHealthDeltaDirty();
}
