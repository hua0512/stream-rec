# stream-rec

Stream-rec is a simple CLI tool to record streams from streaming services.

It's powered by Kotlin, [Ktor](https://ktor.io/), and [ffmpeg](https://ffmpeg.org/).

# Features

- Stream recording, with configurable quality and format.
- Automatic file naming based on the stream title and start time.
- Danmu(Bullet comments) recording
- Integration with [Rclone](https://rclone.org/) for uploading to cloud storage
- Configurable via toml file
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

Configuration is done via a toml file. An [example](config-example.toml) configuration file is provided in the repository.

Please read **CAREFULLY** the comments in the example configuration file to understand how to configure the tool.

# Installation

# Docker

## Building the Docker Image

To build the Docker image, first clone the repository and navigate to the root directory of the project.

```shell
git clone https://github.com/hua0512/stream-rec.git
cd stream-rec
```

Then, build the Docker image using the following command:

```shell
docker build -t stream-rec .
```

## Running the Docker Container

To run the Docker container, use the following command:

```shell
docker run --rm -v /path/to/your/output:/output stream-rec -env CONFIG_PATH=/output/config.toml --name stream-rec stream-rec
```

- Replace `/path/to/your/output` with the host path to the directory where you want to save the output file.
  Please make sure that this path is the same as the path you specified in the **config.toml** file.
- Replace `CONFIG_PATH` with the path to the **config.toml** file.
  Per default, the tool will look for the **config.toml
  ** file in the `/output` directory.
- Replace `stream-rec` with the name you want to give to the container.

The container will automatically start recording the stream according to the configuration file.

# Building from source

## Prerequisites

- Internet access, obviously 😂
- [Git](https://git-scm.com/downloads) (optional, for cloning the repository)
- A java runtime environment (JRE) (version 11 or later), recommended to
  use [Amazon Corretto 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
- [FFmpeg](https://ffmpeg.org/download.html) (Make sure it's in your `PATH`).
- [Rclone](https://rclone.org/downloads/) (optional, for uploading to cloud storage, make sure it's in your `PATH`)

## Building

To build the project, first clone the repository and navigate to the root directory of the project.

```shell
git clone https://github.com/hua0512/stream-rec.git
cd stream-rec
```

Then, build the project using the following command:

```shell
./gradlew build
```

The built jar file `stream-rec.jar` will be located in the `build/libs` directory.

To run the jar file, use the following command:

```shell
java -jar build/libs/stream-rec.jar -env CONFIG_PATH=/path/to/your/config.toml
```

- If `CONFIG_PATH` is not specified, the tool will look for the **config.toml** file in the current directory.

# Contributing

Contributions are welcome! If you have any ideas, suggestions, or bug reports, please feel free to open an issue or a
pull request.

# License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.