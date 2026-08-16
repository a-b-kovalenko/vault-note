package com.andrii.vaultnote.app.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.security.oauth2.GoogleAvatarImageClient;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@TestConfiguration(proxyBeanMethods = false)
class GoogleOAuth2TestConfiguration {

  static final String GOOGLE_PICTURE_URL = "https://lh3.googleusercontent.com/wiremock-avatar";
  static final String GOOGLE_NONCE = "wiremock-test-nonce";

  private static final String NONCE_PARAMETER = "nonce";

  @Bean
  @Primary
  OAuth2AuthorizationRequestResolver fixedNonceAuthorizationRequestResolver(
    @Qualifier("oauth2AuthorizationRequestResolver") OAuth2AuthorizationRequestResolver delegate) {
    return new FixedNonceAuthorizationRequestResolver(delegate);
  }

  @Bean
  @Primary
  GoogleAvatarImageClient googleAvatarImageClient() {
    var imageClient = mock(GoogleAvatarImageClient.class);
    when(imageClient.download(GOOGLE_PICTURE_URL)).thenReturn(testImage());
    return imageClient;
  }

  private static byte[] testImage() {
    var image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.dispose();

    try (var output = new ByteArrayOutputStream()) {
      assertThat(ImageIO.write(image, "png", output)).isTrue();
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not create test avatar", exception);
    }
  }

  private record FixedNonceAuthorizationRequestResolver(OAuth2AuthorizationRequestResolver delegate)
    implements
      OAuth2AuthorizationRequestResolver {

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
      return withFixedNonce(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
      HttpServletRequest request,
      String clientRegistrationId) {
      return withFixedNonce(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest withFixedNonce(
      OAuth2AuthorizationRequest request) {
      if (request == null) {
        return null;
      }

      var additionalParameters = new LinkedHashMap<>(request.getAdditionalParameters());
      additionalParameters.put(NONCE_PARAMETER, GOOGLE_NONCE);
      var attributes = new LinkedHashMap<>(request.getAttributes());
      attributes.put(NONCE_PARAMETER, GOOGLE_NONCE);

      return OAuth2AuthorizationRequest.from(request)
        .additionalParameters(additionalParameters)
        .attributes(attributes)
        .build();
    }
  }
}
