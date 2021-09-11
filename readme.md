# RNNoise4J

A Java wrapper for [RNNoise](https://jmvalin.ca/demo/rnnoise/).

## Usage

**Maven**

``` xml
<dependency>
  <groupId>de.maxhenkel.rnnoise4j</groupId>
  <artifactId>rnnoise4j</artifactId>
  <version>1.0.0</version>
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
  implementation 'de.maxhenkel.rnnoise4j:rnnoise4j:1.0.0'
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
Pointer state = RNNoise.INSTANCE.rnnoise_create(null);

...

float[] audioData = ...; // Raw 16-bit mono PCM sampled audio at 48 kHz
float[] denoisedAudio = new float[audioData.length];
RNNoise.INSTANCE.rnnoise_process_frame(state, denoisedAudio, audioData);

...

RNNoise.INSTANCE.rnnoise_destroy(state);
```

## Sources

- [RNNoise](https://gitlab.xiph.org/xiph/rnnoise)
- [RNNoise Binaries](https://github.com/mjwells2002/rnnoise-bin)