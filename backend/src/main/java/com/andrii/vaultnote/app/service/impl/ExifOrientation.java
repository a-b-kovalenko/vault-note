package com.andrii.vaultnote.app.service.impl;

import java.awt.geom.AffineTransform;

enum ExifOrientation {
  NORMAL(1, false), FLIP_HORIZONTAL(2, false), ROTATE_180(3, false), FLIP_VERTICAL(4, false), TRANSPOSE(5,
    true), ROTATE_90_CLOCKWISE(6, true), TRANSVERSE(7, true), ROTATE_270_CLOCKWISE(8, true);

  private final int value;
  private final boolean swapsDimensions;

  ExifOrientation(int value, boolean swapsDimensions) {
    this.value = value;
    this.swapsDimensions = swapsDimensions;
  }

  static ExifOrientation fromValue(int value) {
    for (var orientation : values()) {
      if (orientation.value == value) {
        return orientation;
      }
    }
    return NORMAL;
  }

  boolean swapsDimensions() {
    return swapsDimensions;
  }

  AffineTransform transform(int width, int height) {
    return switch (this) {
      case NORMAL -> new AffineTransform();
      case FLIP_HORIZONTAL -> new AffineTransform(-1, 0, 0, 1, width, 0);
      case ROTATE_180 -> new AffineTransform(-1, 0, 0, -1, width, height);
      case FLIP_VERTICAL -> new AffineTransform(1, 0, 0, -1, 0, height);
      case TRANSPOSE -> new AffineTransform(0, 1, 1, 0, 0, 0);
      case ROTATE_90_CLOCKWISE -> new AffineTransform(0, 1, -1, 0, height, 0);
      case TRANSVERSE -> new AffineTransform(0, -1, -1, 0, height, width);
      case ROTATE_270_CLOCKWISE -> new AffineTransform(0, -1, 1, 0, 0, width);
    };
  }
}
