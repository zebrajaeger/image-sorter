package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CompoundFileComparator implements FileComparator {
    private final List<FileComparator> comparatorList = new ArrayList<>();

    public boolean add(FileComparator fileComparator) {
        return comparatorList.add(fileComparator);
    }

    @Override
    public String isEqual(File source, File target) throws FileComparatorException {
        for (FileComparator fc : comparatorList) {
            final String r = fc.isEqual(source, target);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    @Override
    public String getId() {
        return null;
    }
}
