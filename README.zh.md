[**English**](./README.md) | **简体中文**

<img align="right" src="https://github.com/user-attachments/assets/1ce21dfc-fd0c-4e6c-b006-ee3844adb274" border="0" alt="" />

# FakePlayerPlus ![](https://img.shields.io/badge/Paper-1.21.11_--_26.2-2B7FFF?logo=telegram&logoColor=3884F7) ![](https://img.shields.io/badge/Folia-1.21.11_--_26.2-C33CCA?logo=leaflet&logoColor=C33CCA)

这个插件模拟了真实玩家，对服务端而言，这个插件生成的假人就是一个真正的“活人”。

> 本插件的灵感源自 [minecraft-fakeplayer](https://github.com/tanyaofei/minecraft-fakeplayer) 插件，随着 Minecraft 版本的快速迭代，原版插件的架构在修复和维护上略显吃力，故基于 Kotlin 对其进行了完全的底层重构。本插件在继承原版插件核心特性的同时，实现了项目架构的高度解耦，使其成为更现代化、更健壮的衍生加强版。

## 迁移

> 为了方便从 minecraft-fakeplayer 无缝过渡。本插件提供了数据迁移指令将原版插件的 SQLite 数据导入到此插件中，防止假人nbt信息丢失。

- **注意**: 在进行迁移操作前，**请务必备份原有的数据文件**，防止不可逆的数据丢失
- **步骤**: 将minecraft-fakeplayer的数据库文件 `plugins/fakeplayer/data.db` 复制到 `plugins/FakeplayerPlus` 目录中。执行 `/fp import data.db fake_player_profile` 进行导入。


## 功能

- [x] **等同真实玩家**
- [x] **保持区块加载**：召唤假人帮你保持区块加载、怪物刷新
- [x] **背包存放物品**：可以使用假人的背包来存放物品。
- [x] **行为动作控制**：支持控制假人执行攻击、挖掘、跳跃、钓鱼等动作，并支持周期性循环。
- [x] **开发者API**🚀：已将基本功能抽象成api包，可供插件开发者调用
- [x] **语言文件**🚀：可自定义语言文件，并支持热重载
- [x] **动态调整假人数量**🚀：服务器TPS过低时可踢出假人并降低召唤数量限制
- [x] **假人设置GUI**🚀：可通过Dialog界面快速开关实体碰撞、无敌模式、自动补货等功能。
- [x] **假人动作GUI**🚀：可通过Dialog界面快速执行假人动作
- [x] **假人聊天功能**🚀：可通过/fp chat让假人发送聊天消息
- [x] **假人PING设置**🚀：可配置假人ping值，也可模拟ping抖动伪装活人
- [x] **多人管理**🚀：玩家可以互相分享假人使用权

## 配置

请参考插件目录下的 `config.yml` 配置文件

## 命令

> [!IMPORTANT]
> 如果不限制玩家的各种命令，可以直接给玩家 `fakeplayer.basic`，这个权限包含了所有安全的权限

| 命令 | 作用 | 权限 | 备注 |
| :--- | :--- | :--- | :--- |
| **/fp spawn** | 召唤假人 | fakeplayer.spawn | 可在配置文件中配置召唤数量限制 |
| | | fakeplayer.spawn.limit.\<node\> | 在配置文件中配置 node 来实现为玩家/权限组单独配置召唤数量限制 |
| /fp spawn \<name\> | 召唤假人时指定名称 | fakeplayer.spawn.name | 不建议给一般玩家权限 因为会占用未注册的真实玩家名额 |
| /fp select \<name\> | 选中假人 | fakeplayer.select | 任何操作假人的命令都可以通过在**指令最后添加 `假人名称` 指定假人** |
| /fp remove | 移除假人 | fakeplayer.remove | **`--all` 移除全部假人** |
| /fp kill | 杀死假人 | fakeplayer.kill | **`--all` 杀死全部假人<br>** 在配置文件中可设置 `死亡时退出` 来模仿原版插件行为，注意：如果服务器开启了死亡掉落 kill指令会导致背包掉落 |
| /fp invsee | 查看假人背包 | fakeplayer.invsee | 玩家对假人右键也可打开 |
| /fp enderchest | 查看假人末影箱 | fakeplayer.enderchest | 玩家对假人Shift+右键也可打开 |
| /fp tp | 传送到假人身边 | fakeplayer.tp | |
| /fp tphere | 让假人传送到身边 | fakeplayer.tp | |
| /fp tpswap | 与假人交换位置 | fakeplayer.tp | |
| /fp tppos | 让假人传送到指定位置 | fakeplayer.tp | |
| /fp skin \<name\> | 给假人设置正版玩家皮肤 | fakeplayer.skin | 此指令有 60 秒冷却 |
| /fp cmd | 让假人执行命令 | fakeplayer.cmd | 命令有空格时或需要 `/` 前缀时需将命令文本使用 `"` 包裹，例如 `/fp cmd "kill @p"` |
| /fp chat | 让假人发送聊天消息 | fakeplayer.chat | 消息有空格时需将消息文本使用 `"` 包裹 |
| **/fp settings** | 打开假人设置 GUI | fakeplayer.settings | |
| **/fp action** | 打开假人动作列表 GUI | fakeplayer.action | 有对应的动作权限（如下）才能显示动作按钮 |
| **/fp action start \<action\>** | 打开假人动作执行 GUI | fakeplayer.action.\<action\> | |
| **/fp action execute \<action\>** | 直接让假人执行动作 | fakeplayer.action.\<action\> | |
| /fp owner list | 列出假人的所有者列表 | fakeplayer.owner.list | |
| **/fp owner add** | 将一个玩家添加为假人的所有者 | fakeplayer.owner.add | |
| /fp owner remove | 取消玩家的所有者权限 | fakeplayer.owner.remove | |
| /fp reload | 重载配置 | fakeplayer.reload | |

## 假人独立设置/动作

请参考 `/fp settings` 和 `/fp action` 指令

![假人UI界面](https://github.com/user-attachments/assets/edf2dce7-009a-4b7c-827f-2b10bc432137)

## PlaceholderAPI

| 变量名 | 变量类型 | 作用说明 | 示例输出 |
| :--- | :---: | :--- | :--- |
| `%fakeplayer_total%` | 全局 | 获取当前全服在线的假人总数量 | `5` |
| `%fakeplayer_list%` | 全局 | 获取当前全服在线的假人名称列表 *（分隔符可以在语言文件中配置）* | `FakePlayer_1,FakePlayer_2` |
| `%fakeplayer_list_0_name%` | 全局 | 获取假人列表中`index`位置的假人的信息（`name`可以替换为下面类型为假人的变量名，例如`uuid`，`spawner`等） | `FakePlayer_1` |
| `%fakeplayer_isfake%` | 玩家 | 判断当前玩家是否为假人 | `true` / `false` |
| `%fakeplayer_name%` | 假人 | 假人名称 | `FakePlayer_1` |
| `%fakeplayer_uuid%` | 假人 | 假人UUID | `d6850f71-24e2-3d31-9ad4-1f5806837a17` |
| `%fakeplayer_spawner%` | 假人 | 假人的召唤者名称 | `Steve` |
| `%fakeplayer_spawntime%` | 假人 | 假人被召唤的时间 *（时间格式可以在语言文件中配置）* | `2026年7月1日 00时00分00秒` |
| `%fakeplayer_actions%` | 假人 | 假人当前正在执行的动作列表 *(翻译文本/分隔符可以在语言文件中配置)* | `攻击\|挖掘` |