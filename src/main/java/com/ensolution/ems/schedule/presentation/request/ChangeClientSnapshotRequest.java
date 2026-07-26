package com.ensolution.ems.schedule.presentation.request;

import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "의뢰기관 스냅샷 수정 요청. 전달하지 않은 필드는 기존 값을 유지하며, "
	+ "하위 사업장·측정시설도 중첩 전달로 부분 수정할 수 있습니다.")
public record ChangeClientSnapshotRequest(
	@Schema(description = "의뢰기관명")
	String name,

	@Schema(description = "사업자등록번호")
	String bizNumber,

	@Schema(description = "대표자")
	String representative,

	@Schema(description = "도로명주소")
	String roadAddress,

	@Schema(description = "상세주소")
	String detailAddress,

	@Schema(description = "우편번호")
	String zipcode,

	@Schema(description = "시설담당자")
	String facilityManager,

	@Schema(description = "채취입회인")
	String samplingWitness,

	@Schema(description = "이메일")
	String email,

	@Schema(description = "전화번호")
	String tel,

	@Schema(description = "사업장 스냅샷. 하위 측정시설(stack)까지 부분 수정되며, 배출·방지시설 목록은 전달하면 전체 교체합니다. "
		+ "측정시설의 표준산소농도·굴뚝 형상·높이 등이 바뀌면 측정 시트가 재계산됩니다.")
	WorkplaceSnapshot workplace
) {}
