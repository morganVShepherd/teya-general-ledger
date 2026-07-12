package moo.interview.teya.dto.response;

public record PageInfo(
        int pageSize,
        String nextCursor,
        boolean hasNextPage
) {
}

