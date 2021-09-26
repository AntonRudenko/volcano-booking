package com.upgrade.volcano.dto;

import com.upgrade.volcano.exception.ErrorCode;

public record ErrorResponse(String message, ErrorCode code) {
}
