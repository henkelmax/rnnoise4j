# RNNoise4J

A Java wrapper for [RNNoise](https://jmvalin.ca/demo/rnnoise/) written in C using JNI.

Java 8+ is required to use this library.

## Supported Platforms

- `Windows x86_64`
- `Windows aarch64`
- `macOS x86_64`
- `macOS aarch64`
- `Linux x86_64`
- `Linux aarch64`

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.rnnoise4j</groupId>
  <artifactId>rnnoise4j</artifactId>
  <version>2.1.2</version>
</dependency>

<repositories>
  <repository>
    <id>henkelmax.public</id>
    <url>https://maven.maxhenkel.de/repository/public</url>
  </repository>
</repositories>
```

**Gradle**

``` groovy
dependencies {
  implementation 'de.maxhenkel.rnnoise4j:rnnoise4j:2.1.2'
}

repositories {
  maven {
    name = "henkelmax.public"
    url = 'https://maven.maxhenkel.de/repository/public'
  }
}
```

## Example Code

``` java
short[] noisyAudio = ...;
Denoiser denoiser = new Denoiser();
//Denoises and returns a new array containing the denoised audio
short[] denoisedAudio = denoiser.denoise(noisyAudio);
//Denoises the audio in-place and returns the probability of speech
float probability = denoiser.denoiseInPlace(noisyAudio);
denoiser.close();
```

## Building from Source

### Prerequisites

- [Java](https://www.java.com/en/) 21
- [Zig](https://ziglang.org/) 0.14.1
- [Ninja](https://ninja-build.org/)

### Building

``` bash
./gradlew build
```

## Credits

- [RNNoise](https://gitlab.xiph.org/xiph/rnnoise)
