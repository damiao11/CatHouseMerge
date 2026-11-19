# 仓库清理记录 · 2026-11-19

应用仍为 **0.3.0 / 3**，设计文档仍为v0.3。本轮只整理仓库、删除已确认废弃项，不修改游戏规则、存档或当前界面表现。尚未提交、推送或合并main。

## 已确认的删除（上轮完成）

- 35张 `android-assets/drawable/gen_*_tray.png`，合计约4.13 MiB；35张原透明生成器全部保留。
- 无调用的 `app/src/main/java/com/example/cathousemerge/ui/RestorationDecor.kt`，包含JournalPaper、JournalCoins、JournalEnergy。
- 三张旧纹理/装饰PNG、所有工具脚本按用户要求保留。本轮没有继续删除图片。

## 本轮整理

1. README维护项目、运行、目录入口；HANDOFF仅保留资源自动同步、接手限制和验收边界，移除重复目录与手工复制步骤。
2. 06是视觉/交互参数唯一入口；DESIGN仅保留方向与不可退化约束，07保留技术落点，不再复制色板数值。
3. 原13的完整使用说明并入12，原16的完整制作说明并入15；13和16保留短跳转页。机器可读JSON、prompts.txt与生成工具不改。
4. 旧Figma SVG、预览图、导入说明归档到 `docs/archive/figma-v0.2/`；原位置保留README入口，更新当前文档链接。SVG和PNG字节不变。
5. 历史测试/截图仅在 `docs/archive/README.md` 建索引，不移动或压缩原证据，避免破坏报告路径与哈希。
6. .gitignore补充精确的本地调试日志、旧源码副本和原始v1.zip规则，不忽略正式报告或全局*.log；本地文件不删除。
7. 修正07里误写的“不点交付按钮”为“点击卡片空白不触发交付”。这是文案纠错，不改代码行为。

## 保留边界

- art源图、UI-v1原型与预览、正式透明图标/房间图、音频、授权文件、问题Word保留。
- core、当前app源码/测试、docs/data、全部工具脚本、已有artifacts报告与截图不改。
- gradle-daemon-jvm.properties为本机原有未跟踪文件，保持原样，不擅自纳入Git或屏蔽。
- 不重写Git历史；删除当前分支文件不会等量缩小完整克隆历史体积。
- 旧报告只对原APK/源码负责，不能把清理后的APK套用旧哈希与设备测试结论。

## 本轮验证

- 受保护的331个已有文件逐个SHA-256核对通过：源图、原型、当前源码/测试、core源码、JSON、工具、本地文件及既有报告/截图均不变。上轮已删除的36个文件不计入本轮基线。
- 归档SVG和PNG与Git原文件字节一致；原13和16的正文信息已迁入新位置，旧页面只保留跳转。合并时将旧“再加4px描边”改为“保留图内已有描边”，以服从06，不改变当前素材。
- 检查变更文档及新索引的100个本地链接，全部有效；两个制作附录锚点存在。
- `git -c core.whitespace=cr-at-eol diff --check`通过，保留原文档换行风格。
- `:app:assembleDebug :app:assembleDebugAndroidTest --offline --no-daemon`检查通过，75项任务全部UP-TO-DATE；这是上轮删除后已构建产物的有效性检查，不冒充本轮强制重编译。
- `:core:test --rerun-tasks --offline --no-daemon`通过：**44项测试，0失败、0错误**，4项任务实际执行。
- 本轮未运行模拟器/真机测试，未进行新的界面截图验收。版本与当前UI表现不变。
- 本机日志在被忽略的 `build/cleanup-docs-20261119/build.log` 和 `core-tests.log`，不为一次文档整理新增重复的Git日志附件。
