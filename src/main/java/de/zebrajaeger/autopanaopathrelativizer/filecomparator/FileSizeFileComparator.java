package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

import java.io.File;

public class FileSizeFileComparator implements FileComparator{
    @Override
    public String isEqual(File source, File target) {
        return source.length() == target.length()
                ? null
                : "FileSize(" + source.length() + ", " + target.length()+")";
    }

    @Override
    public String getId() {
        return "FileSize";
    }
}
