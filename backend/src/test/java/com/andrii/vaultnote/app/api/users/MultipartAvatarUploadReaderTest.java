package com.andrii.vaultnote.app.api.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.exception.AvatarValidationException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class MultipartAvatarUploadReaderTest {

  private final MultipartAvatarUploadReader reader = new MultipartAvatarUploadReader();

  @Test
  void shouldReadMultipartFileBytes() {
    var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});

    assertThat(reader.read(file)).containsExactly(1, 2, 3);
  }

  @Test
  void shouldTranslateReadFailure() throws IOException {
    var file = mock(MultipartFile.class);
    when(file.getBytes()).thenThrow(new IOException("read failed"));

    assertThatThrownBy(() -> reader.read(file))
      .isInstanceOf(AvatarValidationException.class)
      .hasMessage("Avatar file could not be read.")
      .hasCauseInstanceOf(IOException.class);
  }
}
