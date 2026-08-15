package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.api.users.dto.UserProfileDto;
import com.andrii.vaultnote.app.exception.UserNotFoundException;
import com.andrii.vaultnote.app.mapper.UserMapper;
import com.andrii.vaultnote.app.security.CurrentUserProvider;
import com.andrii.vaultnote.app.service.UserService;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.andrii.vaultnote.users.domain.DisplayNameRules;
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
  CurrentUserProvider currentUserProvider;

  @Override
  @Transactional(readOnly = true)
  public Page<UserInfoDto> getUsers(Pageable pageable) {
    log.info("Getting users: page={}, size={}, sort={}",
      pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    return userRepository.findAll(pageable)
      .map(userMapper::toUserInfoDto);
  }

  @Override
  @Transactional(readOnly = true)
  public UserProfileDto getCurrentProfile() {
    var userId = currentUserProvider.currentUserId();
    log.info("Getting current user profile: userId={}", userId);
    return userMapper.toUserProfileDto(findCurrentUser(userId));
  }

  @Override
  @Transactional
  public UserProfileDto updateCurrentProfile(String displayName) {
    DisplayNameRules.validate(displayName);
    var userId = currentUserProvider.currentUserId();
    log.info("Updating current user profile: userId={}", userId);
    var user = findCurrentUser(userId);
    user.setDisplayName(displayName);
    return userMapper.toUserProfileDto(userRepository.saveAndFlush(user));
  }

  private UserEntity findCurrentUser(long userId) {
    return userRepository.findUserWithRolesById(userId)
      .orElseThrow(() -> new UserNotFoundException(userId));
  }

}
