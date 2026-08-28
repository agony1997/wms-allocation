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
    private String phone;
    private ActiveStatus status;
    // ponytail: password 刻意不列入，敏感欄位不外露
    // 無 branchCode：使用者不掛營業所，要回營業所歸屬得從 AuthUserBranchRole／Location 組，
    // 目前兩支查詢端點都不需要，等真的要了再加（見 User.md「資料結構」）
}
