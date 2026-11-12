# 物志 ItemKeeper

已购物品智能管理 App。**不上架任何应用商店**，通过你自己的 GitHub 账号以 **GitHub Releases 提供 APK** 分发，你和伙伴直接下载安装。

- 纯本地运行：不联网、不申请任何运行时权限、不依赖后端；物品数据只存在 App 私有目录。
- 单次构建产物：`app/build/outputs/apk/release/app-release.apk`（约数 MB）。

---

## 1. 功能与需求对照

| 需求 | 实现位置 |
| --- | --- |
| 底部四栏：首页（默认）/ 添加 / 归类 / 界面 | `ui/AppScaffold.kt` |
| 首页两个占位框（框内有文字），点击弹簧动画自然展开 | `ui/screens/HomeScreen.kt`（`AnimatedVisibility` + `expandVertically(spring)` + `fadeIn`，箭头同步旋转） |
| 板块一：名称加粗、右侧橙色价格 + 日均价格、全部标签、状态七色 | `ui/components/ItemCard.kt`、`Chip.kt`、`theme/StatusColors.kt` |
| 板块二：总物价 / 物品数量 / 日均价总和，覆盖所有状态，不罗列明细 | `ui/screens/HomeScreen.kt` 的 `StatsBlock` |
| 添加：各属性填写/选择、红色「保存」、保存即写入并实时刷新首页 | `ui/screens/AddScreen.kt` + `viewmodel/MainViewModel.kt`（StateFlow 联动；保存后自动回首页并展开清单） |
| 归类：价格区间 / 购买日期区间 / 状态 / 类别 四维筛选 | `ui/screens/FilterScreen.kt` |
| 界面：仅「App 风格（浅/暗/跟随系统，默认跟随系统）」一项，无密钥 / 权限功能 | `ui/screens/SettingsScreen.kt` |
| 名称 ≤30 字、备注 ≤50 字、价格两位小数 | `AddScreen.kt`（带字数计数与输入正则） |
| 年/月/日三列滚动选择器，年份 2000–2050 | `ui/components/WheelDatePicker.kt`（纯 Compose 滚轮，日数随年月变化） |
| 平台自由输入 + 9 个快捷候选 | `AddScreen.kt` + `Item.PLATFORM_CANDIDATES` |
| 7 种状态、默认 4 类 + 自定义类别 | `Item.kt`、`data/SettingsRepository.kt` |

**日均价定义**：`日均价格 = 价格 / 持有天数`，其中 `持有天数 = 购买日至今天数 + 1`（购买当天按 1 天计，未来/异常日期按 1 天兜底，不会除零）。统计页「日均价总和」= 所有物品（含全部状态）日均价之和。

**权限说明**：App 不含任何密钥、登录或权限校验，所有物品信息均可见、可编辑、可删除；数据只存在本机，请自行保管设备。

---

## 2. 技术选型

| 项 | 选择 | 理由 |
| --- | --- | --- |
| 平台路线 | Android 原生 Kotlin + Jetpack Compose (Material 3) | 只发 APK、只跑 Android；原生主题跟随系统、滚轮、动画、TextField 均无需跨平台桥接，包体积与启动更小 |
| 架构 | 单 Activity + ViewModel + StateFlow + Repository | 保存后首页/归类/统计天然实时联动，无跨页手动刷新 |
| 持久化 | `org.json` + 本地 JSON 文件；设置用 SharedPreferences | 个人物品量小、筛选在内存完成；不引入 Room/注解处理器/序列化插件，构建最不易出错 |
| 构建 | JDK 17 / Gradle 8.2 / AGP 8.1.4 / Kotlin 1.9.22 / Compose Compiler 1.5.10 / BOM 2024.02.00 | Compose Compiler 与 Kotlin 版本严格对应，是最常见踩坑点，此处已配对 |
| SDK | minSdk 26 / targetSdk 34 / compileSdk 34 | 可直接使用 `java.time`（LocalDate、YearMonth），无需 desugaring |

> 若以后要 iOS 或双端，可再评估 Flutter / KMP；当前只发 APK，跨平台反而增加体积与桥接成本。

### 目录结构

```
ItemKeeper/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── keystore.properties.example          # 签名配置模板（占位，不含口令）
├── .github/workflows/release-apk.yml     # tag 触发：签名构建 + 发布 Releases
├── .gitignore                           # 已排除 *.jks / keystore.properties / *.apk
└── app/
    ├── build.gradle.kts                  # release signingConfig（读本地配置或环境变量）
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                          # 主题、字符串、自适应图标、备份规则
        └── java/com/tang/itemkeeper/
            ├── MainActivity.kt
            ├── model/Item.kt
            ├── data/{ItemRepository,SettingsRepository}.kt
            ├── viewmodel/MainViewModel.kt
            ├── util/Format.kt
            └── ui/
                ├── AppScaffold.kt        # 四栏导航 + 删除确认
                ├── theme/{Theme,StatusColors}.kt
                ├── components/{Chip,ItemCard,WheelDatePicker}.kt
                └── screens/{HomeScreen,AddScreen,FilterScreen,SettingsScreen}.kt
```

---

## 3. 本地运行前准备

1. **环境**：JDK 17、Android Studio Hedgehog (2023.1.1) 或更新版本、Android SDK Platform 34 + Build Tools 34。
2. **Gradle Wrapper**：本仓库只提交 `gradle/wrapper/gradle-wrapper.properties`，**不含二进制 `gradle-wrapper.jar`**（避免向仓库塞二进制）。任选一种补齐：
   - 本机装有 Gradle 8.2：在项目根目录执行 `gradle wrapper --gradle-version 8.2 --distribution-type bin`，然后 `chmod +x gradlew`；
   - 用 Android Studio 打开项目根目录 → `Sync Project with Gradle Files`，IDE 会按 `distributionUrl` 下载并补齐 wrapper；
   - 从任一现有 Android 工程复制 `gradle/wrapper/gradle-wrapper.jar` 到相同位置。
   
   > CI 不走 wrapper：`release-apk.yml` 用 `gradle/actions/setup-gradle` 安装 Gradle 8.2 后直接执行 `gradle assembleRelease`。
3. **调试构建**：`./gradlew assembleDebug`（若本机装了 Gradle 8.2，也可直接 `gradle assembleDebug`），或在 Android Studio 点 Run 装到手机/模拟器。
4. **签名构建**：先完成第 4 步，再执行 `./gradlew assembleRelease`。

---

## 4. 生成签名密钥（一次性，务必离线备份）

Android 安装 APK 必须用签名密钥；**后续所有版本必须用同一把密钥**，否则无法覆盖升级。

```bash
# 在项目根目录执行（会交互式询问姓名/组织等，可随意填；口令请记牢）
keytool -genkeypair -v -storetype PKCS12 \
  -keystore app/release.jks \
  -alias itemkeeper \
  -keyalg RSA -keysize 2048 -validity 10000
```

复制模板并填入真实值：

```bash
cp keystore.properties.example keystore.properties
# 编辑四项：KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD
```

`.gitignore` 已排除 `*.jks`、`keystore.properties`、`*.apk`，**永远不要把私钥或口令提交到 GitHub**。请把 `release.jks` 与口令离线备份（加密U盘/密码管理器）；丢失后只能换 `applicationId` 重新发布，用户需卸载重装且数据会清空。

---

## 5. 通过 GitHub Releases 发布 APK

### 5.0 准备仓库

```bash
cd ItemKeeper
git init
git add .
git commit -m "feat: 物志 v1.0.0"
git branch -M main
git remote add origin https://github.com/<你的用户名>/ItemKeeper.git
git push -u origin main
```

仓库可以是 **Public**（任何人拿到链接都能下载）或 **Private**（下载 Release 附件需要登录有权限的 GitHub 账号）。两者都支持 Releases，都不需要应用商店开发者账号。

### 5.1 方式 A：网页手动发布（最简单）

1. 本地 `./gradlew assembleRelease`，得到 `app/build/outputs/apk/release/app-release.apk`；
2. GitHub 仓库 → **Releases** → **Draft a new release**；
3. 新建 tag 如 `v1.0.0`，标题填「物志 v1.0.0」，把 APK 拖进 **Attach binaries**（若改名为 `ItemKeeper.apk`，链接更稳定）；
4. **Publish release**。资产下方即出现下载链接。

### 5.2 方式 B：`gh` CLI 一条命令

```bash
gh release create v1.0.0 \
  app/build/outputs/apk/release/app-release.apk#ItemKeeper.apk \
  --title "物志 v1.0.0" --generate-notes
```

### 5.3 方式 C：GitHub Actions 自动构建并发布（推荐）

`.github/workflows/release-apk.yml` 已配好：**推送 `v*` tag 即自动签名构建、上传唯一的 `ItemKeeper.apk` 到 Releases**。

在仓库 **Settings → Secrets and variables → Actions → New repository secret** 添加 4 个 Secret：

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | 私钥文件的 Base64 文本（不是文件本身） |
| `KEYSTORE_PASSWORD` | keystore 库口令 |
| `KEY_ALIAS` | 别名，如 `itemkeeper` |
| `KEY_PASSWORD` | key 口令 |

生成 Base64（Windows PowerShell / Git Bash / Linux 三选一）：

```powershell
# Windows PowerShell（结果已进剪贴板，直接粘贴到 Secret）
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\release.jks")) | Set-Clipboard
```

```bash
# Linux / Git Bash
base64 -w0 app/release.jks | clip        # Windows Git Bash
base64 -w0 app/release.jks               # 直接打印；macOS 用 base64 -i app/release.jks | pbcopy
```

触发发版：

```bash
# 改完代码后：app/build.gradle.kts 里 versionCode +1、versionName 同步
git add . && git commit -m "release: v1.0.1"
git tag v1.0.1
git push origin main --tags
```

CI 步骤：检出 → JDK17/Gradle8.2（无 wrapper，直接调用 `gradle`）→ 准备 SDK34 → 解码私钥到 runner 临时目录并写 `keystore.properties` → `assembleRelease` → 校验已签名 APK → 发布唯一的 `ItemKeeper.apk` 附件（每次覆盖同名附件，下载链接不变）。`GITHUB_TOKEN` 由 Actions 自动注入，无需额外配置。

> 私钥在整个流程中只以 Secret 存在，解码产物位于 `$RUNNER_TEMP`，runner 销毁即消失；仓库与 Release 里都不含私钥。

### 5.4 伙伴怎么装

1. 把**唯一**下载链接发给伙伴（附件固定叫 `ItemKeeper.apk`，每次发版覆盖同名文件，链接永久不变）：

   ```
   https://github.com/<你的用户名>/ItemKeeper/releases/latest/download/ItemKeeper.apk
   ```
2. 手机浏览器打开 → 下载 APK → 点击安装。
3. Android 8+ 首次会要求授权：**设置 → 应用 → 特殊访问 → 安装未知应用 → 允许浏览器/文件管理器**。若 Google Play Protect 提示「未知开发者」，选「仍要安装」（个人签名应用属正常现象）。
4. 升级：直接下载新版 APK 覆盖安装即可，数据保留；**前提是签名一致且 `versionCode` 递增**。

### 5.5 与商店分发相比

| | GitHub Releases | 应用商店 |
| --- | --- | --- |
| 审核 / 开发者账号费用 | 无 | 需要 |
| 用户侧 | 需手动允许「未知来源」、无自动更新 | 一键安装、自动更新 |
| 分发范围 | Public 仓库任何人可下载；Private 需登录 | 面向公众 |
| 版本控制 | 自己用 tag + Release 管理，可分阶段放链接 | 商店灰度 |

---

## 6. 注意事项与能力边界

- 本工程已在 Windows 上完成真实构建验证：JDK 17 + Gradle 8.2 + Android SDK 34 执行 `gradle assembleRelease`，产出 5.7 MB 的 `app-release.apk`，`apksigner verify` 通过（v2 签名）。若换机器报 `SDK location not found`，在项目根目录创建 `local.properties` 写 `sdk.dir=<你的 Android SDK 路径>`（该文件已被 gitignore）。
- 未做真机 / 模拟器运行测试，界面与交互请在安装后自行验证。
- 如升级 AGP/Kotlin，**Compose Compiler 版本必须同步匹配 Kotlin 版本**。
- 物品数据以明文 JSON 存在 App 私有目录，卸装即清除，重要数据请自行备份；App 不含任何鉴权逻辑。
- 本版没有「App 内检查更新」，需要时可自行加一个读取 GitHub Releases API 的入口（会引入联网权限，与当前纯本地设计冲突，故未内置）。
- 自定义类别保存在本机设置中；换设备不会同步。

---

## 7. 发版检查清单

- [ ] `versionCode` 已递增、`versionName` 与 tag 一致
- [ ] `./gradlew assembleRelease` 本地通过，APK 已签名
- [ ] `release.jks` 与口令已离线备份，且未提交进仓库
- [ ] 4 个 Secrets 有效（换过签名密钥要同步更新 `KEYSTORE_BASE64`）
- [ ] tag 已推送，Actions 绿了，Releases 里 `ItemKeeper.apk` 可下载
- [ ] 用手机走一遍安装 / 覆盖升级
