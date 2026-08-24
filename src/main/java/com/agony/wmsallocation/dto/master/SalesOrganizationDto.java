package com.agony.wmsallocation.dto.master;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesOrganizationDto {
    private String salesOrgCode;
    private String salesOrgName;
    private ActiveStatus status;
}
