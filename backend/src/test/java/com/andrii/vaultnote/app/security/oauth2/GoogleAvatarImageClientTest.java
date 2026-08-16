package com.andrii.vaultnote.app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.config.GoogleAvatarProperties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.unit.DataSize;

class GoogleAvatarImageClientTest {

  private static final String ALLOWED_URL = "https://lh3.googleusercontent.com/avatar";
  private static final byte[] IMAGE_CONTENT = {1, 2, 3};

  @Mock
  HttpClient httpClient;
  @Mock
  HttpResponse<InputStream> response;

  GoogleAvatarImageClient imageClient;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    imageClient = new GoogleAvatarImageClient(properties(10), httpClient);
  }

  @Test
  void shouldDownloadImageFromAllowedGoogleHost() throws Exception {
    when(response.statusCode()).thenReturn(200);
    when(response.headers()).thenReturn(headers(Map.of()));
    when(response.body()).thenReturn(new ByteArrayInputStream(IMAGE_CONTENT));
    when(httpClient.send(
      any(HttpRequest.class),
      ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
      .thenReturn(response);

    var result = imageClient.download(ALLOWED_URL);

    assertThat(result).containsExactly(IMAGE_CONTENT);
    var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(
      requestCaptor.capture(),
      ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any());
    assertThat(requestCaptor.getValue().uri().toString()).isEqualTo(ALLOWED_URL);
    assertThat(requestCaptor.getValue().headers().firstValue("Accept"))
      .contains("image/jpeg,image/png,image/webp");
  }

  @Test
  void shouldRejectUrlOutsideGoogleHostAllowlist() {
    assertThatThrownBy(() -> imageClient.download("https://example.com/avatar"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Google avatar URL is not allowed");

    verifyNoInteractions(httpClient);
  }

  @Test
  void shouldRejectRedirectResponse() throws Exception {
    when(response.statusCode()).thenReturn(302);
    when(response.body()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(httpClient.send(
      any(HttpRequest.class),
      ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
      .thenReturn(response);

    assertThatThrownBy(() -> imageClient.download(ALLOWED_URL))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Google avatar request returned a non-success status");
  }

  @Test
  void shouldRejectResponseThatExceedsConfiguredLimit() throws Exception {
    when(response.statusCode()).thenReturn(200);
    when(response.headers()).thenReturn(headers(Map.of("Content-Length", List.of("11"))));
    when(response.body()).thenReturn(new ByteArrayInputStream(IMAGE_CONTENT));
    when(httpClient.send(
      any(HttpRequest.class),
      ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any()))
      .thenReturn(response);

    assertThatThrownBy(() -> imageClient.download(ALLOWED_URL))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Google avatar response is too large");
  }

  private static GoogleAvatarProperties properties(long maxDownloadSize) {
    return new GoogleAvatarProperties(
      Duration.ofSeconds(1),
      Duration.ofSeconds(1),
      DataSize.ofBytes(maxDownloadSize),
      Set.of("lh3.googleusercontent.com"));
  }

  private static HttpHeaders headers(Map<String, List<String>> values) {
    return HttpHeaders.of(values, (name, value) -> true);
  }
}
