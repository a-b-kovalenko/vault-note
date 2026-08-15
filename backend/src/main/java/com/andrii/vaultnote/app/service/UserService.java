package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.api.users.dto.UserAvatarDto;
import com.andrii.vaultnote.app.api.users.dto.UserProfileDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

  @PreAuthorize("hasRole('ADMIN')")
  Page<UserInfoDto> getUsers(Pageable pageable);

  @PreAuthorize("isAuthenticated()")
  UserProfileDto getCurrentProfile();

  @PreAuthorize("isAuthenticated()")
  UserProfileDto updateCurrentProfile(String displayName);

  @PreAuthorize("isAuthenticated()")
  UserAvatarDto uploadCurrentAvatar(byte[] content);

  @PreAuthorize("isAuthenticated()")
  byte[] getCurrentAvatar();

  @PreAuthorize("isAuthenticated()")
  void deleteCurrentAvatar();

}
