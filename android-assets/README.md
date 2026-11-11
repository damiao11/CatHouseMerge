# Android 资源包（可直接用）

> 从 `art/` 转出来的**成品资源**：已去掉白底、已按目标尺寸缩放、已压缩。
> 开发直接用这个目录，**不要再从 `art/` 里拿原始图**（那是 1024 的源文件，带白底、体积大）。

## 结构

```
android-assets/
├─ drawable/     92 个 PNG（已透明）  → app/src/main/res/drawable-nodpi/
├─ rooms/        16 个 WebP 1080×1920 → app/src/main/assets/rooms/
├─ scenes/       5 个 WebP 1080×1920  → app/src/main/assets/scenes/
└─ assets.csv    清单（type, file, size, folder）
```

## 规格

| 类型 | 文件 | 尺寸 | 格式 | 说明 |
|---|---|---|---|---|
| 物品 | `ic_{链}_{级}.png` | 288×288 | PNG-24 带透明 | 显示 40dp（棋盘 44dp 格内） |
| 生成器 | `gen_{链}_{级}.png` | 384×384 | PNG-24 带透明 | 显示 44dp（占一格） |
| 猫 | `cat_{名}[_happy|_turned].png` | 384×384 | PNG-24 带透明 | 显示 44dp（订单卡圆形裁切） |
| 房间 | `room_{房}_{before|after}.webp` | 1080×1920 | WebP q86 | 建造界面全屏 `Crop` |
| 插画 | `scene_{id}.webp` | 1080×1920 | WebP q86 | 启动 / 章末 / 结局全屏 |

总大小 **13.9 MB**。

## 导入

1. `drawable/` 整个复制到 `app/src/main/res/drawable-nodpi/`
   （用 `nodpi` 是为了让 Compose 按原像素显示，缩放交给 `Modifier.size()`）
2. `rooms/`、`scenes/` 复制到 `app/src/main/assets/`
3. 用 Coil 加载 assets：
   ```kotlin
   AsyncImage(model = "file:///android_asset/rooms/room_porch_after.webp", ...)
   ```
   或先复制到缓存目录再加载（WebP 走 BitmapFactory 也行）

## 注意

- **已去白底**：物品、生成器、猫的背景是透明的，不要再做二次抠图
- **房间和插画没透明**：它们本来就该有背景，全屏铺满
- **别再压缩**：已经是 q86 WebP，二次压缩会出块状噪点
- 文件名和 `docs/data/items.json` 里的 `asset` 字段一一对应
