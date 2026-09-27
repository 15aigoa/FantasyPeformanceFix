package com.igoa.FantasyPF.mixin;

import com.igoa.FantasyPF.health.HealthDeltaEC;
import com.mega.uom.util.entity.EntityASMUtil;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fantasy Ending の EntityASMUtil#getHealthDelta(LivingEntity) にキャッシュを効かせる Mixin。
 * <p>
 * EntityASMUtil は Fantasy Ending（Minecraft本体ではない、Modのクラス）なので
 * リマップ対象ではない。そのため remap = false を指定している。
 * <p>
 * 方針:
 * 1. HEAD: キャッシュが dirty でなければ、キャッシュ値をそのまま返して
 *    元のロジック（SynchedEntityDataのReadWriteLock取得を伴う読み取り）を丸ごとスキップする。
 * 2. RETURN: dirty だった場合は元のロジックをそのまま実行させ、
 *    計算結果を横取りしてキャッシュに書き込む（結果自体は書き換えない）。
 *    元の実装は一切コピーしていないので、将来Fantasy Ending側の実装が変わっても追従できる。
 * <p>
 * getHealthDelta をキャッシュすることで、これを内部で呼んでいる
 * special_getHealth / special_getHealthDelta / special_isAlive / special_isDeadOrDying
 * すべてが同時に恩恵を受ける。
 */
@Mixin(value = EntityASMUtil.class, remap = false)
public abstract class EntityASMUtilCacheMixin {

    @Inject(method = "getHealthDelta", at = @At("HEAD"), cancellable = true)
    private static void fantasypf$returnCachedIfClean(LivingEntity living, CallbackInfoReturnable<Float> cir) {
        if (living instanceof HealthDeltaEC ec && !ec.isHealthDeltaDirty()) {
            cir.setReturnValue(ec.getCachedHealthDelta());
        }
    }

    @Inject(method = "getHealthDelta", at = @At("RETURN"))
    private static void fantasypf$storeIntoCache(LivingEntity living, CallbackInfoReturnable<Float> cir) {
        if (living instanceof HealthDeltaEC ec) {
            Float value = cir.getReturnValue();
            ec.setCachedHealthDelta(value != null ? value : 0F);
        }
    }
}
