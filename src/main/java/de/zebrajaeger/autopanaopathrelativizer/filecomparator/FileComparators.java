package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

public class FileComparators {
    public static CompoundFileComparator newDefault1MB() {
        CompoundFileComparator fc = new CompoundFileComparator();
        fc.add(new FileSizeFileComparator());
        fc.add(new LastModifiedFileComparator());
        fc.add(new ContentFileComparator(1024 * 1024));
        return fc;
    }

    public static CompoundFileComparator newDefault10MB() {
        CompoundFileComparator fc = new CompoundFileComparator();
        fc.add(new FileSizeFileComparator());
        fc.add(new LastModifiedFileComparator());
        fc.add(new ContentFileComparator(1024 * 1024 * 10));
        return fc;
    }

    public static CompoundFileComparator newDefault() {
        CompoundFileComparator fc = new CompoundFileComparator();
        fc.add(new FileSizeFileComparator());
        fc.add(new LastModifiedFileComparator());
        fc.add(new ContentFileComparator());
        return fc;
    }
}
