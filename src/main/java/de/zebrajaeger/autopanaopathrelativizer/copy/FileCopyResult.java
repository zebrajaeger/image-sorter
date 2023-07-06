package de.zebrajaeger.autopanaopathrelativizer.copy;

import java.io.File;

public record FileCopyResult(File source, File target, FileCopyResultType type, String msg) {
    public FileCopyResult(File source, File target, FileCopyResultType type) {
        this(source, target, type, null);
    }
}
