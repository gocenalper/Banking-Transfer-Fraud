package com.bank.account.adapter.in.web;

import com.bank.account.application.AccountNotFoundException;
import com.bank.account.domain.AccountClosedException;
import com.bank.account.domain.AccountNotEmptyException;
import com.bank.account.domain.DuplicateTransferAttemptException;
import com.bank.account.domain.InsufficientBalanceException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into RFC 9457 problem responses. The distinction that matters:
 * 400 = the request itself is malformed; 409 = the request was fine but clashes with the
 * current state of the world. Stack traces never leak to the wire.
 */
@RestControllerAdvice
class AccountExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail notFound(AccountNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({InsufficientBalanceException.class, AccountClosedException.class,
            AccountNotEmptyException.class, DuplicateTransferAttemptException.class})
    ProblemDetail stateConflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail contention(OptimisticLockingFailureException e) {
        // retries are exhausted by the time this surfaces — tell the client to try again
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The account is busy, please retry the operation");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
