package moo.interview.teya.mapper;

import moo.interview.teya.dto.response.OverdraftPolicyResponse;
import moo.interview.teya.entity.OverdraftPolicy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for OverdraftPolicy entity to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OverdraftPolicyMapper {

    /**
     * Map OverdraftPolicy entity to OverdraftPolicyResponse DTO.
     *
     * @param overdraftPolicy the overdraft policy entity
     * @return the overdraft policy response DTO
     */
    OverdraftPolicyResponse toResponse(OverdraftPolicy overdraftPolicy);
}

