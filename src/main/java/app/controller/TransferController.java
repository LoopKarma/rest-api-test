package app.controller;

import app.dto.TransferRequest;
import app.dto.TransferRequestValidationResult;
import app.exception.TransferAlreadyCreated;
import app.processor.TransferCreateProcessor;
import app.processor.TransferRequestValidator;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import spark.Response;
import spark.Route;

import java.util.UUID;

@Slf4j
public class TransferController {
    TransferRequestValidator requestValidator;
    TransferCreateProcessor createProcessor;

    public TransferController(TransferRequestValidator requestValidator, TransferCreateProcessor createProcessor) {
        this.requestValidator = requestValidator;
        this.createProcessor = createProcessor;
    }

    public Route createTransfer() {
        return ((request, response) -> {
            Gson gson = new Gson();
            TransferRequest transferRequest = gson.fromJson(request.body(), TransferRequest.class);
            if (transferRequest == null || transferRequest.isEmpty()) {
                response.status(HttpStatus.BAD_REQUEST_400);
                return "";
            }
            TransferRequestValidationResult validationResult = requestValidator.validateRequest(transferRequest);
            if (validationResult.isValid()) {
                return processCorrectRequest(response, transferRequest);
            }
            return processIncorrectRequest(response, transferRequest, validationResult);

        });
    }

    private Object processIncorrectRequest(
            Response response,
            TransferRequest transferRequest,
            TransferRequestValidationResult validationResult) {
        if (validationResult.getException() instanceof TransferAlreadyCreated) {
            log.warn("Transfer with UUID:'{}' tried to be created again", transferRequest.getTransferIdentifier());
            response.status(HttpStatus.ACCEPTED_202);
            return new SuccessResponse(transferRequest.getTransferIdentifier());
        }
        response.status(HttpStatus.BAD_REQUEST_400);
        return new ErrorResponse(validationResult.getException().getClass().getSimpleName());
    }

    private Object processCorrectRequest(Response response, TransferRequest transferRequest) {
        createProcessor.createTransfer(transferRequest);
        response.status(HttpStatus.ACCEPTED_202);
        return new SuccessResponse(transferRequest.getTransferIdentifier());
    }

    private static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class SuccessResponse {
        private UUID uuid;

        public SuccessResponse(UUID uuid) {
            this.uuid = uuid;
        }
    }
}
