package com.homelibrary.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class ErrorDetails {

    private LocalDateTime timeStamp;
    private HttpStatusCode httpStatusCode;
    private String message;
}
