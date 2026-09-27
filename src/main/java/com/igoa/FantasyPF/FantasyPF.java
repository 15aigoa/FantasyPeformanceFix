package com.igoa.FantasyPF;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FantasyPF (modid: fantasypf)
 * <p>
 * Fantasy Ending の {@code EntityASMUtil.getHealthDelta(LivingEntity)} は、
 * 呼ばれるたびに SynchedEntityData の ReadWriteLock を取得して値を読みに行く実装になっており、
 * この関数はゲーム内のあらゆる getHealth() 呼び出し(ASMによる差し替え経由)から
 * 大量に呼ばれるため、Render threadで無視できないコストになっていた。
 * <p>
 * このModは Mixin により「delta値が実際に更新された時だけ再取得する」キャッシュを
 * 外付けで追加している。Fantasy Ending本体のコードは一切書き換えていない。
 */
@Mod(FantasyPF.MODID)
public class FantasyPF {
    public static final String MODID = "fantasypf";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public FantasyPF() {
        LOGGER.info("[FantasyPF] Fantasy Ending health-delta cache patch loaded.");
    }
}
