package com.erp.core.platform.error;

import com.erp.core.catalog.application.CatalogExceptions.Conflict;
import com.erp.core.catalog.application.CatalogExceptions.ResourceNotFound;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFound.class)
    ProblemDetail notFound(ResourceNotFound exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler({Conflict.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(Exception exception) {
        String detail = exception instanceof Conflict ? exception.getMessage() : "A unique or relational constraint was violated";
        return problem(HttpStatus.CONFLICT, "Conflict", detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(), (first, ignored) -> first));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            org.springframework.data.mapping.PropertyReferenceException.class})
    ProblemDetail invalidRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request parameters or body are invalid");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
