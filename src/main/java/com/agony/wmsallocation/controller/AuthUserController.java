package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.auth.UserDto;
import com.agony.wmsallocation.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class AuthUserController {

    private final AuthUserService authUserService;

    @GetMapping
    public List<UserDto> findAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return authUserService.findAllActive();
        }
        return authUserService.findAll();
    }

    @GetMapping("/{userCode}")
    public UserDto findByUserCode(@PathVariable String userCode) {
        return authUserService.findByUserCode(userCode);
    }

}
