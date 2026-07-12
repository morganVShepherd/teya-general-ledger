package moo.interview.teya.mapper;

import moo.interview.teya.dto.response.TransactionResponse;
import moo.interview.teya.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Transaction entity to DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {
    
    /**
     * Map Transaction entity to TransactionResponse DTO.
     * 
     * @param transaction the transaction entity
     * @return the transaction response DTO
     */
    TransactionResponse toResponse(Transaction transaction);
}

