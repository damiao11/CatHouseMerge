# 交付说明（给开发同学）

> 仓库已接入 Android 应用与纯 Kotlin 游戏引擎。运行方法见 README，设计依据见 `docs/07`。

## 一、仓库里有什么

| 目录 | 内容 | 这份要不要读 |
|---|---|---|
| `docs/` | 14 份设计文档（概念、系统、关卡、数值、剧情、UI、技术、风险…） | 先读 07，其余当字典查 |
| `docs/data/` | 7 份内容 JSON（订单、台词、链、物品、天数、产出表、音频清单） | 直接读进内存 |
| `android-assets/` | **成品资源 113 张**（已去白底、已压尺寸） | 直接复制进工程 |
| `ui/figma/` | 组件 SVG、导入说明 | 对尺寸和配色 |
| `art/` | 原始 1024 素材与对照图（112 MB） | 不用，重导时才用 |
| `tools/` | 订单生成脚本 | 改了数值再跑 |

## 二、开工顺序

现有工程已通过 Gradle 任务接入下述资源，无需手工复制。修改 `docs/data/*.json` 或 `android-assets/` 后重新构建即可生效。

1. 读 `docs/07` 的 7.1（技术栈与依赖）→ 建 Gradle 工程
2. 把 `android-assets/drawable/` 复制到 `app/src/main/res/drawable-nodpi/`
3. 把 `android-assets/rooms/`、`scenes/` 复制到 `app/src/main/assets/`
4. 把 `docs/data/*.json` 复制到 `app/src/main/assets/content/`
5. 按 7.12 的四个里程碑推进

## 三、三个必须知道的点

**1. 资源已经处理好了**
`android-assets/` 里的图标是**透明 PNG**，别再从 `art/` 拿原图（那是白底 1024 的源文件）。

**2. 尺寸规格**

| 类型 | 文件尺寸 | 显示尺寸 |
|---|---|---|
| 物品 | 288×288 | 40dp（棋盘 44dp 格内） |
| 生成器 | 384×384 | 44dp |
| 猫 | 384×384 | 44dp 圆形裁切 |
| 房间 / 插画 | 1080×1920 | 全屏 Crop |

**3. 房间图是放大过的**
原图 572×1024，升到 1080×1920。全屏显示够用、不锐利，演示没问题，别拿去做高清截图。

## 四、数据之间的对应

```
days.json ──orderIds──→ orders.json
    │
    └─room──→ android-assets/rooms/room_{room}_{before|after}.webp

dialogue.json 的 who：daju / huahua / tuanzi / meiqiu / ali / huzi
    └──→ android-assets/drawable/cat_{who}.png（表情加 _happy / _turned）

items.json 的 asset：ic_wood_3
    └──→ android-assets/drawable/ic_wood_3.png
```

## 五、验收

`docs/07` 的 7.13 有 45 条验收清单，逐条打勾即可。
