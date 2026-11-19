package com.fadikazoura.nextrep.common;
import com.fadikazoura.nextrep.account.IncorrectPasswordException;
import com.fadikazoura.nextrep.account.InvalidAvatarException;
import com.fadikazoura.nextrep.auth.InvalidCredentialsException;
import com.fadikazoura.nextrep.exercise.ExerciseNotFoundException;
import com.fadikazoura.nextrep.feedback.RatingNotFoundException;
import com.fadikazoura.nextrep.feedback.TooManyMessagesException;
import com.fadikazoura.nextrep.plan.PlanDayNotFoundException;
import com.fadikazoura.nextrep.plan.PlanNotFoundException;
import com.fadikazoura.nextrep.user.EmailAlreadyUsedException;
import com.fadikazoura.nextrep.user.InvalidPhoneNumberException;
import com.fadikazoura.nextrep.user.InvalidProfileException;
import com.fadikazoura.nextrep.user.ProfileNotFoundException;
import com.fadikazoura.nextrep.user.UserNotFoundException;
import com.fadikazoura.nextrep.user.UsernameAlreadyUsedException;
import com.fadikazoura.nextrep.workout.EmptyWorkoutException;
import com.fadikazoura.nextrep.workout.WorkoutAlreadyCompletedException;
import com.fadikazoura.nextrep.workout.WorkoutInProgressException;
import com.fadikazoura.nextrep.workout.WorkoutNotFoundException;
import com.fadikazoura.nextrep.workout.WorkoutSetNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
        return problem(HttpStatus.CONFLICT, "Email already in use", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "User not found", ex.getMessage());
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    ProblemDetail handleProfileNotFound(ProfileNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Profile not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidProfileException.class)
    ProblemDetail handleInvalidProfile(InvalidProfileException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid profile", ex.getMessage());
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    ProblemDetail handleExerciseNotFound(ExerciseNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Exercise not found", ex.getMessage());
    }

    @ExceptionHandler(PlanNotFoundException.class)
    ProblemDetail handlePlanNotFound(PlanNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Plan not found", ex.getMessage());
    }

    @ExceptionHandler(PlanDayNotFoundException.class)
    ProblemDetail handlePlanDayNotFound(PlanDayNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Plan day not found", ex.getMessage());
    }

    @ExceptionHandler(WorkoutNotFoundException.class)
    ProblemDetail handleWorkoutNotFound(WorkoutNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Workout not found", ex.getMessage());
    }

    @ExceptionHandler(WorkoutSetNotFoundException.class)
    ProblemDetail handleWorkoutSetNotFound(WorkoutSetNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Set not found", ex.getMessage());
    }

    @ExceptionHandler(WorkoutInProgressException.class)
    ProblemDetail handleWorkoutInProgress(WorkoutInProgressException ex) {
        return problem(HttpStatus.CONFLICT, "Workout in progress", ex.getMessage());
    }

    @ExceptionHandler(WorkoutAlreadyCompletedException.class)
    ProblemDetail handleWorkoutAlreadyCompleted(WorkoutAlreadyCompletedException ex) {
        return problem(HttpStatus.CONFLICT, "Workout already completed", ex.getMessage());
    }

    @ExceptionHandler(EmptyWorkoutException.class)
    ProblemDetail handleEmptyWorkout(EmptyWorkoutException ex) {
        return problem(HttpStatus.CONFLICT, "Empty workout", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body is missing or contains invalid values");
    }

    @ExceptionHandler(UsernameAlreadyUsedException.class)
    ProblemDetail handleUsernameAlreadyUsed(UsernameAlreadyUsedException ex) {
        return fieldProblem(HttpStatus.CONFLICT, "Username already taken", "username", ex.getMessage());
    }

    @ExceptionHandler(InvalidPhoneNumberException.class)
    ProblemDetail handleInvalidPhone(InvalidPhoneNumberException ex) {
        return fieldProblem(HttpStatus.BAD_REQUEST, "Invalid phone number", "phone", ex.getMessage());
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    ProblemDetail handleIncorrectPassword(IncorrectPasswordException ex) {
        return fieldProblem(HttpStatus.BAD_REQUEST, "Incorrect password", ex.field(), ex.getMessage());
    }

    @ExceptionHandler(InvalidAvatarException.class)
    ProblemDetail handleInvalidAvatar(InvalidAvatarException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid avatar", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", "The picture must be at most 200 KB");
    }

    @ExceptionHandler(RatingNotFoundException.class)
    ProblemDetail handleRatingNotFound(RatingNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not rated yet", ex.getMessage());
    }

    @ExceptionHandler(TooManyMessagesException.class)
    ProblemDetail handleTooManyMessages(TooManyMessagesException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many messages", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request", "Request validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    /** A problem about one input field, in the same "errors" shape as validation errors, so forms can show it. */
    private static ProblemDetail fieldProblem(HttpStatus status, String title, String field, String message) {
        ProblemDetail problem = problem(status, title, message);
        problem.setProperty("errors", Map.of(field, message));
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
