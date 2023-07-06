package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

import java.io.File;

public interface FileComparator {
    String isEqual(File source, File target) throws FileComparatorException;

    String getId();
}
