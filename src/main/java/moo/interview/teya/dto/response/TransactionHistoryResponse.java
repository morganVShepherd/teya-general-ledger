package moo.interview.teya.dto.response;

import java.util.List;

public record TransactionHistoryResponse(
        List<TransactionResponse> transactions,
        PageInfo pageInfo
) {
}

