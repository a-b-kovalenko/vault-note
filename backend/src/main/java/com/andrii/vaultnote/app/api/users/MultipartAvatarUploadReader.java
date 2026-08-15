package com.andrii.vaultnote.app.api.users;

import com.andrii.vaultnote.app.exception.AvatarValidationException;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MultipartAvatarUploadReader {

  public byte[] read(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException exception) {
      throw new AvatarValidationException("Avatar file could not be read.", exception);
    }
  }
}
