package com.andrii.vaultnote.app.security.oauth2;

import com.andrii.vaultnote.app.config.GoogleAvatarProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoogleAvatarImageClient {

  private static final int BUFFER_SIZE = 8192;
  private static final int MAX_JAVA_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  GoogleAvatarProperties properties;
  HttpClient httpClient;
  Set<String> allowedHosts;

  @Autowired
  public GoogleAvatarImageClient(GoogleAvatarProperties properties) {
    this(properties, createHttpClient(properties));
  }

  GoogleAvatarImageClient(GoogleAvatarProperties properties, HttpClient httpClient) {
    this.properties = properties;
    this.httpClient = httpClient;
    this.allowedHosts = properties.allowedHosts().stream()
      .map(host -> host.toLowerCase(Locale.ROOT))
      .collect(Collectors.toUnmodifiableSet());
    validateProperties(properties);
  }

  public byte[] download(String pictureUrl) {
    var uri = validateUri(pictureUrl);
    var request = HttpRequest.newBuilder(uri)
      .timeout(properties.requestTimeout())
      .header("Accept", "image/jpeg,image/png,image/webp")
      .GET()
      .build();

    try {
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (var body = response.body()) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          throw new IllegalStateException("Google avatar request returned a non-success status");
        }

        var contentLength = response.headers().firstValueAsLong("Content-Length");
        if (contentLength.isPresent() && contentLength.getAsLong() > maxDownloadSize()) {
          throw new IllegalStateException("Google avatar response is too large");
        }

        return readBounded(body);
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Google avatar request was interrupted", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("Google avatar request failed", exception);
    }
  }

  private URI validateUri(String pictureUrl) {
    if (!StringUtils.hasText(pictureUrl)) {
      throw new IllegalArgumentException("Google avatar URL must not be blank");
    }

    final URI uri;
    try {
      uri = new URI(pictureUrl.trim());
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("Google avatar URL is invalid", exception);
    }

    var host = uri.getHost();
    if (!"https".equalsIgnoreCase(uri.getScheme())
      || !StringUtils.hasText(host)
      || !allowedHosts.contains(host.toLowerCase(Locale.ROOT))
      || uri.getPort() != -1
      || uri.getUserInfo() != null
      || uri.getFragment() != null) {
      throw new IllegalArgumentException("Google avatar URL is not allowed");
    }

    return uri;
  }

  private byte[] readBounded(InputStream body) throws IOException {
    var output = new ByteArrayOutputStream();
    var buffer = new byte[BUFFER_SIZE];
    int read;
    while ((read = body.read(buffer)) != -1) {
      output.write(buffer, 0, read);
      if (output.size() > maxDownloadSize()) {
        throw new IllegalStateException("Google avatar response is too large");
      }
    }
    return output.toByteArray();
  }

  private int maxDownloadSize() {
    return (int) properties.maxDownloadSize().toBytes();
  }

  private static HttpClient createHttpClient(GoogleAvatarProperties properties) {
    return HttpClient.newBuilder()
      .connectTimeout(properties.connectTimeout())
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();
  }

  private static void validateProperties(GoogleAvatarProperties properties) {
    var maxDownloadSize = properties.maxDownloadSize().toBytes();
    if (properties.connectTimeout() == null || properties.connectTimeout().isZero()
      || properties.connectTimeout().isNegative()
      || properties.requestTimeout() == null || properties.requestTimeout().isZero()
      || properties.requestTimeout().isNegative()
      || maxDownloadSize <= 0
      || maxDownloadSize > MAX_JAVA_ARRAY_SIZE
      || properties.allowedHosts().isEmpty()) {
      throw new IllegalArgumentException("Google avatar client properties are invalid");
    }
  }
}
