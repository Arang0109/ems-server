package com.ensolution.ems.client_management.infrastructure.mapper;

import com.ensolution.ems.client_management.domain.Company;
import com.ensolution.ems.client_management.infrastructure.entity.CompanyEntity;
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
public interface CompanyEntityMapper {
	@Mapping(target = "workplaces", ignore = true)
	CompanyEntity toEntity(Company company);

	Company toDomain(CompanyEntity company);

	List<Company> toDomainList(List<CompanyEntity> companies);
}
