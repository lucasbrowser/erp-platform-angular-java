package com.erp.core.platform.identity;

import java.util.List;

public record CurrentIdentityResponse(String userId, List<String> authorities) {
    public CurrentIdentityResponse {
        authorities = List.copyOf(authorities);
    }
}
