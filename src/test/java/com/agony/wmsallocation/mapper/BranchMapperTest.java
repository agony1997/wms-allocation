package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.branch.BranchDto;
import com.agony.wmsallocation.entity.branch.Branch;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@Import(BranchMapperImpl.class)
class BranchMapperTest {

    // 編譯後才產生
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private BranchMapper branchMapper;

    @Test
    @DisplayName("toDto - 所有欄位應正確對應")
    void toDto_shouldMapAllFields() {
        Branch branch = new Branch();
        branch.setBranchCode("B001");
        branch.setSalesOrgCode("S001");
        branch.setBranchName("台北營業所");
        branch.setAddress("台北市中正區");
        branch.setPhone("02-12345678");
        branch.setStatus(ActiveStatus.ACTIVE);

        BranchDto dto = branchMapper.toDto(branch);

        assertThat(dto.getBranchCode()).isEqualTo("B001");
        assertThat(dto.getSalesOrgCode()).isEqualTo("S001");
        assertThat(dto.getBranchName()).isEqualTo("台北營業所");
        assertThat(dto.getAddress()).isEqualTo("台北市中正區");
        assertThat(dto.getPhone()).isEqualTo("02-12345678");
        assertThat(dto.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("toDto null 輸入 - 應回傳 null")
    void toDto_whenSourceIsNull_shouldReturnNull() {
        assertThat(branchMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("toDto 選填欄位為 null - 應正常對應不報錯")
    void toDto_whenOptionalFieldsAreNull_shouldMapWithoutError() {
        Branch branch = new Branch();
        branch.setBranchCode("B002");
        branch.setSalesOrgCode("S001");
        branch.setBranchName("高雄營業所");
        branch.setStatus(ActiveStatus.INACTIVE);
        // address, phone 不設值（nullable 欄位）

        BranchDto dto = branchMapper.toDto(branch);

        assertThat(dto.getBranchCode()).isEqualTo("B002");
        assertThat(dto.getAddress()).isNull();
        assertThat(dto.getPhone()).isNull();
    }
}
