<div align="center">

# ItemKeeper · 物志

**已购物品智能管理 · 纯本地 · 液态玻璃 UI**

[简体中文](./README.md) | [English](./README_EN.md)

[![Download APK](https://img.shields.io/badge/下载_APK-Releases-0ABAB5?style=for-the-badge&logo=android)](https://github.com/seasoul694-del/ItemKeeper/releases/latest/download/ItemKeeper.apk)

</div>

---

**ItemKeeper（物志）** 是一款记录「已购买物品」的 Android 应用：把买过的东西登记下来，自动算出每件物品的**日均使用成本**，并用一整面液态玻璃把它呈现出来。

> 📱 纯本地运行 · 无网络权限 · 无任何权限申请 · 数据只存在你的手机里

## ✨ 功能特性

- **液态玻璃界面**：底栏、统计卡、排序卡、筛选卡全部采用真实背景折射玻璃（Android 13+ 原生折射，低版本自动降级半透明），支持上传自定义全局背景，玻璃元素会实时折射背景
- **五页导航**：首页清单 / 归总统计 / 添加 / 归类筛选 / 界面设置，玻璃底栏支持点按与横向拖动切换
- **物品档案**：名称、价格、购买日期（三列滚轮选择）、购买平台（9 个快捷项 + 自由输入）、状态、类别、备注，支持为每件物品上传实拍图
- **就地展开**：首页点击物品卡片原位展开，查看完整信息，编辑 / 删除 / 换图一步到位
- **归总统计**：总物价、物品数量、日均价总和的滚动计数，各类别物价分布柱状图（进入时从 0 生长，类别多时可左右滑动循环浏览）
- **四维归类**：价格区间 / 购买日期区间 / 状态 / 类别组合筛选，留空即不限制
- **八种排序**：购买日期、价格、日均价各支持双向排序，类别与状态支持归组，选择会被记住
- **类别管理**：预设 + 自定义类别，支持随时删除，历史物品不受影响
- **全局背景**：上传一张图片作为全屏背景，状态栏与手势条透明浮于图片之上

## 📥 下载安装

1. 打开 [Releases 页面](https://github.com/seasoul694-del/ItemKeeper/releases/latest) 下载 `ItemKeeper.apk`
   （或直接使用固定链接：`https://github.com/seasoul694-del/ItemKeeper/releases/latest/download/ItemKeeper.apk`）
2. 允许「安装未知来源应用」（个人分发应用，未上架商店）
3. 安装即用；覆盖安装升级无需卸载旧版（签名一致）

**系统要求**：Android 8.0（API 26）及以上；液态玻璃折射效果在 Android 13 及以上完整呈现。

## 🔒 隐私说明

- **不联网**：未申请任何网络权限，数据从不离开设备
- **不申请权限**：无存储、无定位、无通讯录——图片选择走系统相册选择器
- **数据位置**：物品清单与设置保存在应用私有目录，卸载应用即清除
- **无账号无鉴权**：打开即用

## 💡 日均成本算法

`日均价格 = 价格 ÷ 持有天数`，其中 `持有天数 = 购买日至今 + 1`（购买当天按 1 天计，异常日期兜底为 1 天）。一件 100 元、用了 200 天的物品，日均成本就是 ¥0.50/天。

## 🛠 技术栈

- Kotlin 2.4 + Jetpack Compose 1.12（Material 3）
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（backdrop 2.0.1）+ [shapes](https://github.com/Kyant0/shapes) 1.2.1
- 本地 JSON 持久化 + SharedPreferences，零第三方后端
- AGP 9.3.2 / Gradle 9.7.1 / R8 混淆

## 📄 许可

供个人使用与学习交流。玻璃效果实现参考 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 项目，感谢原作者。
