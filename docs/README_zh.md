<h4 align="right">
  <strong>简体中文</strong> | <a href="https://github.com/hua0512/stream-rec/blob/main/README.md">English</a>
</h4>

# Stream-rec

Stream-rec 是一个自动录制各种直播平台的工具。

基于 [Kotlin](https://kotlinlang.org/), [Ktor](https://ktor.io/), 和 [ffmpeg](https://ffmpeg.org/)。

本项目来源于我个人对一个能够自动录制直播,弹幕并支持分段上传到云存储的工具的需求。

> [!WARNING]\
> 本项目是我个人学习 Kotlin 协程、flow、Ktor、dao、repository 模式和其他技术的结果， 欢迎大家提出建议和意见。

# 功能列表

- 自动录播，可配置录制质量，路径，格式，并发量，分段录制（时间或文件大小），分段上传，根据直播标题和开始时间自动命名文件。
- 自动弹幕录制（XML格式），可使用 [DanmakuFactory](https://github.com/hihkm/DanmakuFactory) 进行弹幕转换，或配合[AList](https://alist.nn.ci/zh/)来实现弹幕自动挂载。
- 使用 [SQLite](https://www.sqlite.org/index.html) 持久化存储录播和上传信息
- 支持 [Rclone](https://rclone.org/) 上传到云存储
- 使用 TOML 文件进行配置
- 支持 Docker

# 直播平台支持列表

|    平台     | 录制 | 弹幕 |
|:---------:|:--:|:--:|
|    虎牙     | ✅  | ✅  |
|    抖音     | ✅  | ✅  |
|    斗鱼     | ❌  | ❌  |
| Bilibili  | ❌  | ❌  |
|  Twitch   | ❌  | ❌  |
|  Youtube  | ❌  | ❌  |
| Niconico  | ❌  | ❌  |
| AfreecaTv | ❌  | ❌  |

- 目前除了虎牙和抖音外，其他平台暂不支持。 (因为我只用这两个平台 😄)。
- 更多平台的支持将在未来加入 (如果我有时间的话)。

# 配置

配置文件使用 TOML 格式。 仓库中提供了一个 [示例](../config/config-example-zh.toml) 配置文件。

> [!IMPORTANT]\
> 请**仔细**阅读示例配置文件中的注释，以了解如何配置工具。

未来计划支持通过 Web 界面进行配置。

# 安装

# 1. Docker (推荐)

## 1.1 构建 Docker 镜像

首先，克隆仓库并进入项目的根目录。

```shell
git clone https://github.com/hua0512/stream-rec.git
cd stream-rec
```

然后，使用以下命令构建 Docker 镜像：

```shell
docker build -t stream-rec .
```

## 1.2 运行 Docker 容器

> [!IMPORTANT]\
> 运行前请先创建 **config.toml** 配置文件。

使用以下命令运行 Docker 容器：

```shell
docker run --rm -it -v /path/to/your/output:/your/config.toml/outputFolder --env CONFIG_PATH=/your/config.toml/outputFolder/config.toml --name stream-rec stream-rec
```

- 将 `/path/to/your/output` 替换为您要保存输出文件的目录的**主机路径**。
- 将 `/your/config.toml/outputFolder` 替换为 **config.toml** 文件的**容器路径(outputFolder)**。
- 将 `CONFIG_PATH` 替换为容器里的 **config.toml** 文件的路径。

> [!WARNING]\
> 不指定CONFIG_PATH环境变量的情况下，工具默认会在程序运行目录中查找 **config.toml** 文件。

> [!WARNING]\
> 数据库文件将在与 **config.toml** 文件相同的目录中创建。

- 可选将 `stream-rec` 替换为容器的名称。

容器将根据配置文件自动开始录制。

# 2. 从源码构建

## 2.1 环境要求

- 有魔法的网络（虽然但是，你都能上 GitHub 了，应该没问题）
- [Git](https://git-scm.com/downloads) (可选，用于克隆仓库)
- [Java 开发环境 (JDK)] (版本 17 或更高),
  推荐使用 [Amazon Corretto 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)。
- [FFmpeg](https://ffmpeg.org/download.html) (确保它在你的系统变量 `PATH` 中)。
- [Rclone](https://rclone.org/downloads/) (可选，用于上传到云存储，确保它在你的系统变量 `PATH` 中)
- [Sqlite3](https://www.sqlite.org/download.html) (用于存储录播和上传信息，确保它在你的系统变量 `PATH` 中)

## 2.2 构建

首先，克隆仓库并进入项目的根目录。

```shell
git clone https://github.com/hua0512/stream-rec.git
cd stream-rec
```

然后，使用以下命令构建项目：

```shell
./gradlew stream-rec:build -x test
```

构建的 fat jar 文件 `stream-rec.jar` 将位于 `stream-rec/build/libs` 目录中。

## 2.3 运行 jar 文件

> [!IMPORTANT]\
> 运行前请先创建 **config.toml** 配置文件。


使用以下命令运行 jar 文件：

```shell
java -jar stream-rec/build/libs/stream-rec.jar
```

可以使用CONFIG_PATH环境变量来指定配置文件的路径。

```shell
java -DCONFIG_PATH=/path/to/your/config.toml -jar stream-rec/build/libs/stream-rec.jar
```

> [!WARNING]\
> 不指定CONFIG_PATH环境变量的情况下，工具默认会在程序运行目录中查找 **config.toml** 文件。

> [!WARNING]\
> 数据库文件将在于 `config.toml` 文件相同的目录中创建。

# 故障排除

- 如果您遇到任何问题，请首先查看 [ISSUES](https://github.com/hua0512/stream-rec/issues)
- 工具默认会将日志输出到 `config.toml` 文件所在的目录，`logs` 目录中。
- 可以设置环境变量 `LOG_LEVEL` 为 `debug` 来启用调试日志。
- 如果您仍然遇到问题，请随时提出问题。

# 贡献

欢迎贡献！如果您有任何想法、建议或错误报告，请随时提出问题或拉取请求。

# 许可证

本项目根据 MIT 许可证进行许可。有关详细信息，请参阅 [LICENSE](../LICENSE) 文件。