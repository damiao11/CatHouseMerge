# audio · 音频资源

> 规格见 [docs/15](../../docs/15-音效与BGM.md)，提示词见 [docs/16](../../docs/16-音乐提示词-直接复制.md)。
> 代码读 `manifest.json`；**每个文件的触发点与描述就在下面两张表里**，来源授权见 [CREDITS.md](../../CREDITS.md)。

## BGM / 环境声（7 个）

| 文件 | 时长 | 长度依据 | 实测 BPM | LUFS | 类型 | 体积 |
|---|---|---|---|---|---|---|
| `bgm_day.ogg` | 78.02s | 24 小节 | 73.8 | -18.0 | 循环 | 1531 KB |
| `bgm_night.ogg` | 79.88s | 20 小节 | 60.1 | -18.0 | 循环 | 1666 KB |
| `bgm_title.ogg` | 38.48s | 8 小节 | 49.9 | -18.0 | 循环 | 819 KB |
| `sting_chapter.ogg` | 3.00s | — | — | -15.9 | 单次 | 70 KB |
| `sting_ending.ogg` | 12.00s | — | — | -16.7 | 单次 | 250 KB |
| `amb_rain.ogg` | 20.00s | 环境声 | — | -24.0 | 循环 | 271 KB |
| `amb_birds.ogg` | 15.00s | 环境声 | — | -24.0 | 循环 | 203 KB |

- BGM 44.1kHz 立体声 Vorbis **q6（≈160kbps）**；环境声 **q3（≈96kbps）**。
- BGM 的循环长度取 **4 小节整数倍**，头尾 2 秒等功率交叉淡化；环境声取最均匀的一段。
- `LOOPSTART=0` / `LOOPEND=<samples>` 写在 Ogg 的 Vorbis comment 里。
- 实测 BPM 与设计值不同：`bgm_day` 73.8（设计 72）、`bgm_title` 49.9（设计 56）。
- `sting_ending` 是 -16.7 而非 -16：卡在 -3 dBTP 峰限。

## SFX（42 条，217 KB）

`sfx/` 下 41 个文件 + `build_chapter` 复用 `bgm/sting_chapter.ogg`。单声道 44.1kHz Vorbis q3（≈64kbps）。

| 文件 | 时长 | 触发 | 描述 | 峰值 dBTP | 素材来源 |
|---|---|---|---|---|---|
| `sfx_ui_tap.ogg` | 0.05s | 点击按钮 | 轻木鱼，很闷 | -3.0 | impactWood_light_000 |
| `sfx_ui_back.ogg` | 0.08s | 返回 | 同 tap，音高略低 | -3.0 | impactWood_light_002 |
| `sfx_ui_panel_open.ogg` | 0.15s | 打开面板 | 纸张展开 | -3.0 | bookOpen |
| `sfx_ui_panel_close.ogg` | 0.12s | 关闭面板 | 纸张收拢 | -3.0 | bookClose |
| `sfx_ui_toggle.ogg` | 0.06s | 开关 | 短哒 | -3.0 | toggle_001 |
| `sfx_ui_error.ogg` | 0.20s | 不可用操作 | 低沉单音 | -3.0 | lowDown |
| `sfx_item_pick.ogg` | 0.08s | 拖起物品 | 布料摩擦 | -3.0 | bed-sheets-and-blanket-rustling-… |
| `sfx_item_drop.ogg` | 0.10s | 放下物品 | 轻木落桌 | -3.0 | impactWood_medium_001 |
| `sfx_merge_low.ogg` | 0.20s | 合成 1–2 级 | 气泡啵 | -3.0 | pepSound2 |
| `sfx_merge_mid.ogg` | 0.22s | 合成 3–4 级 | 气泡 + 小铃 | -3.0 | pepSound5 |
| `sfx_merge_high.ogg` | 0.25s | 合成 5–6 级 | 铃 + 上扬和弦 | -3.0 | powerUp4 |
| `sfx_merge_invalid.ogg` | 0.18s | 合成失败 | 闷响 | -3.0 | impactSoft_heavy_002 |
| `sfx_gen_tap_wood.ogg` | 0.12s | 木材堆点击 | 木箱咚 | -3.0 | impactWood_heavy_000 |
| `sfx_gen_tap_metal.ogg` | 0.12s | 工具箱点击 | 金属轻碰 | -3.0 | impactMetal_light_000 |
| `sfx_gen_tap_fabric.ogg` | 0.12s | 布筐点击 | 布料噗 | -3.0 | cloth1 |
| `sfx_gen_tap_glass.ogg` | 0.12s | 灯泡盒点击 | 玻璃轻响 | -3.0 | impactGlass_light_000 |
| `sfx_gen_tap_ceramic.ogg` | 0.12s | 陶土袋点击 | 陶土闷响 | -3.0 | impactPlate_light_000 |
| `sfx_gen_tap_plant.ogg` | 0.12s | 种子袋点击 | 种子沙沙 | -3.0 | flax2 |
| `sfx_gen_tap_relic.ogg` | 0.12s | 旧木箱点击 | 旧木吱呀 | -3.0 | wooddoorsqueak |
| `sfx_gen_upgrade.ogg` | 0.45s | 生成器升级 | 咔 + 光晕上扬 | -3.0 | powerUp11 |
| `sfx_gen_blocked.ogg` | 0.15s | 生成器无处可放 | 短闷响 | -3.0 | impactSoft_heavy_004 |
| `sfx_order_ready.ogg` | 0.50s | 订单凑齐 | 风铃三音 | -3.0 | wind-chimes-single-04 |
| `sfx_order_complete.ogg` | 0.80s | 交付成功 | 硬币 + 猫叫 | -3.0 | handleCoins |
| `sfx_coin_fly.ogg` | 0.30s | 金币飞行 | 单枚硬币 | -3.0 | handleCoins2 |
| `sfx_build_step.ogg` | 0.40s | 建造一步 | 锤子两下 | -3.0 | impactWood_heavy_002 |
| `sfx_build_reveal.ogg` | 1.20s | 整间揭幕 | 上扬 + 铃 | -3.0 | powerUp12 |
| `sfx_store_item.ogg` | 0.15s | 收进仓库 | 布袋落地 | -3.0 | dropLeather |
| `sfx_retrieve_item.ogg` | 0.12s | 从仓库取出 | 抽出啵 | -3.0 | handleSmallLeather |
| `sfx_warehouse_full.ogg` | 0.25s | 仓库满 | 闷响 | -3.0 | impactSoft_heavy_003 |
| `sfx_bubble_appear.ogg` | 0.40s | 气泡出现 | 闪光上升 | -3.0 | phaserUp2 |
| `sfx_bubble_place.ogg` | 0.15s | 拖上棋盘 | 落位哒 | -3.0 | impactGeneric_light_001 |
| `sfx_cat_orange.ogg` | 0.50s | 大橘 | 低沉、有点懒 | -3.0 | meow9 |
| `sfx_cat_calico.ogg` | 0.40s | 花花 | 轻、细、短 | -4.5 | cat-meow |
| `sfx_cat_ragdoll.ogg` | 0.40s | 团子 | 软、拖长 | -3.0 | cat_meow |
| `sfx_cat_black.ogg` | 0.40s | 煤球 | 短促、不爱搭理 | -3.0 | cat-meowing |
| `sfx_cat_tabby.ogg` | 0.50s | 阿狸 | 野、亮 | -4.8 | cat-meow |
| `sfx_cat_huzi.ogg` | 0.50s | 虎子 | 沙哑、弱 | -3.0 | meow9 |
| `sfx_text_blip.ogg` | 0.03s | 打字机每 3 字 | 极轻 | -13.0 | tick_001 |
| `sfx_dialog_open.ogg` | 0.20s | 对话弹出 | 轻呼 | -3.0 | open_001 |
| `sfx_energy_gain.ogg` | 0.30s | 体力 +1 | 上扬小铃 | -3.0 | powerUp1 |
| `sfx_energy_empty.ogg` | 0.30s | 体力耗尽 | 低沉 | -3.0 | phaserDown1 |
| `sting_chapter.ogg` | 3.00s | 章末动画 | 复用 sting_chapter（章末动画） | — | 复用 sting_chapter |

- 长于 0.3s 的归一到 **-14 LUFS**；50ms 级瞬态按 **-3 dBTP 峰值**对齐（这类音 LUFS 读数天然偏低，别照读数调音量）。
- `text_blip` 额外压了 10dB（文档要求打字机音极轻）。
- **`cat_tabby` / `cat_huzi` 是变调派生的**（素材只下了 4 条猫叫）：阿狸 = +16% 变调提速，虎子 = -10% + 低通。建议补两条真实猫叫替换。

## 缺口

只差这两条猫叫的“真素材”。其余 42 条齐了，`build_chapter` 复用 `sting_chapter`。

## 重新处理

- SFX：`tools/make_sfx_pack.py --kenney <Kenney 解压目录> --freesound <素材目录> [id ...]`，改 `SPECS` 里的源文件名即可换素材。
- BGM：`tools/convert_bgm.py --src <mp3 文件夹>`。
