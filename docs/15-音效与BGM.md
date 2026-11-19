# 15 · 音频规范与制作（音效、BGM与提示词）

> 视觉规范在 [06](06-UI-UX设计规范.md)，这里只管声音。清单同时给了一份 `docs/data/audio.json`，代码可以直接读。

## 1. 声音基调

三个词：**木、暖、轻**。

| 用 | 不用 |
|---|---|
| 木吉他、钢琴、木琴、沙锤、轻刷鼓、环境声（雨、鸟、风） | 电子合成器、鼓机、失真吉他、人声演唱、8-bit |

目标感觉：**下午的旧屋**，不是游戏厅。
所有音效都要"短、干、不抢戏"——玩家一局要听几百次，刺耳就完了。

## 2. 混音规范

| 项 | 值 |
|---|---|
| BGM 响度 | -18 LUFS，峰值 ≤ -3 dBFS |
| SFX 响度 | -14 LUFS，峰值 ≤ -3 dBFS |
| 对话时 BGM | 200ms 内 duck 到 -6dB，结束后 400ms 恢复 |
| 同屏 SFX | 最多 8 个；同类合成音最多 3 个 |
| 重复触发 | 同一音效 60ms 内不重复；音高随机 ±3% |
| 静音 | BGM / SFX 分开控制，都要能单独关 |

## 3. BGM（3 首循环 + 2 段单次）

| id | 用途 | 编制 | BPM | 时长 | 循环 |
|---|---|---|---|---|---|
| `bgm_day` | 主界面，D1–D18 与 D22–D24 | 木吉他分解和弦 + 钢琴 + 沙锤 | 72 | 90s | 是 |
| `bgm_night` | D19–D21 阁楼 | 钢琴 + 弦乐垫 + 远处雨声 | 60 | 80s | 是 |
| `bgm_title` | 启动画面 | 钢琴单音 + 环境 | 56 | 40s | 是 |
| `sting_chapter` | 第 6 / 12 / 18 天章末 | 上扬三音 + 铃 | — | 3s | 否 |
| `sting_ending` | 结局 | 主题再现 + 长尾 | — | 12s | 否 |

**循环要求**：首尾各留 2 秒可无缝衔接，导出时标 loop point。`bgm_day` 要经得起循环 20 次以上不烦——编曲上少用明显旋律，多用分解和弦。

> 用 AI 音乐工具（Gemini / Lyria）出这几段时，「无缝」得自己后处理：循环长度取 4 小节整数倍、头尾 2 秒等功率交叉淡化、再归一化到 -18 LUFS。具体提示词与步骤见 [音乐制作与提示词](#music-production)。

## 4. SFX 清单（44 条）

### UI（6）

| id | 触发 | 时长 | 描述 |
|---|---|---|---|
| `ui_tap` | 点按钮 | 0.05s | 轻木鱼，很闷 |
| `ui_back` | 返回 | 0.08s | 同 tap，音高略低 |
| `ui_panel_open` | 打开面板 | 0.15s | 纸张展开 |
| `ui_panel_close` | 关闭面板 | 0.12s | 纸张收拢 |
| `ui_toggle` | 开关 | 0.06s | 短"哒" |
| `ui_error` | 不可用操作 | 0.20s | 低沉单音，不刺耳 |

### 合成（6）

| id | 触发 | 时长 | 描述 |
|---|---|---|---|
| `item_pick` | 拖起物品 | 0.08s | 布料摩擦 |
| `item_drop` | 放下物品 | 0.10s | 轻木落桌 |
| `merge_low` | 合成 1–2 级 | 0.20s | 气泡"啵" |
| `merge_mid` | 合成 3–4 级 | 0.22s | 气泡 + 小铃 |
| `merge_high` | 合成 5–6 级 | 0.25s | 铃声 + 上扬和弦 |
| `merge_invalid` | 合成失败 | 0.18s | 闷响，短 |

> 也可以用**同一个采样 + 变速**（1.0 / 1.12 / 1.25）做出三档，省两个文件。

### 生成器（9）

| id | 触发 | 时长 | 描述 |
|---|---|---|---|
| `gen_tap_wood` | 木材堆 | 0.12s | 木箱"咚" |
| `gen_tap_metal` | 工具箱 | 0.12s | 金属轻碰 |
| `gen_tap_fabric` | 布筐 | 0.12s | 布料"噗" |
| `gen_tap_glass` | 灯泡盒 | 0.12s | 玻璃轻响 |
| `gen_tap_ceramic` | 陶土袋 | 0.12s | 陶土闷响 |
| `gen_tap_plant` | 种子袋 | 0.12s | 种子沙沙 |
| `gen_tap_relic` | 旧木箱 | 0.12s | 旧木吱呀 |
| `gen_upgrade` | 升级成功 | 0.45s | "咔" + 光晕上扬 |
| `gen_blocked` | 无处可放 | 0.15s | 短闷响 |

**七种材质音是这个游戏手感的重点**——同一条链听久了不烦，靠的就是它。

### 订单（3）

| id | 触发 | 时长 | 描述 |
|---|---|---|---|
| `order_ready` | 订单凑齐 | 0.50s | 风铃三音 |
| `order_complete` | 交付成功 | 0.80s | 硬币 + 猫叫 |
| `coin_fly` | 金币飞行 | 0.30s | 单枚硬币，很轻 |

### 建造（3）

| id | 触发 | 时长 | 描述 |
|---|---|---|---|
| `build_step` | 建造一步 | 0.40s | 锤子两下 |
| `build_reveal` | 整间揭幕 | 1.20s | 上扬 + 铃，最长的一个 |
| `build_chapter` | 章末动画 | 3.00s | 同 `sting_chapter`，可复用 |

### 仓库与气泡（5）

| id | 触发 | 时长 | 描述 |
|---|---|---|---|
| `store_item` | 收进仓库 | 0.15s | 布袋落地 |
| `retrieve_item` | 取出 | 0.12s | 抽出"啵" |
| `warehouse_full` | 仓库满 | 0.25s | 闷响 |
| `bubble_appear` | 新工具气泡出现 | 0.40s | 闪光上升 |
| `bubble_place` | 拖上棋盘 | 0.15s | 落位"哒" |

### 猫（6）

| id | 猫 | 时长 | 音色 |
|---|---|---|---|
| `cat_orange` | 大橘 | 0.50s | 低沉、有点懒 |
| `cat_calico` | 花花 | 0.40s | 轻、细、短 |
| `cat_ragdoll` | 团子 | 0.40s | 软、拖长 |
| `cat_black` | 煤球 | 0.40s | 短促、不爱搭理 |
| `cat_tabby` | 阿狸 | 0.50s | 野、亮 |
| `cat_huzi` | 虎子 | 0.50s | 沙哑、弱 |

**六只要能闭眼区分**。这是角色感最容易加分的地方。

### 对话与体力（4）

| id | 触发 | 时长 | 描述 |
|---|---|---|---|
| `text_blip` | 打字机每 3 个字 | 0.03s | 极轻，音量 -20dB |
| `dialog_open` | 对话弹出 | 0.20s | 轻"呼" |
| `energy_gain` | 体力 +1 | 0.30s | 上扬小铃 |
| `energy_empty` | 体力耗尽 | 0.30s | 低沉 |

### 环境（2）

| id | 用途 | 时长 | 说明 |
|---|---|---|---|
| `amb_rain` | 第 1 天门廊 | 20s | 循环，音量很低 |
| `amb_birds` | 院子（D16–18） | 15s | 循环，白天鸟叫 |

## 5. 声音与画面的对应（关键几条）

| 画面 | 声音 |
|---|---|
| 合成弹跳 | `merge_*` + 短震动（10ms） |
| 生成器升级换形 | `gen_upgrade` + 中震动（20ms） |
| 建造揭幕擦除 | `build_reveal` + 轻震动 |
| 订单卡翻面 | `order_complete` |
| 对话开场 | `dialog_open` + BGM duck |

**震动和声音要成对出现**，只有声音没有震动会显得"轻飘飘"。

## 6. 资源规格

| 项 | 规格 |
|---|---|
| 格式 | OGG Vorbis（Android 原生支持）；< 100ms 的音效用 WAV |
| 采样率 | 44.1kHz；低端机可降到 22.05kHz |
| 声道 | SFX 单声道；BGM 立体声 |
| 码率 | SFX 96kbps；BGM 160kbps |
| 大小 | 单个 < 300KB；音频总量 < 6MB |
| 命名 | `sfx_ui_tap.ogg` / `bgm_day.ogg` / `amb_rain.ogg` |

## 7. 实现建议（Android）

- **SFX 用 `SoundPool`**：启动时预加载 44 个短音，`maxStreams = 8`，`AudioAttributes.USAGE_GAME`。
- **BGM 用 `MediaPlayer` 或 `ExoPlayer`**：`setLooping(true)`，切歌用 300ms 淡入淡出，避免爆音。
- **音频焦点**：来电 / 其他 App 播放时暂停 BGM，回来再继续。
- **静音开关**：设置里分开存 `bgmOn` / `sfxOn`（DataStore）。
- **预加载时机**：启动画面，别在进主界面时才加载（会卡一下）。
- **低端机兜底**：如果 `SoundPool` 加载失败，退化成只播 BGM，不崩。

## 8. 免费素材来源

| 站点 | 适合 | 注意 |
|---|---|---|
| Pixabay（Music / Sound Effects） | BGM、环境声 | 免费商用，不用署名 |
| Freesound | 材质音、猫叫 | 看清 CC0 还是 CC-BY，CC-BY 要署名 |
| Kenney（Audio 包） | UI 音、合成音 | CC0，最省事 |
| OpenGameArt | BGM | 授权混杂，逐个确认 |
| itch.io（免费音效包） | 全套 | 看作者说明 |

**所有用到的素材记进 `CREDITS.md`**（名称 / 作者 / 链接 / 授权），答辩时被问到能直接拿出来。

## 9. 最小可用集（先做这 12 个）

时间不够时，先做这些就能跑：

```
ui_tap, item_pick, merge_low, merge_mid, merge_high,
gen_tap_wood, order_complete, build_reveal,
cat_orange, cat_calico, text_blip, bgm_day
```

其余用同一个音效变调顶替，等有空再补。

## 10. 验收清单（录音/素材拿到后逐条打勾）

- [ ] 连续合成 20 次，不觉得吵
- [ ] 六只猫的叫声能闭眼区分
- [ ] 静音后游戏仍可完整进行
- [ ] BGM 循环接缝处没有"啪"的爆音
- [ ] 对话弹出时 BGM 会压低，结束后恢复
- [ ] 音频总量 < 6MB
- [ ] 所有素材来源已记录

## 11. 怎么真的做出来

规格到文件之间还差一步：**怎么生成**。分两条路，都写在 [音乐制作与提示词](#music-production)：

| 类别 | 做法 | 落在哪 |
|---|---|---|
| 3 首循环 BGM + 2 段 sting + 2 条环境声（7 个文件） | 用 Gemini 音乐（Lyria）生成，提示词直接粘；生成后裁小节、交叉淡化、归一化 | `android-assets/audio/bgm/`、`amb/` |
| 44 条 SFX | 音乐模型做不了 50ms 的点击；已用 Kenney + Freesound 的 CC0 素材裁好（映射与裁剪脚本 `tools/make_sfx_pack.py`，换素材可重跑） | `android-assets/audio/sfx/`（41 个）+ `build_chapter` 复用 `sting_chapter` |

`docs/data/audio_prompts.json` 是制作提示词的机器可读版，`tools/gen_audio.py` 生成的 `audio.json` 是给代码读的清单，两者按 `id` 一一对应。



<a id="music-production"></a>

## 附录：音乐制作与提示词（原16）

> 本节保留原16制作记录；第三方工具限制属于当时经验，不表示已重新核实当前服务。正式App接入状态见HANDOFF，已有音频文件不代表已接入播放。下文“15”指本文件正文。

> 规格在 [15 音效与 BGM](15-音效与BGM.md)，这里只干一件事：把 15 里的 3 首循环 BGM + 2 段单次 + 2 条环境声，变成能直接粘进 Gemini 音乐框的提示词，并且照顾到那个工具的五条硬限制。
> 机器可读版：[`docs/data/audio_prompts.json`](data/audio_prompts.json)

### 0. 先认清工具的五条硬限制

| # | 限制 | 对你的影响 | 对策 |
|---|---|---|---|
| 1 | 「时长」只有**短版 / 标准**两档，没有秒数可填 | 15 要的 90s / 80s / 40s 它不会给准 | BGM 与环境声选**标准**、sting 选**短版**，循环段自己裁（§3） |
| 2 | 流派是固定下拉 | 「摇滚 / 电子 / 嘻哈」这些一选就跑偏 | **全部选「自定义」**，用提示词锁乐器；预设只当备选（§2 表） |
| 3 | 只有「纯音乐 / 含人声」 | 偶尔会冒哼唱 | 选纯音乐，提示词里再写一遍 instrumental |
| 4 | 不给无缝循环，也不保证 BPM 与时长 | 接缝处会「啪」，速度会漂 | 后处理裁小节 + 2s 等功率交叉淡化（§4） |
| 5 | 每次结果都不同，而且默认有「作品感」 | 会给你前奏、收尾、主旋律 hook —— 正好是游戏 BGM 最不要的 | 每条生成 3 版挑 1；提示词里明令禁止 intro / outro / cadence / fade |

另外两条要知道：**不模仿指定艺人**（写「像某某的歌」没用）；**输出带 SynthID 水印**（不影响用，但 CREDITS 里要写「AI 生成」）。

### 1. 通用模板

每条提示词都由四段拼成，别省：

```
【布料】音乐风格 + 乐器 + BPM + 拍号 + 调性
【场景】这是什么地方、什么时间、什么情绪（15 的「木、暖、轻」）
【反例】无主旋律 / 无推进 / 无鼓机 / 不刺耳
【尾巴】一上来就是织体，到最后同样力度：不要前奏、不要尾奏、不要终止式、不要淡出
```

**否定清单**（已经写进下面的提示词，改的时候别删）：

```
no vocals, no lyrics, no choir, no electronic synth pads, no drum machine,
no EDM, no 8-bit, no distorted guitar, no brass hits, no build-up, no fade-out
```

### 2. 七条提示词

> 时长档：BGM 与环境声一律选 **标准**，单次音效（sting）选 **短版**。标准版每次出多长不固定，先量再裁（§3）。
> 流派：下表首选「自定义」，是为了让提示词独占乐器清单；但你直接选预设（**原声 / 氛围音乐 / 电影配乐**）也可以，提示词里编制已经写死。两种都试过，哪个像就用哪个。

#### 2.1 `bgm_day` · 主界面

| 项 | 值 |
|---|---|
| 流派 | 自定义 →（原声 / 低保真） |
| 时长档 | 标准 |
| 取用 | 24 小节 = 80.0s（§3） |
| 循环 | 是 |

**英文（推荐粘这个）**

```
Cozy acoustic instrumental loop for a home-renovation game. 72 BPM, 4/4, warm
key of C major. Fingerpicked steel-string acoustic guitar plays a gentle
I-V-vi-IV arpeggio; soft upright piano adds sparse single notes high in the
register; light shaker on eighth notes; brushed upright bass on root and fifth.
Late-afternoon sunlight in an old wooden house - intimate, unhurried, a little
nostalgic, never sad. Steady repeating texture, no lead melody, no build-ups,
no fills. Dry and close, small room reverb, no brightness, no harshness.
Instrumental only, no vocals. Start straight on the pattern and keep the same
energy to the very last second: no intro, no outro, no final cadence, no
fade-out.
```

**中文**

```
家用修缮游戏的温馨原声循环。72 BPM，4/4，C 大调。钢弦木吉他分解和弦
I–V–vi–IV，立式钢琴在高音区点稀疏单音，沙锤走八分，擦弦贝斯根音加五度。
旧木屋下午的阳光：亲密、不着急、有点怀旧但不明亮伤感。织体稳定重复，
不要主旋律、不要推进、不要加花。干声、近场、小房间混响，不刺耳。
纯器乐，无人声。一上来就是织体，到最后同样力度：不要前奏、不要尾奏、
不要终止式、不要淡出。
```

> 这条要循环 20 次以上不烦，所以**故意不要旋律**。生成 3 版，挑「最不像一首歌」的那版。

#### 2.2 `bgm_night` · 阁楼 D19–D21

| 项 | 值 |
|---|---|
| 流派 | 自定义 →（氛围音乐 / 古典） |
| 时长档 | 标准 |
| 取用 | 20 小节 = 80.0s |
| 循环 | 是 |

**英文**

```
Quiet late-night instrumental for the attic chapter of a story game. 60 BPM,
4/4, A minor with a warm major lift. Felt piano plays slow sparse chords with
long sustain; a low string pad of cello and viola holds underneath; faint rain
on a window and soft room tone, no thunder. Intimate, melancholic but safe -
like sitting on the floor under a bare bulb. No percussion, no melody hook, no
swells that resolve. Very soft dynamics, wide natural reverb. Instrumental, no
vocals. No intro, no outro, no fade-out; keep it even from start to finish.
```

**中文**

```
剧情游戏阁楼章节的安静夜景。60 BPM，4/4，a 小调，偶尔抬到明亮的大三和弦。
毡化钢琴慢慢地弹稀疏和弦、长延音；低音弦乐垫（大提琴 + 中提琴）在下面托着；
窗外有很轻的雨和房间底噪，不打雷。亲密、忧郁但安全——像坐在裸灯泡下的地板上。
没有打击乐、没有旋律钩子、没有「解决式」的渐强。动态很轻，宽的自然混响。
纯器乐，无人声。不要前奏、不要尾奏、不要淡出，从头到尾一样平。
```

#### 2.3 `bgm_title` · 启动画面

| 项 | 值 |
|---|---|
| 流派 | 自定义 →（氛围音乐） |
| 时长档 | 标准 |
| 取用 | 8 小节 = 34.3s（嫌短用 12 小节 = 51.4s） |
| 循环 | 是 |

**英文**

```
Minimal title-screen instrumental. 56 BPM, sparse solo piano, single notes
with long pedal decay in a large empty room; one soft low drone; faint wooden
creaks and birds far outside a window. Wistful and still, a lot of silence
between notes. Instrumental, no vocals, no drums, no melody development.
No intro or outro, no fade-out, one quiet level throughout.
```

**中文**

```
极简启动画面。56 BPM，独奏钢琴，单音、长踏板、大而空的房间；一条很轻的低音
持续音；远处木头吱呀和窗外的鸟。怅然、静止，音符之间留很多空白。
纯器乐，无人声，无鼓，不做旋律发展。不要前奏尾奏、不要淡出，全程一个音量。
```

#### 2.4 `sting_chapter` · 第 6 / 12 / 18 天章末（3s）

| 项 | 值 |
|---|---|
| 流派 | 自定义 →（电影配乐） |
| 时长档 | 短版（取前 3 秒） |
| 循环 | 否 |

**英文**

```
Three-note ascending chime: soft glockenspiel with a small bell shimmer,
notes rising step by step, each with a quick natural decay, warm and hopeful,
finished within three seconds. No music bed, no drums, instrumental.
```

**中文**

```
三音上行的钟琴：柔和的钟琴带一点铃的泛音，逐级上行，每个音自然快速衰减，
温暖、有希望，三秒内结束。不要伴奏铺底，不要鼓，纯器乐。
```

> 3 秒的东西模型常常前摇两秒。生成后**只截最后 3 秒**，前摇全扔。真做不出来就退化成 3 个单音自己拼（比这更可控）。

#### 2.5 `sting_ending` · 结局（12s）

| 项 | 值 |
|---|---|
| 流派 | 自定义 →（电影配乐） |
| 时长档 | 短版 |
| 循环 | 否 |

**英文**

```
A twelve-second closing cue. Solo piano states a simple four-note motif, a
soft string swell joins underneath, resolving on a warm major chord with a
long natural reverb tail. Tender, final, like closing the door on a good
summer. Instrumental, no vocals, no drums.
```

**中文**

```
十二秒的收尾。钢琴独奏一个四音动机，低音弦乐在下面慢慢铺开，最后落在温暖的
大三和弦上，留长自然混响尾巴。温柔、终章感，像给一个好夏天关上门。
纯器乐，无人声，无鼓。
```

#### 2.6 `amb_rain` · 第 1 天门廊（20s 循环）

| 项 | 值 |
|---|---|
| 流派 | 自定义 →（氛围音乐） |
| 时长档 | 标准 |
| 取用 | 20.0s |
| 循环 | 是 |

**英文**

```
Field-recording ambience: steady medium rain on a wooden porch roof, water
dripping from the eaves, wet leaves moving, no thunder, no music, no voices,
no birds. Continuous even texture, loopable.
```

**中文**

```
实景录音式的环境声：中等强度的雨打在木头门廊顶上，屋檐滴水，湿叶子在动。
不要雷、不要音乐、不要人声、不要鸟。质地连续均匀，可循环。
```

#### 2.7 `amb_birds` · 院子 D16–D18（15s 循环）

| 项 | 值 |
|---|---|
| 流派 | 自定义 →（氛围音乐） |
| 时长档 | 标准 |
| 取用 | 15.0s |
| 循环 | 是 |

**英文**

```
Morning courtyard ambience: sparrows chirping and one distant collared dove,
light breeze in leaves, very faint far-off street noise, no music, no voices,
no rain. Gentle, consistent, loopable.
```

**中文**

```
清晨院子的环境声：麻雀叽喳，远处一只斑鸠，树叶间有微风，极轻的远处街道声。
不要音乐、不要人声、不要雨。温和、均匀、可循环。
```

### 3. 时长按小节取，别按秒

15 里写的 90s / 80s / 40s 是设计值；72 BPM 下 90 秒 = 27 小节，正好切在 4 小节和弦循环的**中间**，循环接缝一定错。所以循环长度取 4 小节的整数倍：

| 曲目 | BPM | 1 小节 | 4 小节 | 15 的目标 | **实际取** | 说明 |
|---|---|---|---|---|---|---|
| `bgm_day` | 72 | 3.33s | 13.33s | 90s | **24 小节 = 80.0s** | 6 个和弦循环；离 90s 最近的另一个是 28 小节 = 93.3s |
| `bgm_night` | 60 | 4.00s | 16.00s | 80s | **20 小节 = 80.0s** | 正好，不用改 |
| `bgm_title` | 56 | 4.29s | 17.14s | 40s | **8 小节 = 34.3s** | 40s = 9.33 小节；要长一点取 12 小节 = 51.4s |
| `sting_chapter` | — | — | — | 3s | 3.0s | 直接截 |
| `sting_ending` | — | — | — | 12s | 12.0s | 直接截 |
| `amb_rain` | — | — | — | 20s | 20.0s | 无条件循环，接缝靠淡化 |
| `amb_birds` | — | — | — | 15s | 15.0s | 同上 |

**BPM 别信提示词。** 模型经常给 69 或 74。拿到音频后在 Audacity 里量 32 个小节的总长，除回去算真实 BPM，再乘出小节长度——差 1 BPM，80 秒就会漂掉一整拍。

**「标准」有多长也不确定。** 每次都不同，先量这一版的真实长度，再取「不超过它、且是 4 小节整数倍」的最大值。表里的 80s / 34.3s 是理想值；标准版不够长就往下退一档（`bgm_day` 退到 16 小节 = 53.3s，`bgm_night` 退到 12 小节 = 48s）。

`android-assets/audio/` 的清单里 `seconds` 记**实测值**，别抄设计值。

### 4. 拿到音频后的五步

**① 转 44.1k 立体声 WAV**（后面都好处理）

```
ffmpeg -i raw.mp3 -ar 44100 -ac 2 raw.wav
```

**② 裁 4 小节整数倍**（Audacity 里选中 → 裁剪；起点对齐强拍，也就是第 1 拍落在波形最「鼓」的那一下）

**③ 头尾 2 秒等功率交叉淡化**（这一步决定有没有「啪」）

把「开头 2 秒」接到「结尾」，在两秒交叠区做等功率淡化，线性接缝就没了：

```
ffmpeg -i cut.wav -filter_complex "[0:a]asplit=2[a][b];[a]atrim=start=2,asetpts=PTS-STARTPTS[body];[b]atrim=start=0:end=2,asetpts=PTS-STARTPTS[head];[body][head]acrossfade=d=2:c1=tri:c2=tri" -c:a pcm_s16le loop.wav
```

做完长度短 2 秒，正常（就是拿掉的那 2 秒）。Audacity 的做法：复制前 2 秒贴到末尾 → 选中末尾 4 秒 → 效果 → 交叉淡化 → 删掉开头多出的 2 秒。

**④ 响度归一化**

| 素材 | 目标 | 峰值 |
|---|---|---|
| BGM | -18 LUFS | ≤ -3 dBTP |
| SFX | -14 LUFS | ≤ -3 dBTP |

```
ffmpeg -i loop.wav -af "loudnorm=I=-18:TP=-3:LRA=11" -c:a pcm_s16le norm.wav
```

**⑤ 导 OGG 并标循环点**

```
ffmpeg -i norm.wav -c:a libvorbis -q:a 6 -ar 44100 -ac 2 bgm_day.ogg
```

`-q:a 6` ≈ 160kbps，跟 15 的规格对齐。文件名按 15 §6：`bgm_day.ogg` / `amb_rain.ogg`。

### 5. 验收（在 15 的清单上补四条）

- [ ] 真实 BPM 与设计值误差 < 1%（量 32 小节）
- [ ] 循环接缝放大看没有阶跃，连播 20 次听不出「又开始了」
- [ ] 实测 -18 ± 1 LUFS，峰值没超过 -3 dBTP
- [ ] 三段 BGM 互相对比：响度一致、音色是一家人（同一套乐器描述）

### 6. 记进 `CREDITS.md`

AI 生成的素材，答辩时最容易被问「这是哪来的」，写清楚：

```markdown
| 文件 | 来源 | 模型 / 作者 | 日期 | 说明 |
|---|---|---|---|---|
| bgm_day.ogg | AI 生成 | Gemini（Lyria 3 Pro） | 2026-11-11 | 提示词见 docs/16 §2.1，含 SynthID 水印 |
| amb_rain.ogg | AI 生成 | Gemini（Lyria 3 Pro） | 2026-11-11 | 提示词见 docs/16 §2.6 |
```

### 7. 那 44 条 SFX 为什么不走这条路

音乐模型出不了 50ms 的「哒」和「木箱咚」——它一次至少给十几秒、带结构的音乐。44 条 SFX 只有三条路：

| 路 | 适合 | 代价 |
|---|---|---|
| **自己写脚本合成**（Python + numpy） | 想完全定制、零版权 | 猫叫很难做像；本次没走这条，用的是 CC0 素材 |
| **CC0 素材包**（Kenney / Pixabay） | UI、金币、纸张 | 要挑、要统一响度、要记来源 |
| **实录** | 六只猫、木头、金属 | 最好听，但要设备和人 |

七种材质音是这个游戏手感的重点，给声音同学的参数在这（脚本里也是这套）：

| 音 | 主频 | 衰减 | 噪声占比 | 关键处理 |
|---|---|---|---|---|
| 木 | 180–260Hz | 0.10s | 0.35 | 带通 800Hz，短促，尾巴留一点房间 |
| 金属 | 1.6–2.8kHz 泛音 | 0.35s | 0.15 | 三个不成谐波比的正弦（2.7 / 5.4 / 8.1 倍），各自衰减不同 |
| 布 | — | 0.07s | 0.95 | 低通 1.2kHz，8ms 淡入去「啪」 |
| 玻璃 | 2.2kHz + 4.4kHz | 0.28s | 0.10 | 高 Q 带通，衰减快 |
| 陶 | 320Hz | 0.12s | 0.40 | 低通 900Hz，闷，无高频 |
| 种子/沙 | — | 0.09s | 1.00 | 高通 3kHz，颗粒感（随机包络调制） |
| 旧木 | 140Hz | 0.25s | 0.25 | 加慢速音高抖动（±6Hz，12Hz LFO）做「吱呀」 |

最小可用集仍是 15 §9 的 12 条：`ui_tap, item_pick, merge_low/mid/high, gen_tap_wood, order_complete, build_reveal, cat_orange, cat_calico, text_blip, bgm_day`。
