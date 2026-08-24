package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.auth.UserDto;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.AuthUserMapper;
import com.agony.wmsallocation.repository.AuthUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AuthUserService {

    private final AuthUserRepo authUserRepo;
    private final AuthUserMapper authUserMapper;

    public List<UserDto> findAll() {
        return authUserRepo.findAll().stream()
                .map(authUserMapper::toDto)
                .toList();
    }

    public List<UserDto> findAllActive() {
        return authUserRepo.findByStatus(ActiveStatus.ACTIVE).stream()
                .map(authUserMapper::toDto)
                .toList();
    }

    public UserDto findByUserCode(String userCode) {
        return authUserRepo.findByUserCode(userCode)
                .map(authUserMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者：userCode=" + userCode));
    }

}
