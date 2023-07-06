package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;

public class LastModifiedFileComparator implements FileComparator {

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy.MM.dd hh:mm:ss.SSS");

    @Override
    public String isEqual(File source, File target) {
        return source.lastModified() == target.lastModified()
                ? null :
                "LastModified("
                        + DATE_FORMAT.format(source.lastModified())
                        + ","
                        + DATE_FORMAT.format(target.lastModified())
                        + ")";
    }

    @Override
    public String getId() {
        return "LastModified";
    }
}
