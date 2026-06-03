package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.infrastructure.JpaCompanyEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
		componentModel = "spring",
		builder = @Builder(),
		unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CompanyDomainEntityMapper {
	@Mapping(target = "workplaces", ignore = true)
	JpaCompanyEntity toEntity(Company company);
	
	Company toDomain(JpaCompanyEntity company);
	
	List<Company> toDomainList(List<JpaCompanyEntity> companies);
}
