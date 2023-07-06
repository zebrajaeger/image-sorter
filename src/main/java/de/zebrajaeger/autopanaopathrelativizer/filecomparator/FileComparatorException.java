package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

public class FileComparatorException extends Exception {

    public FileComparatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileComparatorException(Throwable cause) {
        super(cause);
    }
}
