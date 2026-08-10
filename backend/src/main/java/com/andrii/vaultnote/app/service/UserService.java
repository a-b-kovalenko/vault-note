package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

  @PreAuthorize("hasRole('ADMIN')")
  Page<UserInfoDto> getUsers(Pageable pageable);

}
