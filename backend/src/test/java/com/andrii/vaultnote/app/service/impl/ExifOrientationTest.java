package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExifOrientationTest {

  @Test
  void shouldMapExifValuesToNamedOrientations() {
    assertThat(ExifOrientation.fromValue(1)).isEqualTo(ExifOrientation.NORMAL);
    assertThat(ExifOrientation.fromValue(2)).isEqualTo(ExifOrientation.FLIP_HORIZONTAL);
    assertThat(ExifOrientation.fromValue(3)).isEqualTo(ExifOrientation.ROTATE_180);
    assertThat(ExifOrientation.fromValue(4)).isEqualTo(ExifOrientation.FLIP_VERTICAL);
    assertThat(ExifOrientation.fromValue(5)).isEqualTo(ExifOrientation.TRANSPOSE);
    assertThat(ExifOrientation.fromValue(6)).isEqualTo(ExifOrientation.ROTATE_90_CLOCKWISE);
    assertThat(ExifOrientation.fromValue(7)).isEqualTo(ExifOrientation.TRANSVERSE);
    assertThat(ExifOrientation.fromValue(8)).isEqualTo(ExifOrientation.ROTATE_270_CLOCKWISE);
  }

  @Test
  void shouldUseNormalOrientationForUnknownValue() {
    assertThat(ExifOrientation.fromValue(0)).isEqualTo(ExifOrientation.NORMAL);
    assertThat(ExifOrientation.fromValue(9)).isEqualTo(ExifOrientation.NORMAL);
  }

  @Test
  void shouldIdentifyOrientationsThatSwapDimensions() {
    assertThat(ExifOrientation.NORMAL.swapsDimensions()).isFalse();
    assertThat(ExifOrientation.FLIP_HORIZONTAL.swapsDimensions()).isFalse();
    assertThat(ExifOrientation.ROTATE_180.swapsDimensions()).isFalse();
    assertThat(ExifOrientation.FLIP_VERTICAL.swapsDimensions()).isFalse();
    assertThat(ExifOrientation.TRANSPOSE.swapsDimensions()).isTrue();
    assertThat(ExifOrientation.ROTATE_90_CLOCKWISE.swapsDimensions()).isTrue();
    assertThat(ExifOrientation.TRANSVERSE.swapsDimensions()).isTrue();
    assertThat(ExifOrientation.ROTATE_270_CLOCKWISE.swapsDimensions()).isTrue();
  }

  @Test
  void shouldProvideTransformForEveryOrientation() {
    for (var orientation : ExifOrientation.values()) {
      assertThat(orientation.transform(100, 80)).isNotNull();
    }
  }
}
