[English](./README.md) | [简体中文](./README.zh.md) | **繁體中文**

<img align="right" src="https://github.com/user-attachments/assets/1ce21dfc-fd0c-4e6c-b006-ee3844adb274" border="0" alt="" />

# FakePlayerPlus ![](https://img.shields.io/badge/Paper-1.21.11_--_26.2-2B7FFF?logo=telegram&logoColor=3884F7) ![](https://img.shields.io/badge/Folia-1.21.11_--_26.2-C33CCA?logo=leaflet&logoColor=C33CCA)

這個插件模擬了真實玩家，對伺服器而言，此插件生成的假人就是一個真正的「活人」。

> 本插件的靈感源自 [minecraft-fakeplayer](https://github.com/tanyaofei/minecraft-fakeplayer) 插件，隨著 Minecraft 版本的快速迭代，原版插件的架構在修復和維護上略顯吃力，故基於 Kotlin 對其進行了完全的底層重構。本插件在繼承原版插件核心特性的同時，實現了專案架構的高度解耦，使其成為更現代化、更健壯的衍生加強版。

## 遷移

> 為了方便從 minecraft-fakeplayer 無縫過渡，本插件提供了資料遷移指令將原版插件的 SQLite 資料匯入到此插件中，防止假人 nbt 資訊遺失。

- **注意**: 在進行遷移操作前，**請務必備份原有的資料檔案**，防止不可逆的資料遺失。
- **步驟**: 將 minecraft-fakeplayer 的資料庫檔案 `plugins/fakeplayer/data.db` 複製到 `plugins/FakeplayerPlus` 目錄中。執行 `/fp import data.db fake_player_profile` 進行匯入。


## 功能

- [x] **等同真實玩家**
- [x] **保持區塊載入**：召喚假人幫你保持區塊載入、怪物生成
- [x] **背包存放物品**：可以使用假人的背包來存放物品。
- [x] **行為動作控制**：支援控制假人執行攻擊、挖掘、跳躍、釣魚等動作，並支援週期性循環。
- [x] **開發者 API**🚀：已將基本功能抽象成 api 套件，可供插件開發者調用
- [x] **語言檔案**🚀：可自訂語言檔案，並支援熱重載（包含繁體中文 `zh_TW`、簡體中文 `zh_CN`、英文 `en`）
- [x] **動態調整假人數量**🚀：伺服器 TPS 過低時可踢出假人並降低召喚數量限制
- [x] **假人設定 GUI**🚀：可透過 Dialog 介面快速開關實體碰撞、無敵模式、自動補貨等功能。
- [x] **假人動作 GUI**🚀：可透過 Dialog 介面快速執行假人動作
- [x] **假人聊天功能**🚀：可透過 /fp chat 讓假人發送聊天訊息
- [x] **假人 PING 設定**🚀：可設定假人 ping 值，也可模擬 ping 抖動偽裝活人
- [x] **多人管理**🚀：玩家可以互相分享假人使用權

## 設定

請參考插件目錄下的 `config.yml` 設定檔

## 指令

> [!IMPORTANT]
> 若不限制玩家的各項指令，可以直接給予玩家 `fakeplayer.basic`，此權限包含了所有安全的指令權限

| 指令 | 作用 | 權限 | 備註 |
| :--- | :--- | :--- | :--- |
| **/fp help** | 顯示幫助指令列表 | fakeplayer.help | 支援 `/fp help [頁碼]` 分頁查看 |
| **/fp spawn** | 召喚假人 | fakeplayer.spawn | 可在設定檔中設定召喚數量限制 |
| | | fakeplayer.spawn.limit.\<node\> | 在設定檔中設定 node 來實現為玩家/權限組單獨設定召喚數量限制 |
| /fp spawn \<name\> | 召喚假人時指定名稱 | fakeplayer.spawn.name | 不建議給一般玩家權限，因為會佔用未註冊的真實玩家名額 |
| /fp select \<name\> | 選取假人 | fakeplayer.select | 任何操作假人的指令都可以透過在**指令最後添加 `假人名稱` 指定假人** |
| /fp remove | 移除假人 | fakeplayer.remove | **`--all` 移除全部假人** |
| /fp kill | 擊殺假人 | fakeplayer.kill | **`--all` 擊殺全部假人<br>** 在設定檔中可設定 `死亡時動作` 來模仿原版插件行為。注意：若伺服器開啟了死亡掉落，kill 指令會導致背包掉落 |
| /fp invsee | 查看假人背包 | fakeplayer.invsee | 玩家對假人按右鍵亦可開啟 |
| /fp enderchest | 查看假人終界箱 | fakeplayer.enderchest | 玩家對假人按 Shift+右鍵亦可開啟 |
| /fp tp | 傳送到假人身邊 | fakeplayer.tp | |
| /fp tphere | 讓假人傳送到身邊 | fakeplayer.tp | |
| /fp tpswap | 與假人交換位置 | fakeplayer.tp | |
| /fp tppos | 讓假人傳送到指定座標 | fakeplayer.tp | |
| /fp skin \<name\> | 為假人設定正版玩家外觀 | fakeplayer.skin | 此指令有 60 秒冷卻時間 |
| /fp cmd | 讓假人執行指令 | fakeplayer.cmd | 指令有空格或需要 `/` 前綴時需使用 `"` 包裹，例如 `/fp cmd "kill @p"` |
| /fp chat | 讓假人發送聊天訊息 | fakeplayer.chat | 訊息有空格時需使用 `"` 包裹 |
| **/fp settings** | 開啟假人設定 GUI | fakeplayer.settings | |
| **/fp action** | 開啟假人動作列表 GUI | fakeplayer.action | 需有對應的動作權限（如下）才會顯示動作按鈕 |
| **/fp action start \<action\>** | 開啟假人動作執行 GUI | fakeplayer.action.\<action\> | |
| **/fp action execute \<action\>** | 直接讓假人執行動作 | fakeplayer.action.\<action\> | |
| /fp owner list | 列出假人的擁有者列表 | fakeplayer.owner.list | |
| **/fp owner add** | 將玩家新增為假人的擁有者 | fakeplayer.owner.add | |
| /fp owner remove | 移除玩家的擁有者權限 | fakeplayer.owner.remove | |
| /fp reload | 重載設定 | fakeplayer.reload | |

## 假人獨立設定 / 動作

請參考 `/fp settings` 和 `/fp action` 指令

![假人UI界面](https://github.com/user-attachments/assets/edf2dce7-009a-4b7c-827f-2b10bc432137)

## PlaceholderAPI

| 變數名稱 | 變數類型 | 作用說明 | 範例輸出 |
| :--- | :---: | :--- | :--- |
| `%fakeplayer_total%` | 全域 | 取得目前全服線上的假人總數量 | `5` |
| `%fakeplayer_list%` | 全域 | 取得目前全服線上的假人名稱列表 *（分隔符號可在語言檔案中設定）* | `FakePlayer_1,FakePlayer_2` |
| `%fakeplayer_list_0_name%` | 全域 | 取得假人列表中 `index` 位置的假人資訊（`name` 可替換為下方類型為假人的變數名稱，例如 `uuid`、`spawner` 等） | `FakePlayer_1` |
| `%fakeplayer_isfake%` | 玩家 | 判斷目前玩家是否為假人 | `true` / `false` |
| `%fakeplayer_name%` | 假人 | 假人名稱 | `FakePlayer_1` |
| `%fakeplayer_uuid%` | 假人 | 假人 UUID | `d6850f71-24e2-3d31-9ad4-1f5806837a17` |
| `%fakeplayer_spawner%` | 假人 | 假人的召喚者名稱 | `Steve` |
| `%fakeplayer_spawntime%` | 假人 | 假人被召喚的時間 *（時間格式可在語言檔案中設定）* | `2026年7月1日 00時00分00秒` |
| `%fakeplayer_actions%` | 假人 | 假人目前正在執行的動作列表 *（翻譯文字/分隔符號可在語言檔案中設定）* | `攻擊\|挖掘` |
