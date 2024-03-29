<h4 align="right">
  <strong>English</strong> | <a href="https://github.com/hua0512/stream-rec/blob/main/docs/README_zh.md">简体中文</a>
</h4>

# Stream-rec

Stream-rec is an automatic stream recording tool for various streaming services.

It's powered by Kotlin, [Ktor](https://ktor.io/), and [ffmpeg](https://ffmpeg.org/).

This project is the result of my personal need for a tool that can automatically record live streams and upload them to cloud storage.

> [!WARNING]\
> This project is the result of my personal learning of Kotlin Coroutines, flow, Ktor, dao, repository pattern and other technologies.

# Features

- Automatic stream recording, with configurable quality and format.
- Automatic file naming based on the stream title and start time.
- Automatic Danmu(Bullet comments) recording
- Persistent storage of stream and upload information (using SQLite)
- Integration with [Rclone](https://rclone.org/) for uploading to cloud storage
- Configurable via TOML file
- Docker support

# Supported streaming services

|  Service  | Recording | Danmu |
|:---------:|:---------:|:-----:|
|   Huya    |     ✅     |   ✅   |
|  Douyin   |     ✅     |   ✅   |
|   Douyu   |     ❌     |   ❌   |
| Bilibili  |     ❌     |   ❌   |
|  Twitch   |     ❌     |   ❌   |
|  Youtube  |     ❌     |   ❌   |
| Niconico  |     ❌     |   ❌   |
| AfreecaTv |     ❌     |   ❌   |

- As for now, only Huya and Douyin are supported. (Because I only use these two services 😄).
- More services will be supported in the future (if I have time).

# Configuration

Configuration is done via a toml file. An [example](config/config-example.toml) configuration file is provided in the repository.

> [!IMPORTANT]\
> Please read **CAREFULLY** the comments in the example configuration file to understand how to configure the tool.

Configuration via web interface is planned for the future.

# Installation

# 1. Docker (Recommended)

## 1.1 Building the Docker Image

To build the Docker image, first clone the repository and navigate to the root directory of the project.

```shell
git clone https://github.com/hua0512/stream-rec.git
cd stream-rec
```

Then, build the Docker image using the following command:

```shell
docker build -t stream-rec .
```

## 1.2 Running the Docker Container

> [!IMPORTANT]\
> Before running the container, please create a **config.toml** configuration file.

To run the Docker container, use the following command:

```shell
docker run --rm -it -v /path/to/your/output:/your/config.toml/outputFolder --env CONFIG_PATH=/your/config.toml/outputFolder/config.toml --name stream-rec stream-rec
```

- Replace `/path/to/your/output` with the **host path** to the directory where you want to save the output file.
- Replace `/your/config.toml/outputFolder` with the **container path(outputFolder)** of the **config.toml** file.
- Replace `CONFIG_PATH` with the **container path** to the **config.toml** file.

> [!WARNING]\
> When CONFIG_PATH is not specified, the tool will look for the **config.toml** file in the application's working directory (/app)

> [!WARNING]\
> Database file will be created in the same directory as the **config.toml** file.

- Replace `stream-rec` with the name you want to give to the container.

The container will automatically start recording the stream according to the configuration file.

# 2. Building from source

## 2.1 Prerequisites

- Internet access, obviously 😂
- [Git](https://git-scm.com/downloads) (optional, for cloning the repository)
- A java development kit (JDK) (version 17 or
  later), [Amazon Corretto 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html) is recommended.
- [FFmpeg](https://ffmpeg.org/download.html) (Make sure it's in your `PATH`).
- [Rclone](https://rclone.org/downloads/) (optional, for uploading to cloud storage, make sure it's in your `PATH`)
- [Sqlite3](https://www.sqlite.org/download.html) (for storing stream, upload information, make sure it's in your `PATH`)

## 2.2 Building

To build the project, first clone the repository and navigate to the root directory of the project.

```shell
git clone https://github.com/hua0512/stream-rec.git
cd stream-rec
```

Then, build the project using the following command:

```shell
./gradlew stream-rec:build -x test
```

The built fat jar file `stream-rec.jar` will be located in the `stream-rec/build/libs` directory.

## 2.3 Running the jar file

> [!IMPORTANT]\
> Before running the container, please create a **config.toml** configuration file.


To run the jar file, use the following command:

```shell
java -jar stream-rec/build/libs/stream-rec.jar
```

You can also specify the path to the configuration file using the `CONFIG_PATH` environment variable:

```shell
java -DCONFIG_PATH=/path/to/your/config.toml -jar stream-rec/build/libs/stream-rec.jar
```

> [!WARNING]\
> If `CONFIG_PATH` is not specified, the tool will look for the `config.toml` file in the application's working directory.

> [!WARNING]\
> Database file will be created in the same directory as the `config.toml` file.

# Troubleshooting

Check logs under `logs` directory, by default, it will be created in the same directory as the `config.toml` file.

- There´s a environment variable `LOG_LEVEL` that can be set to `debug` to enable debug logs.

# Contributing

Contributions are welcome! If you have any ideas, suggestions, or bug reports, please feel free to open an issue or a
pull request.

# License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.