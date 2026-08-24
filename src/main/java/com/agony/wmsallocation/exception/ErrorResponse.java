package com.agony.wmsallocation.exception;

import java.time.LocalDateTime;

public record ErrorResponse(int httpStatusCode,
                            String message,
                            String errorCode,
                            LocalDateTime timestamp,
                            String path) {

}
