package com.agony.wmsallocation.dto.branch;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchDto {
    private String branchCode;
    private String salesOrgCode;
    private String branchName;
    private String address;
    private String phone;
    private ActiveStatus status;
}
