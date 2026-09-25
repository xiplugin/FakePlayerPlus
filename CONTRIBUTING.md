**English** | [简体中文](./CONTRIBUTING.zh.md) | [繁體中文](./CONTRIBUTING.zh-TW.md)

# FakePlayerPlus Contribution and Development Guide

Thank you for your interest in contributing to **FakePlayerPlus**!
Whether you are reporting bugs, improving features, refining the API, adding custom components, or submitting documentation, your participation is very welcome.

## Contributing Code

1. Fork this project.
2. Clone your fork:
   ```bash
   git clone https://github.com/<username>/FakePlayerPlus.git
   ```
3. Create a new branch:
   ```bash
   git checkout -b feat/<descriptive-branch-name>
   ```
4. Make your changes and test them.
5. Create a commit.
6. Push to your fork.
7. Before creating a Pull Request, please confirm that:
   * No unnecessary dependencies have been introduced.
   * Necessary testing has been completed.
   * If the API was modified, describe the impact in the PR.
   * If user-visible functionality was changed, update the relevant documentation accordingly.
   * Title: it is recommended to follow the same format as the Commit Message, e.g. `feat: add custom component registration API`.
8. Submit the Pull Request.

## Developing Add-on Plugins (Using the API)

FakePlayerPlus provides a standalone API artifact for other plugins to integrate with.

- Add the dependency (Gradle):
   ```kotlin
   repositories {
       maven {
           url = uri("https://maven.pkg.github.com/xiplugin/FakePlayerPlus")
           credentials {
               username = "<GITHUB_USERNAME>"
               password = "<GITHUB_PASSWORD_OR_TOKEN>"
           }
       }
   }

   dependencies {
       compileOnly("com.coderxi.plugin.fakeplayer:api:<VERSION>")
   }
   ```
- Add a dependency on this plugin in `plugin.yml` or `paper-plugin.yml`:
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

- Create a custom component:
  ```kotlin
  class MyFppComponent : FakePlayerPlusPluginComponent {

      val fakeplayers get() = fpm.fakeplayers()

      init {
          println(fakeplayers)
      }

      @EventHandler
      fun handle(e: FakePlayerPreparingEvent) {
          // your code here
      }

  }
  ```
- Register the custom component in your plugin's startup class:
  ```java
  FakePlayerPlusPluginApi.registerComponent(new MyFppComponent());
  ```

## Custom Language Files

FakePlayerPlus supports custom language files.

- Place custom language files in the language folder of the plugin data directory, named according to the corresponding Locale. When a file exists, it will override the default language content provided by the plugin.
   ```text
   plugins/
   └── FakePlayerPlus/
       └── messages/
           ├── messages.properties
           ├── messages_zh-TW.properties
           ├── messages_zh-CN.properties
   ```
