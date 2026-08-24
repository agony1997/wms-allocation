package com.agony.wmsallocation.dto.master;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerDto {
    private String customerCode;
    private String customerName;
    private String salesOrgCode;
    private String userCode;
    private String address;
    private String phone;
    private ActiveStatus status;
}
