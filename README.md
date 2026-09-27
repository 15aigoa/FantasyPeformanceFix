# FantasyPF (fantasypf)

> **This mod was created with the assistance of [Claude](https://claude.ai) (Anthropic).**
> このModはAnthropic社のAIモデル「[Claude](https://claude.ai)」の支援を受けて作成されました。

---

## English

Fantasy Ending's `EntityASMUtil.getHealthDelta(LivingEntity)` acquires a
`SynchedEntityData` ReadWriteLock on every single call. This method is
called from nearly every `LivingEntity#getHealth()` call in the game
(redirected there via ASM, through `special_getHealth`), as well as from
`special_getHealthDelta` / `special_isAlive` / `special_isDeadOrDying`.
Because health bars, nameplates, and other rendering code call `getHealth()`
extremely often, this added up to a non-trivial cost on the render thread.

This mod is an **external Mixin patch** — it does not modify Fantasy
Ending's own source at all. It works by:

1. Adding a cached delta-value field to `LivingEntity` (`HealthDeltaEC`)
2. Hooking `Entity#onSyncedDataUpdated` (the common callback fired for both
   local writes *and* incoming network sync packets) to invalidate the
   cache only when `FE_GET_HEALTH_DATA` actually changes
3. Injecting into `EntityASMUtil#getHealthDelta` so that, when the cache is
   valid, the lock-based read is skipped entirely and the cached value is
   returned immediately

### Why not just hook `setHealthDelta`?

`FE_GET_HEALTH_DATA` is network-synced data. On the client, its value can
change purely from an incoming sync packet — without `setHealthDelta` ever
being called locally (e.g. when another player's or mob's health is synced
to you). Relying on `setHealthDelta` alone would miss that path and leave
stale cached values on the client, so this mod hooks vanilla
`Entity#onSyncedDataUpdated` instead, which fires for both paths. Note that
in Minecraft 1.20.1, `onSyncedDataUpdated` has **two overloads**
(`EntityDataAccessor<?>` for local writes, `List<SynchedEntityData.DataValue<?>>`
for batched network sync) — both are hooked explicitly by full method
descriptor to avoid Mixin's ambiguous by-name resolution.

### Building

1. Place a compatible `fantasy_ending-*-all.jar` in `libs/` (see
   `libs/README.md` — the jar is not committed to this repo).
2. Open the project in IntelliJ IDEA as a Gradle project (`./gradlew` is
   included).
3. `./gradlew build` produces the mod jar under `build/libs/`.
4. `runClient` may crash on unrelated, environment-specific Mixin issues
   inside Fantasy Ending itself (unrelated to this mod) — dropping the
   built jar directly into a real modpack's `mods` folder alongside
   Endinglib + Fantasy Ending is the recommended way to verify behavior.

### Updating for a newer Fantasy Ending version

- Replace the jar in `libs/`.
- Confirm `EntityASMUtil#getHealthDelta(LivingEntity): float` and
  `Entity#onSyncedDataUpdated` signatures haven't changed.
- Adjust the `fantasy_ending` version range in `mods.toml` if needed.

### Known limitations

- `special_isAlive` / `special_isDeadOrDying` also call `getHealthDelta`
  internally, so they benefit from this cache automatically. The same
  invalidation logic applies, but please report anything that looks off
  around health-based alive/dead checks.
- Scope is intentionally limited to `getHealthDelta`. Other costs found
  during profiling (`PostProcessingShaders#renderShaders`/`canUse`,
  GoetyRevelationFix's `ClientEventsHandler#drawTrails`) are out of scope
  for this mod.

---

## 日本語

Fantasy Ending の `EntityASMUtil.getHealthDelta(LivingEntity)` は、呼ばれるたびに
`SynchedEntityData`のReadWriteLockを取得してから値を読みに行く実装になっています。
このメソッドはASMによって`LivingEntity#getHealth()`のほぼ全呼び出し（`special_getHealth`
経由）や、`special_getHealthDelta`/`special_isAlive`/`special_isDeadOrDying`からも
呼ばれるため、体力バーやネームプレート表示などRender thread側の描画コードから
極めて高頻度に叩かれ、無視できないコストになっていました。

このModは **Fantasy Ending本体のソースは一切書き換えず**、外付けのMixinパッチとして

1. `LivingEntity`にdelta値のキャッシュ用フィールドを追加 (`HealthDeltaEC`)
2. `Entity#onSyncedDataUpdated`（ローカルでの書き込み・ネットワーク同期パケット受信の
   どちらでも呼ばれる共通フック）を検知して、`FE_GET_HEALTH_DATA`が実際に更新された
   時だけキャッシュを無効化する
3. `EntityASMUtil#getHealthDelta`にMixinを差し込み、キャッシュが有効なら
   ReadWriteLockを伴う読み取りそのものをスキップして即座にキャッシュ値を返す

という形で実現しています。

### なぜsetHealthDeltaの呼び出しだけをトリガーにしなかったか

`FE_GET_HEALTH_DATA`はネットワーク同期対象のデータです。クライアント側では、
`setHealthDelta`をローカルで一度も呼ばなくても、サーバーからの同期パケット受信
だけでこの値が書き換わるケースがあります（他プレイヤーやMobの体力表示など）。
`setHealthDelta`の呼び出しだけをトリガーにすると、このケースを取りこぼして
クライアント側で古い値が表示され続けるバグになるため、あえて vanilla の
`Entity#onSyncedDataUpdated`側で検知する設計にしています。1.20.1では
`onSyncedDataUpdated`に**2つのオーバーロード**（ローカル書き込み用の
`EntityDataAccessor<?>`版、ネットワーク同期パケットの一括反映用の
`List<SynchedEntityData.DataValue<?>>`版）が存在するため、Mixinの
あいまいな名前解決を避けるべく両方とも完全なディスクリプタ指定でフックしています。

### ファイル構成

```
FantasyPF/
├── build.gradle
├── settings.gradle
├── gradle.properties          (forge_version=47.4.23。実際のmodpackがこのバージョンで
│                                動作していることをspark profileのmetadataで確認済み)
├── gradlew / gradlew.bat
├── gradle/wrapper/
├── libs/
│   └── README.md              (jar本体は著作権の都合上コミットしていません)
└── src/main/
    ├── java/com/igoa/FantasyPF/
    │   ├── FantasyPF.java                  Modエントリポイント
    │   ├── health/
    │   │   └── HealthDeltaEC.java          キャッシュ用インターフェース
    │   └── mixin/
    │       ├── LivingEntityHealthDeltaCacheMixin.java  LivingEntityにキャッシュフィールド追加
    │       ├── EntitySyncedDataMixin.java               onSyncedDataUpdated検知でdirty化
    │       └── EntityASMUtilCacheMixin.java             getHealthDeltaにキャッシュ判定を注入
    └── resources/
        ├── META-INF/mods.toml
        ├── fantasypf.mixins.json
        └── pack.mcmeta
```

### ビルド手順

1. `libs/README.md`の指示に従い、対応するバージョンの`fantasy_ending-*-all.jar`を
   `libs/`に配置する（著作権の都合上、jar自体はこのリポジトリにコミットしていません）。
2. IntelliJ IDEAでこのフォルダをGradleプロジェクトとして開く（`gradlew`同梱済み）。
3. `./gradlew build`で`build/libs/`にModのjarが生成されます。
4. `runClient`はFantasy Ending本体側の環境依存のMixin問題（このModとは無関係）で
   落ちることがあります。実際のmodpackの`mods`フォルダにビルドしたjarを
   Endinglib・Fantasy Endingと一緒に入れて動作確認する方法を推奨します。

### Fantasy Endingのバージョンを更新する場合

- `libs/`のjarを新しいものに差し替える
- `EntityASMUtil#getHealthDelta(LivingEntity): float`と
  `Entity#onSyncedDataUpdated`のシグネチャが変わっていないか確認する
  （変わっていなければMixin側の修正は不要）
- `mods.toml`の`[[dependencies.fantasypf]]`の`fantasy_ending`側`versionRange`を
  必要に応じて調整する

### 既知の注意点

- `special_isAlive`/`special_isDeadOrDying`も内部で`getHealthDelta`を呼んでいるため、
  今回のキャッシュの恩恵を自動的に受けます。無効化ロジックは同じなので基本問題ない
  はずですが、体力に応じた生死判定まわりで挙動がおかしいと感じたら教えてください。
- 今回はスコープを`getHealthDelta`一点に絞っています。プロファイルで見つかった
  `PostProcessingShaders#renderShaders`/`canUse`や`ClientEventsHandler#drawTrails`
  （GoetyRevelationFix側）は今回のパッチ範囲外です。
