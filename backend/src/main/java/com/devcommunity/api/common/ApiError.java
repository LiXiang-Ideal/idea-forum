package com.devcommunity.api.common;

import java.util.List;

public record ApiError(ErrorBody error) {
    public record ErrorBody(String code, String message, List<String> details) {
    }
}
