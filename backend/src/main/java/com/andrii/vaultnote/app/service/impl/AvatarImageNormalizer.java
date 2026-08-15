package com.andrii.vaultnote.app.service.impl;

import static java.util.Locale.ROOT;

import com.andrii.vaultnote.app.config.AvatarProperties;
import com.andrii.vaultnote.app.exception.AvatarValidationException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AvatarImageNormalizer {

  private static final Set<String> SUPPORTED_FORMATS = Set.of("jpeg", "jpg", "png", "webp");

  AvatarProperties properties;

  public NormalizedAvatar normalize(byte[] source) {
    validateByteSize(source);

    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
      if (input == null) {
        throw invalid("Avatar content is not a readable image.");
      }

      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw invalid("Avatar content is not a supported image.");
      }

      return normalize(source, input, readers.next());
    } catch (AvatarValidationException exception) {
      throw exception;
    } catch (IOException | RuntimeException exception) {
      throw invalid("Avatar content could not be decoded.", exception);
    }
  }

  private NormalizedAvatar normalize(byte[] source, ImageInputStream input, ImageReader reader) {
    try {
      var format = reader.getFormatName().toLowerCase(ROOT);
      if (!SUPPORTED_FORMATS.contains(format)) {
        throw invalid("Avatar must be a JPEG, PNG, or WebP image.");
      }

      reader.setInput(input, true, true);
      var width = reader.getWidth(0);
      var height = reader.getHeight(0);
      validateDimensions(width, height);

      var image = reader.read(0);
      if (image == null) {
        throw invalid("Avatar content could not be decoded.");
      }

      var oriented = applyExifOrientation(image, readExifOrientation(source));
      var normalized = resizeToSquare(oriented);
      return new NormalizedAvatar(encodeJpeg(normalized));
    } catch (AvatarValidationException exception) {
      throw exception;
    } catch (IOException | RuntimeException exception) {
      throw invalid("Avatar content could not be normalized.", exception);
    } finally {
      reader.dispose();
    }
  }

  private void validateByteSize(byte[] source) {
    if (source == null || source.length == 0) {
      throw invalid("Avatar file must not be empty.");
    }
    if (source.length > properties.maxUploadSize().toBytes()) {
      throw invalid("Avatar file is too large.");
    }
  }

  private void validateDimensions(int width, int height) {
    if (width < properties.minInputDimension() || height < properties.minInputDimension()) {
      throw invalid("Avatar dimensions must be at least %dx%d."
        .formatted(properties.minInputDimension(), properties.minInputDimension()));
    }
    if (width > properties.maxInputWidth() || height > properties.maxInputHeight()) {
      throw invalid("Avatar dimensions exceed the allowed limit.");
    }
    if ((long) width * height > properties.maxInputPixels()) {
      throw invalid("Avatar pixel count exceeds the allowed limit.");
    }
  }

  private BufferedImage resizeToSquare(BufferedImage source) {
    var outputSize = properties.outputSize();
    var scale = outputSize / (double) Math.min(source.getWidth(), source.getHeight());
    var scaledWidth = (int) Math.ceil(source.getWidth() * scale);
    var scaledHeight = (int) Math.ceil(source.getHeight() * scale);
    var x = (outputSize - scaledWidth) / 2;
    var y = (outputSize - scaledHeight) / 2;

    var target = new BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_RGB);
    var graphics = target.createGraphics();
    try {
      graphics.setComposite(AlphaComposite.Src);
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, outputSize, outputSize);
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(source, x, y, scaledWidth, scaledHeight, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  private byte[] encodeJpeg(BufferedImage image) throws IOException {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      throw new IOException("JPEG encoder is not available.");
    }

    var writer = writers.next();
    try (var output = new ByteArrayOutputStream();
      ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
      var parameters = writer.getDefaultWriteParam();
      parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      parameters.setCompressionQuality((float) properties.jpegQuality());
      writer.setOutput(imageOutput);
      writer.write(null, new IIOImage(image, null, null), parameters);
      imageOutput.flush();
      return output.toByteArray();
    } finally {
      writer.dispose();
    }
  }

  private static ExifOrientation readExifOrientation(byte[] source) {
    try {
      var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(source));
      var directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      return directory == null
        ? ExifOrientation.NORMAL
        : ExifOrientation.fromValue(directory.getInt(ExifIFD0Directory.TAG_ORIENTATION));
    } catch (ImageProcessingException | IOException | MetadataException exception) {
      return ExifOrientation.NORMAL;
    }
  }

  private static BufferedImage applyExifOrientation(
    BufferedImage source,
    ExifOrientation orientation) {
    if (orientation == ExifOrientation.NORMAL) {
      return source;
    }

    var width = source.getWidth();
    var height = source.getHeight();
    var target = new BufferedImage(
      orientation.swapsDimensions() ? height : width,
      orientation.swapsDimensions() ? width : height,
      BufferedImage.TYPE_INT_ARGB);
    var transform = orientation.transform(width, height);
    var graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.drawImage(source, transform, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  private static AvatarValidationException invalid(String message) {
    return new AvatarValidationException(message);
  }

  private static AvatarValidationException invalid(String message, Throwable cause) {
    return new AvatarValidationException(message, cause);
  }

  public record NormalizedAvatar(byte[] content) {

    public int byteSize() {
      return content.length;
    }
  }
}
