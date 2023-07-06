package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class ContentFileComparator implements FileComparator {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    public static final int EOF = -1;

    private final long maxBytesToCompare;

    public ContentFileComparator() {
        this.maxBytesToCompare = Long.MAX_VALUE;
    }

    @Override
    public String isEqual(File source, File target) throws FileComparatorException {
        try (FileInputStream sourceIs = new FileInputStream(source);
             FileInputStream targetIs = new FileInputStream(target)) {
            if (maxBytesToCompare == Long.MAX_VALUE) {
                return IOUtils.contentEquals(sourceIs, targetIs)
                        ? null
                        : getId();
            } else {
                return contentEquals(sourceIs, targetIs)
                        ? null
                        : getId();

            }
        } catch (IOException e) {
            throw new FileComparatorException("could not compare content", e);
        }
    }

    @Override
    public String getId() {
        return (maxBytesToCompare == Long.MAX_VALUE)
                ? "ContentEquals"
                : "ContentEquals(" + maxBytesToCompare + ")";
    }

    /**
     * modified copy of org.apache.commons.io.IOUtils#contentEquals(java.io.InputStream, java.io.InputStream)
     */
    public boolean contentEquals(final InputStream input1, final InputStream input2) throws IOException {
        if (input1 == input2) {
            return true;
        }
        if (input1 == null || input2 == null) {
            return false;
        }

        // reuse one
        final byte[] array1 = new byte[DEFAULT_BUFFER_SIZE];
        final byte[] array2 = new byte[DEFAULT_BUFFER_SIZE];
        int pos1;
        int pos2;
        int count1;
        int count2;
        long contentIndex = 0;
        while (true) {
            pos1 = 0;
            pos2 = 0;
            for (int index = 0; index < DEFAULT_BUFFER_SIZE; index++) {
                if (pos1 == index) {
                    do {
                        count1 = input1.read(array1, pos1, DEFAULT_BUFFER_SIZE - pos1);
                    } while (count1 == 0);
                    if (count1 == EOF) {
                        return pos2 == index && input2.read() == EOF;
                    }
                    pos1 += count1;
                }
                if (pos2 == index) {
                    do {
                        count2 = input2.read(array2, pos2, DEFAULT_BUFFER_SIZE - pos2);
                    } while (count2 == 0);
                    if (count2 == EOF) {
                        return pos1 == index && input1.read() == EOF;
                    }
                    pos2 += count2;
                }
                if (array1[index] != array2[index]) {
                    return false;
                }

                if (contentIndex++ >= maxBytesToCompare) {
                    return true;
                }
            }
        }
    }

}
