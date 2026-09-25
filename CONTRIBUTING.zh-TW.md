[English](./CONTRIBUTING.md) | [简体中文](./CONTRIBUTING.zh.md) | **繁體中文**

# FakePlayerPlus 貢獻與開發指南

感謝你對 **FakePlayerPlus** 的關注與貢獻！
無論是提交 Bug、改進功能、完善 API、新增自訂元件，還是提交文件，都非常歡迎你的參與。

## 貢獻程式碼

1. Fork 本專案。
2. Clone 你的 fork 儲存庫：
   ```bash
   git clone https://github.com/<使用者名稱>/FakePlayerPlus.git
   ```
3. 建立一個新的分支：
    ```bash
    git checkout -b feat/<具體功能分支名>
    ```
4. 完成修改並進行測試。
5. 提交 Commit。
6. Push 到你的 Fork。
7. 建立 Pull Request 前，請確認：
   * 沒有引入不必要的相依套件。
   * 已完成必要的測試。
   * 如果修改了 API，請在 PR 中說明影響範圍。
   * 如果修改了使用者可見的功能，請同步更新相關文件。
   * 標題：建議使用與 Commit Message 一致的格式，例如 `feat: add custom component registration API`
8. 提交 Pull Request。

## 開發附屬插件（使用 API）

FakePlayerPlus 提供獨立的 API 開發套件，供其他插件進行整合。

- 匯入相依套件（Gradle）：
   ```kotlin
   repositories {
       maven {
           url = uri("https://maven.pkg.github.com/xiplugin/FakePlayerPlus")
           credentials {
               username = "<GITHUB使用者名稱>"
               password = "<GITHUB密碼或TOKEN權杖>"
           }
       }
   }
   
   dependencies {
       compileOnly("com.coderxi.plugin.fakeplayer:api:<版本號>")
   }
   ```
- 在 `plugin.yml` 或 `paper-plugin.yml` 中新增對本插件的相依：
   ```yaml
   depend:
     - FakePlayerPlus
   ```
   ```yaml
   dependencies:
     server:
       FakePlayerPlus:
         load: BEFORE
         required: true
   ```

- 建立自訂元件：
  ```kotlin
  class MyFppComponent : FakePlayerPlusPluginComponent {

      val fakeplayers get() = fpm.fakeplayers()

      init {
          println(fakeplayers)
      }

      @EventHandler
      fun handle(e: FakePlayerPreparingEvent) {
          //具體程式碼
      }

  }
  ```
- 在你的插件啟動類別註冊自訂元件：
  ```java
  FakePlayerPlusPluginApi.registerComponent(new MyFppComponent());
  ```

## 自訂語言檔案

FakePlayerPlus 支援自訂語言檔案。

- 自訂語言檔案放置於插件資料目錄的語言資料夾中，並依照對應的 Locale 命名，當檔案存在時會覆蓋插件預設提供的語言內容。
   ```text
   plugins/
   └── FakePlayerPlus/
       └── messages/
           ├── messages.properties
           ├── messages_zh-TW.properties
           ├── messages_zh-CN.properties
   ```
