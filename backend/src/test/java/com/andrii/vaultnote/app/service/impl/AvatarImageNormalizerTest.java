package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andrii.vaultnote.app.config.AvatarProperties;
import com.andrii.vaultnote.app.exception.AvatarValidationException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

class AvatarImageNormalizerTest {

  private static final AvatarProperties PROPERTIES = new AvatarProperties(
    DataSize.ofMegabytes(2),
    64,
    2048,
    2048,
    4_194_304,
    256,
    0.85);

  private final AvatarImageNormalizer normalizer = new AvatarImageNormalizer(PROPERTIES);

  @ParameterizedTest
  @ValueSource(strings = {"jpeg", "png"})
  void shouldNormalizeSupportedImageFormatsToCanonicalJpeg(String format) throws IOException {
    var normalized = normalizer.normalize(imageBytes(format, 80, 120));
    var decoded = ImageIO.read(new ByteArrayInputStream(normalized.content()));

    assertThat(decoded.getWidth()).isEqualTo(256);
    assertThat(decoded.getHeight()).isEqualTo(256);
    assertThat(normalized.byteSize()).isEqualTo(normalized.content().length);
    assertThat(normalized.content()).startsWith((byte) 0xFF, (byte) 0xD8);
  }

  @Test
  void shouldDecodeWebpInput() throws IOException {
    var webpProperties = new AvatarProperties(
      PROPERTIES.maxUploadSize(),
      1,
      PROPERTIES.maxInputWidth(),
      PROPERTIES.maxInputHeight(),
      PROPERTIES.maxInputPixels(),
      PROPERTIES.outputSize(),
      PROPERTIES.jpegQuality());
    var webp = Base64.getDecoder().decode(
      "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");

    var normalized = new AvatarImageNormalizer(webpProperties).normalize(webp);

    assertThat(ImageIO.read(new ByteArrayInputStream(normalized.content())))
      .extracting(BufferedImage::getWidth, BufferedImage::getHeight)
      .containsExactly(256, 256);
  }

  @Test
  void shouldRejectContentThatIsNotAnImage() {
    assertThatThrownBy(() -> normalizer.normalize("not-an-image".getBytes()))
      .isInstanceOf(AvatarValidationException.class)
      .hasMessage("Avatar content is not a supported image.");
  }

  @Test
  void shouldRejectImageSmallerThanMinimumDimensions() throws IOException {
    assertThatThrownBy(() -> normalizer.normalize(imageBytes("png", 63, 100)))
      .isInstanceOf(AvatarValidationException.class)
      .hasMessage("Avatar dimensions must be at least 64x64.");
  }

  @Test
  void shouldRejectImageLargerThanMaximumDimensions() throws IOException {
    assertThatThrownBy(() -> normalizer.normalize(imageBytes("png", 2049, 100)))
      .isInstanceOf(AvatarValidationException.class)
      .hasMessage("Avatar dimensions exceed the allowed limit.");
  }

  private static byte[] imageBytes(String format, int width, int height) throws IOException {
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    try {
      graphics.setColor(Color.BLUE);
      graphics.fillRect(0, 0, width, height);
    } finally {
      graphics.dispose();
    }

    var output = new ByteArrayOutputStream();
    assertThat(ImageIO.write(image, format, output)).isTrue();
    return output.toByteArray();
  }
}
