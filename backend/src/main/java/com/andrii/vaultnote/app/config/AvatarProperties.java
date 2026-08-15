package com.andrii.vaultnote.app.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.avatar")
public record AvatarProperties(
  @NotNull DataSize maxUploadSize,
  @Min(1) int minInputDimension,
  @Min(1) int maxInputWidth,
  @Min(1) int maxInputHeight,
  @Min(1) long maxInputPixels,
  @Min(1) int outputSize,
  @DecimalMin("0.0") @DecimalMax("1.0") double jpegQuality) {
}
