# RNNoise4J

A Java wrapper for [RNNoise](https://jmvalin.ca/demo/rnnoise/) written in Rust using JNI.

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.rnnoise4j</groupId>
  <artifactId>rnnoise4j</artifactId>
  <version>2.0.0</version>
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
  implementation 'de.maxhenkel.rnnoise4j:rnnoise4j:2.0.0'
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
- [nnnoiseless](https://github.com/jneem/nnnoiseless)
- [jni-rs](https://github.com/jni-rs/jni-rs)
