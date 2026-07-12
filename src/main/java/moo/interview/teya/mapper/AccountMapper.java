package moo.interview.teya.mapper;

import moo.interview.teya.dto.response.AccountResponse;
import moo.interview.teya.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Account entity to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {

    /**
     * Map Account entity to AccountResponse DTO.
     *
     * @param account the account entity
     * @return the account response DTO
     */
    AccountResponse toResponse(Account account);
}

