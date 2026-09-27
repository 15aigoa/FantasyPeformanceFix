package com.igoa.FantasyPF.mixin;

import com.igoa.FantasyPF.health.HealthDeltaEC;
import com.mega.uom.util.entity.EntityASMUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Entity#onSyncedDataUpdated は、SynchedEntityData の値が変わった時に
 * ローカルでの書き込み・ネットワーク同期パケット受信のどちらの経路でも呼ばれる
 * 共通フックなので、ここでキャッシュ無効化のトリガーにする。
 * <p>
 * 1.20.1の Entity には実は2つのオーバーロードがある:
 * <ul>
 *   <li>{@code onSyncedDataUpdated(EntityDataAccessor<?>)}
 *       … SynchedEntityData#set() によるローカルでの単発書き込み時に呼ばれる
 *       (Fantasy Ending の EntityASMUtil#setHealthDelta はこちら経由)</li>
 *   <li>{@code onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>>)}
 *       … ネットワーク同期パケット受信時(assignValues)に、まとめて複数の値が
 *       更新された際に呼ばれる。クライアント側で他エンティティの体力表示が
 *       更新されるのは主にこちら経由のはず。</li>
 * </ul>
 * メソッド名だけを指定するとMixinがどちらか一方(List版)にしか解決できず
 * クラッシュしたため、両方とも明示的なディスクリプタ指定でフックしている。
 * <p>
 * {@code EntityASMUtil.setHealthDelta} の呼び出しだけをトリガーにすると、
 * List版経由の同期(他エンティティの体力表示更新など)を取りこぼしてしまうため、
 * あえて vanilla の Entity 側の両オーバーロードで検知している。
 * <p>
 * Entity は vanillaクラスなので remap 対象(デフォルトのremap=trueのまま)。
 */
@Mixin(Entity.class)
public abstract class EntitySyncedDataMixin {

    @Inject(
            method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V",
            at = @At("HEAD")
    )
    private void fantasypf$onSyncedDataUpdatedSingle(EntityDataAccessor<?> key, CallbackInfo ci) {
        if (key == EntityASMUtil.FE_GET_HEALTH_DATA && (Object) this instanceof LivingEntity living) {
            ((HealthDeltaEC) living).markHealthDeltaDirty();
        }
    }

    @Inject(
            method = "onSyncedDataUpdated(Ljava/util/List;)V",
            at = @At("HEAD")
    )
    private void fantasypf$onSyncedDataUpdatedBulk(List<SynchedEntityData.DataValue<?>> dataValues, CallbackInfo ci) {
        EntityDataAccessor<?> target = EntityASMUtil.FE_GET_HEALTH_DATA;
        if (target == null || !((Object) this instanceof LivingEntity living)) {
            return;
        }
        int targetId = target.getId();
        for (SynchedEntityData.DataValue<?> dataValue : dataValues) {
            if (dataValue.id() == targetId) {
                ((HealthDeltaEC) living).markHealthDeltaDirty();
                break;
            }
        }
    }
}
