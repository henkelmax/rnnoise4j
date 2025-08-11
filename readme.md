# RNNoise4J

A Java wrapper for [RNNoise](https://jmvalin.ca/demo/rnnoise/) written in C using JNI.


This library includes natives for:

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
  <version>2.1.0</version>
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
  implementation 'de.maxhenkel.rnnoise4j:rnnoise4j:2.1.0'
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
short[] denoisedAudio = denoiser.denoise(noisyAudio);
denoiser.close();
```

## Sources

- [RNNoise](https://gitlab.xiph.org/xiph/rnnoise)
