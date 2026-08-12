package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.mapper.UserMapper;
import com.andrii.vaultnote.app.service.UserService;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

  UserJpaRepository userRepository;
  UserMapper userMapper;

  @Override
  @Transactional(readOnly = true)
  public Page<UserInfoDto> getUsers(Pageable pageable) {
    log.info("Getting users: page={}, size={}, sort={}",
      pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    return userRepository.findAll(pageable)
      .map(userMapper::toUserInfoDto);
  }
}
