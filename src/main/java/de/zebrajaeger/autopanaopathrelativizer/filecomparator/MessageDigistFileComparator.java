package de.zebrajaeger.autopanaopathrelativizer.filecomparator;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


/**
 * Maybe not useful? Especially compared to ContentFileComparator
 */
@AllArgsConstructor
@NoArgsConstructor
public class MessageDigistFileComparator implements FileComparator {
    public static final int BUFFER_LENGTH = 1024 * 8;
    public String algorithmName = "MD5";
    private long bytesToCheck = Long.MAX_VALUE;

    @Override
    public String isEqual(File source, File target) throws FileComparatorException {
        try {
            byte[] a = checksum(source);
            byte[] b = checksum(target);
            return Arrays.compare(a, b) == 0 ? null : getId();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new FileComparatorException(e);
        }
    }

    @Override
    public String getId() {
        return "ContentHash(" + algorithmName + ", " + bytesToCheck + ")";
    }

    private byte[] checksum(File f) throws IOException, NoSuchAlgorithmException {
        final MessageDigest algorithm = MessageDigest.getInstance(algorithmName);
        byte[] buffer = new byte[BUFFER_LENGTH];
        int l;
        long processed = 0;
        try (InputStream is = new FileInputStream(f)) {
            while ((l = is.read(buffer, 0, BUFFER_LENGTH)) != -1) {
                if (processed + l <= bytesToCheck) {
                    algorithm.update(buffer, 0, l);
                    processed += l;
                } else {
                    l = (int) (bytesToCheck - processed);
                    algorithm.update(buffer, 0, l);
                    break;
                }
            }
        }
        return algorithm.digest();
    }

}
