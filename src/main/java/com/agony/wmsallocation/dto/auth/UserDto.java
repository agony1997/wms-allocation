package com.agony.wmsallocation.dto.auth;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private String userCode;
    private String email;
    private String userName;
    private String branchCode;
    private String phone;
    private ActiveStatus status;
    // ponytail: password 刻意不列入，敏感欄位不外露
}
