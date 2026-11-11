# 美术资源（Art）

> 这里只放**已经定稿**的图。草稿、备选、AI 原始输出都放 `_wip/`，不要混进来。
>
> 出图用的提示词记录在 [PROMPTS-LOG.md](PROMPTS-LOG.md)（39 条物品 + 6 条猫 + 5 张插画 + 35 级生成器 + 8 组房间，含迭代规则）。

## 目录结构

```
art/
├─ items/
│  ├─ wood/        木工    ic_wood_{1..6}.png
│  ├─ metal/       五金    ic_metal_{1..5}.png
│  ├─ fabric/      布艺    ic_fabric_{1..6}.png
│  ├─ light/       灯具    ic_light_{1..5}.png
│  ├─ ceramic/     陶瓷    ic_ceramic_{1..6}.png
│  ├─ plant/       绿植    ic_plant_{1..6}.png
│  └─ relic/       旧物    ic_relic_{1..5}.png
├─ generators/    生成器   gen_{chain}_{level}.png
├─ cats/          猫头像   cat_{名}.png
├─ rooms/         房间     room_{room}_{before|after}.png
├─ scenes/        插画     scene_{id}.png
└─ ui/            UI 资源（Figma 导出）
```

## 命名规范

| 类型 | 格式 | 例子 |
|---|---|---|
| 物品 | `ic_{chain}_{level}.png` | `ic_wood_3.png` |
| 生成器 | `gen_{chain}_{level}.png` | `gen_wood_2.png` |
| 猫 | `cat_{名}.png` | `cat_daju.png` |
| 房间 | `room_{room}_{before\|after}.png` | `room_porch_before.png` |
| 插画 | `scene_{id}.png` | `scene_ending.png` |

全小写、下划线、不用中文文件名。

## 规格

| 类型 | 尺寸 | 格式 |
|---|---|---|
| 物品 | 1024 × 1024 原始 → 导出 288 × 288 | PNG |
| 生成器 | 1024 × 1024 → 384 × 384 | PNG |
| 猫 | 1024 × 1024 → 384 × 384 | PNG |
| 房间 | 1152 × 2048 → 1080 × 1920 | WebP |
| 插画 | 9:16 → 1080 × 1920 | WebP |

## 进度

| 链 | 等级 | 状态 |
|---|---|---|
| 木工 | 6 | ✅ 6/6 定稿 |
| 五金 | 5 | ✅ 5/5 定稿 |
| 布艺 | 6 | ✅ 6/6 定稿 |
| 灯具 | 5 | ✅ 5/5 定稿 |
| 陶瓷 | 6 | ✅ 6/6 定稿 |
| 绿植 | 6 | ✅ 6/6 定稿 |
| 旧物 | 5 | ✅ 5/5 定稿 |

## 其他资源

| 类别 | 数量 | 状态 |
|---|---|---|
| 猫头像 | 6 | ✅ 6/6 基础 |
| 猫表情差分 | 12 | ✅ 12/12（开心、别过头）|
| 生成器 | 7 × 5 级 | ✅ 35/35 |
| 房间前后 | 8 × 2 | ✅ 16/16 |
| 插画 | 5 | ✅ 5/5 |

## 风格基准

木工链是**风格锚点**。做其他链时，把 `items/wood/ic_wood_3.png` 上传给 Gemini 当参考，并说：

```
保持和参考图完全一样的画风、描边粗细、配色和阴影，阴影要和参考图一样淡。
```

## 已定稿清单

- `items/wood/` 全部 6 张（含 `_contact_sheet.png` 对照图）
- `items/metal/` 全部 5 张（含 `_contact_sheet.png` 对照图）
- `items/fabric/` 全部 6 张（含 `_contact_sheet.png` 对照图）
- `items/light/` 全部 5 张（含 `_contact_sheet.png` 对照图）
- `items/ceramic/` 全部 6 张（含 `_contact_sheet.png` 对照图）
- `items/plant/` 全部 6 张（含 `_contact_sheet.png` 对照图）
- `items/relic/` 全部 5 张（含 `_contact_sheet.png` 对照图）
- `cats/` 6 只 × 3 表情 = 18 张（含 `_contact_sheet_full.png` 三态总览）
- `scenes/` 5 张插画（启动/D6/D12/D18/D24，含 `_contact_sheet.png`）
- `generators/` 七台生成器各 5 级（木工/五金/布艺/灯具/陶瓷/绿植/旧物）（含对应 `_contact_sheet_*.png`）
