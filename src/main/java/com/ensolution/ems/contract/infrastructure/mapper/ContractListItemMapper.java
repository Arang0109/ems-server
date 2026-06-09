package com.ensolution.ems.contract.infrastructure.mapper;

import com.ensolution.ems.contract.application.command.ContractListItem;
import com.ensolution.ems.contract.infrastructure.entity.ContractTableViewEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
	componentModel = "spring",
	builder = @Builder(),
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ContractListItemMapper {

	ContractListItem toListItem(ContractTableViewEntity entity);
	List<ContractListItem> toListItems(List<ContractTableViewEntity> entities);
}
