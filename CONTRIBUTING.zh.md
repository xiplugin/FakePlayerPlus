[English](./CONTRIBUTING.md) | **简体中文** | [繁體中文](./CONTRIBUTING.zh-TW.md)

# FakePlayerPlus贡献与开发指南

感谢你对 **FakePlayerPlus** 的关注与贡献！
无论是提交 Bug、改进功能、完善 API、添加自定义组件，还是提交文档，都非常欢迎你的参与。

## 贡献代码

1. Fork 本项目。
2. clone你的fork仓库
   ```bash
   git clone https://github.com/<用户名>/FakePlayerPlus.git
   ```
3. 创建一个新的分支：
    ```bash
    git checkout -b feat/<具体功能分支名>
    ```
4. 完成修改并进行测试。
5. 提交 Commit。
6. Push 到你的 Fork。
7. 创建 Pull Request 前，请确认：
   * 没有引入不必要的依赖。
   * 已完成必要的测试。
   * 如果修改了API，请在PR中说明影响范围。
   * 如果修改了用户可见功能，请同步更新相关文档
   * 标题：推荐使用与 Commit Message 一致的格式，例如 `feat: add custom component registration API`
8. 提交 Pull Request。

## 开发附属插件(使用API)
FakePlayerPlus 提供独立的 API 开发包，供其他插件进行集成。

-  导入依赖(Gradle)
   ```kotlin
   repositories {
       maven {
           url = uri("https://maven.pkg.github.com/xiplugin/FakePlayerPlus")
           credentials {
               username = "<GITHUB用户名>"
               password = "<GITHUB密码或TOKEN令牌>"
           }
       }
   }
   
   dependencies {
       compileOnly("com.coderxi.plugin.fakeplayer:api:<版本号>")
   }
   ```
- 在`plugin.yml`或`paper-plugin.yml`中添加对本插件的依赖
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

- 创建自定义组件
  ```kotlin
  class MyFppComponent : FakePlayerPlusPluginComponent {

    val fakeplayers get() = fpm.fakeplayers()

    init {
        println(fakeplayers)
    }

    @EventHandler
    fun handle(e: FakePlayerPreparingEvent) {
        //具体代码
    }

  }
  ```
- 在你的插件启动类注册自定义组件
  ```java
  FakePlayerPlusPluginApi.registerComponent(MyFppComponent())
  ```

## 自定义语言文件

FakePlayerPlus 支持自定义语言文件。
- 自定义语言文件置在插件数据目录的语言文件夹中，并按照对应的 Locale 命名，当文件存在时会覆盖插件默认提供的语言内容。
   ```text
   plugins/
   └── FakePlayerPlus/
       └── messages/
           ├── messages.properties
           ├── messages_zh-TW.properties
           ├── messages_zh-CN.properties
   ```