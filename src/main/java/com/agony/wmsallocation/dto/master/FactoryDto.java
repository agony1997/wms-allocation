package com.agony.wmsallocation.dto.master;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FactoryDto {
    private String factoryCode;
    private String factoryName;
    private String address;
    private String phone;
    private ActiveStatus status;
}
