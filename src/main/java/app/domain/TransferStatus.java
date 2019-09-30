package app.domain;

public enum TransferStatus {
    NEW(0),
    PROCESSING(1),
    SUCCESS(2),
    DISCARD(3);

    private short status;

    TransferStatus(int status) {
        this.status = (short) status;
    }

    public short getStatus() {
        return (short) status;
    }
}
