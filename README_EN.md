<div align="center">

# ItemKeeper

**Track everything you've bought · Fully offline · Liquid glass UI**

[简体中文](./README.md) | [English](./README_EN.md)

[![Download APK](https://img.shields.io/badge/Download_APK-Releases-0ABAB5?style=for-the-badge&logo=android)](https://github.com/seasoul694-del/ItemKeeper/releases/latest/download/ItemKeeper.apk)

</div>

---

**ItemKeeper** is an Android app for tracking the things you've bought: register your purchases and it automatically computes the **daily cost of ownership** for every item, presented through a full liquid-glass interface.

> 📱 Runs fully offline · No internet permission · No permissions requested · Your data never leaves your phone

## ✨ Features

- **Liquid glass UI**: bottom bar, stats card, sort sheet and filter card all refract what's behind them (native refraction on Android 13+, graceful translucent fallback below); upload your own global background and every glass surface refracts it in real time
- **Five-tab navigation**: Home list / Summary / Add / Filter / Settings — the glass bottom bar supports both taps and horizontal drags
- **Item profiles**: name, price, purchase date (three-wheel picker), platform (9 quick picks + free input), status, category, notes — plus a real photo for every item
- **In-place expansion**: tap an item card to expand it right where it sits; edit / delete / change photo are always one tap away
- **Summary stats**: rolling counters for total spend, item count and total daily cost, plus a per-category bar chart that grows from zero on entry (horizontally scrollable with many categories)
- **Four-dimension filtering**: price range / purchase date range / status / category — leave anything empty for no limit
- **Eight sort modes**: date, price and daily cost each in both directions; group by category or status — your choice is remembered
- **Item lifecycle**: switching an item to "Used up / Scrapped / Sold / Lost" opens a glass sheet for the **end date** (and **sale price** for sold items) — its daily cost freezes at that date forever; "Sold" uses net cost (price − sale price). Ended items always sort below in-use ones with greyed-out prices
- **Category management**: preset + custom categories, deletable at any time; historical items are never affected
- **Global background**: one image fills the whole screen; the status bar and gesture bar float transparently on top of it

## 📥 Installation

1. Download `ItemKeeper.apk` from the [latest release](https://github.com/seasoul694-del/ItemKeeper/releases/latest)
2. Allow "install unknown apps" (side-loaded personal build, not on any store)
3. Install and go; future updates can be installed over the top (same signing key)

**Requirements**: Android 8.0 (API 26)+; the full refraction effect needs Android 13+.

## 🔒 Privacy

- **No network**: no internet permission is requested, data never leaves the device
- **No permissions**: no storage, location or contacts — photo picking uses the system picker
- **Data location**: items and settings live in the app's private directory and are removed on uninstall
- **No accounts**: open and use

## 💡 Daily cost formula

`daily cost = cost ÷ counted days` (counted days = span + 1; the purchase day itself counts as day 1):

- **In use** (In use / Idle / Given away): cost = purchase price, counted up to today, decreasing every day
- **Ended** (Used up / Scrapped / Sold / Lost): counted only up to the **end date** — the daily cost **freezes forever** afterwards
- **Sold**: cost = purchase price − sale price (net cost), spread over the holding period

A ¥100 item used for 200 days costs ¥0.50/day; if sold on day 200 for ¥60, the net cost is ¥40 and the daily cost freezes at ¥0.20/day. The Summary card's "total daily cost" only accumulates in-use items.

## 🛠 Tech stack

- Kotlin 2.4 + Jetpack Compose 1.12 (Material 3)
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (backdrop 2.0.1) + [shapes](https://github.com/Kyant0/shapes) 1.2.1
- Local JSON persistence + SharedPreferences, zero third-party backend
- AGP 9.3.2 / Gradle 9.7.1 / R8 minification

## 📄 License

For personal use and learning. The glass effect is built on [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — thanks to the original author.
