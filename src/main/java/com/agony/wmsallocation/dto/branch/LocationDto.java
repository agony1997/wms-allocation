package com.agony.wmsallocation.dto.branch;

import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationDto {
    private String locationCode;
    private String locationName;
    private String branchCode;
    private String userCode;
    private LocationType locationType;
    private ActiveStatus status;
}
