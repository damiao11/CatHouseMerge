# 开发接手说明

> 应用仍为0.3.0 / 3（待验收、未发布）。本文件只维护接手事项，不重复项目目录、运行命令和UI参数。

## 阅读入口

1. [README](README.md)：项目目录、运行方法、当前版本与文档索引。
2. [DESIGN](DESIGN.md)：当前方案与规范分工。
3. [06 UI规范](docs/06-UI-UX设计规范.md)：唯一的颜色、布局、尺寸及交互依据。
4. [07 技术方案](docs/07-技术方案与数据架构.md)：代码落点、数据和验收清单。
5. [版本记录](docs/版本迭代记录.md)：历史变更与实际验证范围。

## 修改资源与数据时

- Gradle已经从 `android-assets/` 与 `docs/data/` 自动同步资源和JSON，**无需手工复制进app**。入口见 `app/build.gradle.kts` 的 prepareGameAssets / prepareGameResources。
- 图标使用现有透明PNG，不拿 `art/` 的白底源图直接替代。尺寸和显示规则查06；房间原图放大后用于手机展示，不等于新增高清原画。
- 对应关系：days.orderIds → orders；days.room → room_{room}_{before|after}.webp；items.asset → 同名drawable；dialogue.who → 同名猫头像。
- 订单表由 `tools/gen_orders.py` 生成；改数值应同步生成Markdown和JSON，不分别手填。其他生成脚本先读脚本，再检查输出差异。

## 当前实现与边界

主界面和修缮页是原生Compose，不是WebView。维持7×9棋盘稳定槽位、按钮交付、拖放入仓与引擎售价，不用网页简化规则覆盖正式逻辑。

当前音效仍为本地提示音，正式BGM接入等目标未全部完成；原有素材清单不证明播放功能已实现。封面字体再分发授权仍须素材提供者确认，授权文件不能删除。

2026-11-19清理删除了35张旧木托变体和无调用的RestorationDecor.kt，原透明生成器、纹理图片及工具脚本保留。旧Figma组件统一放在 [历史资料目录](docs/archive/README.md)，不是当前视觉依据。

## 验收与记录

按07验收清单检查；运行命令只维护在README。过去44项核心测试、29项设备测试对应清理前迁移状态，不代表之后每个APK都重跑过。当前清理范围和检查见 [清理记录](docs/maintenance/repository-cleanup-2026-11-19.md)。本轮不改引擎、存档、版本号，也不自动提交、推送或合并分支。
