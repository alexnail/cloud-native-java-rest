package demo;

import java.util.Optional;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@ControllerAdvice(annotations = RestController.class)
public class CustomerControllerAdvice {

    // <1>
    private final MediaType vndErrorMediaType = MediaType.parseMediaType("application/vnd.error");

    // <2>
    @ExceptionHandler(CustomerNotFoundException.class)
    ResponseEntity<VndErrors> notFoundException(CustomerNotFoundException e) {
        return error(e, HttpStatus.NOT_FOUND, e.getCustomerId() + "");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<VndErrors> assertionException(IllegalArgumentException ex) {
        return error(ex, HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
    }

    // <3>
    private <E extends Exception> ResponseEntity<VndErrors> error(E error, HttpStatus httpStatus, String logref) {
        String msg = Optional.of(error.getMessage())
                .orElse(error.getClass().getSimpleName());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(vndErrorMediaType);
        return new ResponseEntity<>(new VndErrors(logref, msg), httpHeaders, httpStatus);
    }
}