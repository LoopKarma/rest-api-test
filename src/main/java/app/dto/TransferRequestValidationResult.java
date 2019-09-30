package app.dto;

import lombok.Data;

@Data
public class TransferRequestValidationResult {
    private boolean isValid = false;
    private Exception exception;

    public TransferRequestValidationResult(boolean isValid) {
        this.isValid = isValid;
    }

    public TransferRequestValidationResult(Exception exception) {
        this.exception = exception;
    }
}
