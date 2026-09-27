# libs/

This folder is where the **Fantasy Ending** mod jar needs to be placed in order
to compile this project. The jar itself is **not** committed to this
repository (it is someone else's copyrighted mod, not ours to redistribute).

Download `fantasy_ending-1_20_1-2_7_20-all.jar` (or a newer compatible
version) from CurseForge and place it here:

```
libs/fantasy_ending-1_20_1-2_7_20-all.jar
```

If you use a different filename/version, no build.gradle changes are needed —
`implementation fileTree("libs")` picks up any jar placed in this folder
automatically. Just make sure the class/method signatures this mod's Mixins
target (`EntityASMUtil#getHealthDelta`, `Entity#onSyncedDataUpdated`) haven't
changed in the version you use.

---

（日本語）このフォルダにはコンパイル用に **Fantasy Ending** のjarを配置してください。
著作権の都合上、jar自体はこのリポジトリにはコミットしていません。CurseForgeから
`fantasy_ending-1_20_1-2_7_20-all.jar`（またはそれ以降の互換バージョン）を
ダウンロードしてこのフォルダに置いてください。
