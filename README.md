**English** | [简体中文](./README.zh.md)

<img align="right" src="https://github.com/user-attachments/assets/1ce21dfc-fd0c-4e6c-b006-ee3844adb274" border="0" alt="" />

# FakePlayerPlus ![](https://img.shields.io/badge/Paper-1.21.11_--_26.2-2B7FFF?logo=telegram&logoColor=3884F7) ![](https://img.shields.io/badge/Folia-1.21.11_--_26.1.2-C33CCA?logo=leaflet&logoColor=C33CCA)

This plugin generates fake players that act as real ones. To the server, they are fully recognized as real, living players.

> This plugin is inspired by [minecraft-fakeplayer](https://github.com/tanyaofei/minecraft-fakeplayer) plugin. With the rapid iteration of Minecraft versions, the original plugin's architecture is slightly struggling in patching and maintenance, so it was completely refactored from the ground up based on Kotlin. While inheriting the core features of the original plugin, this plugin achieves a high degree of decoupling in project architecture, making it a more modern, robust, and enhanced derivative version.

## Migration

> To facilitate a seamless transition from `minecraft-fakeplayer`, this plugin provides a data migration command to import SQLite data from the original plugin, preventing the loss of fake player NBT data.

- **Note**: Before performing the migration, **please make sure to back up your original data files** to prevent irreversible data loss.
- **Steps**: Copy the `minecraft-fakeplayer` database file from `plugins/fakeplayer/data.db` into the `plugins/FakeplayerPlus` directory. Then, execute `/fp import data.db fake_player_profile` to import the data.

## Features

- [x] **Identical to Real Players**
- [x] **Keep Chunks Loaded**: Summon fake players to help you keep chunks loaded and mobs refreshing
- [x] **Inventory Storage**: You can use the fake player's inventory to store items.
- [x] **Behavior & Action Control**: Supports controlling fake players to perform actions such as attacking, mining, jumping, fishing, etc., and supports periodic loops.
- [x] **Developer API**🚀: Core features have been abstracted into an api package, available for plugin developers to invoke
- [x] **Language Files** 🚀: Custom language files supported with hot-reload
- [x] **Dynamic Count Adjustment** 🚀: When the server TPS is too low, it can kick fake players and lower the summon limit
- [x] **Settings GUI** 🚀: Quickly toggle entity collision, invincibility mode, auto-restock, and other features via Dialog interface.
- [x] **Action GUI** 🚀: Easily execute fake player actions via Dialog interface
- [x] **Chat Feature** 🚀: Make fake players send chat messages using /fp chat
- [x] **Latency (PING) Settings** 🚀: Configure fake player ping values, or simulate ping jitter to camouflage real players
- [x] **Multi-user Management** 🚀: Players can share fake player control rights with each other

## Configuration

Please refer to the `config.yml` configuration file in the plugin directory

## Commands

> [!IMPORTANT]
> If you don't restrict various commands for players, you can directly give players `fakeplayer.basic`, this permission includes all safe permissions

| Command | Description | Permission | Notes |
| :--- | :--- | :--- | :--- |
| **/fp spawn** | Summon a fake player | fakeplayer.spawn | The summon limit can be configured in the configuration file |
| | | fakeplayer.spawn.limit.\<node\> | Configure node in the configuration file to set custom summon limits for specific players/groups |
| /fp spawn \<name\> | Summon with a specific name | fakeplayer.spawn.name | Not recommended for regular players because it occupies unregistered real player names |
| /fp select \<name\> | Select a fake player | fakeplayer.select | Any action command can target a specific fake player by adding `<name>` at the end of the command |
| /fp remove | Remove a fake player | fakeplayer.remove | **`--all` to remove all fake players** |
| /fp kill | Kill the fake player | fakeplayer.kill | **`--all` to kill all fake players<br>** The `death-action` option can be configured in the config.yml to mimic original plugin behavior. Note: If keep-inventory is disabled on the server, the kill command will cause items to drop from the inventory. |
| /fp invsee | View fake player inventory | fakeplayer.invsee | Players can also right-click the fake player to open it |
| /fp enderchest | View fake player enderchest | fakeplayer.enderchest | Players can also shift-right-click the fake player to open it |
| /fp tp | Teleport to the fake player | fakeplayer.tp | |
| /fp tphere | Teleport the fake player to you | fakeplayer.tp | |
| /fp tpswap | Swap positions with the fake player | fakeplayer.tp | |
| /fp tppos | Teleport fake player to specific position | fakeplayer.tp | |
| /fp skin \<name\> | Set a premium account skin for the fake player | fakeplayer.skin | This command has a 60-second cooldown |
| /fp cmd | Force fake player to execute a command | fakeplayer.cmd | If the command contains spaces or requires a `/` prefix, wrap it in `"`, e.g., `/fp cmd "kill @p"` |
| /fp chat | Force fake player to send a chat message | fakeplayer.chat | Wrap the text in `"` if the message contains spaces |
| **/fp settings** | Open the Settings GUI | fakeplayer.settings | |
| **/fp action** | Open the Action List GUI | fakeplayer.action | The action buttons will only display if the player has the corresponding permission (see below) |
| **/fp action start \<action\>** | Open the Action Execution GUI | fakeplayer.action.\<action\> | |
| **/fp action execute \<action\>** | Execute Action directly | fakeplayer.action.\<action\> | |
| /fp owner list | List the owners of the fake player | fakeplayer.owner.list | |
| **/fp owner add** | Add a player as an owner of the fake player | fakeplayer.owner.add | |
| /fp owner remove | Revoke a player's owner permissions | fakeplayer.owner.remove | |
| /fp reload | Reload configuration | fakeplayer.reload | |

## Fake Player Independent Settings / Actions

Please refer to the `/fp settings` and `/fp action` commands.

![FakePlayer UI](https://github.com/user-attachments/assets/a60189fa-3416-4164-9854-16dedb05b721)

## PlaceholderAPI

| Placeholder | Type | Description | Example Output |
| :--- | :---: | :--- | :--- |
| `%fakeplayer_total%` | Global | Gets the total number of fake players currently online across the server | `5` |
| `%fakeplayer_list%` | Global | Gets the list of names of all fake players currently online *(The separator can be configured in the language file)* | `FakePlayer_1,FakePlayer_2` |
| `%fakeplayer_list_0_name%` | Global | Gets the information of the fake player at the specified `index` in the list (`name` can be replaced with any FakePlayer-type attribute below, e.g., `uuid`, `spawner`, etc.) | `FakePlayer_1` |
| `%fakeplayer_isfake%` | Player | Checks if the current player is a fake player | `true` / `false` |
| `%fakeplayer_name%` | FakePlayer | The name of the fake player | `FakePlayer_1` |
| `%fakeplayer_uuid%` | FakePlayer | The UUID of the fake player | `d6850f71-24e2-3d31-9ad4-1f5806837a17` |
| `%fakeplayer_spawner%` | FakePlayer | Gets the name of the creator who summoned this fake player | `Steve` |
| `%fakeplayer_spawntime%` | FakePlayer | The time when the fake player was summoned *(The time format can be configured in the language file)* | `2026-07-01 00:00:00` |
| `%fakeplayer_actions%` | FakePlayer | Gets the list of actions currently being executed by the fake player *(The localized text and separator can be configured in the language file)* | `Attack\|Mine` |