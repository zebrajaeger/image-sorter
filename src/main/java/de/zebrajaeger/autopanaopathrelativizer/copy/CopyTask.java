package de.zebrajaeger.autopanaopathrelativizer.copy;

import de.zebrajaeger.autopanaopathrelativizer.filecomparator.FileComparator;
import de.zebrajaeger.autopanaopathrelativizer.filecomparator.FileComparatorException;
import de.zebrajaeger.autopanaopathrelativizer.filetime.FileTimeReader;
import de.zebrajaeger.autopanaopathrelativizer.filetime.Timestamp;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@AllArgsConstructor
public class CopyTask implements Callable<CopyResult> {
    private final File source;
    private final File target;
    private final FileComparator fc;
    private final DateTimeFormatter pathPattern;
    private final FileTimeReader fileTimeReader;
    private final boolean dryRun;

    @Override
    public CopyResult call() throws Exception {

        File source = this.source.getAbsoluteFile().getCanonicalFile();
        File targetRoot = this.target.getAbsoluteFile().getCanonicalFile();
        Path targetRootPath = targetRoot.toPath();

        if (!targetRoot.exists()) {
            if (dryRun) {
                log.info("DRY-RUN: create directory: '{}'", targetRoot.getAbsolutePath());
            } else {
                log.info("create directory: '{}'", targetRoot.getAbsolutePath());
                FileUtils.forceMkdir(targetRoot);
            }
        }

        final CopyResult copyResult = new CopyResult();
        final FileWalker fileWalker = new FileWalker();
        fileWalker.scan(source, sourceFile -> {
            final List<Timestamp> fileTimes = fileTimeReader.getFileTime(sourceFile);
            if (!fileTimes.isEmpty()) {
                final Timestamp timestamp = fileTimes.get(0);
                final String relPath = pathPattern.format(timestamp.localDateTime());
                final Path targetPath = targetRootPath.resolve(relPath);
                final File targetFile = new File(targetPath.toFile(), sourceFile.getName());

                if (!targetFile.exists()) {
                    try {
                        if (dryRun) {
                            log.info("DRY-RUN: copy '{}' -> '{}'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
                        } else {
                            log.info("copy '{}' -> '{}'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
                            FileUtils.copyFile(sourceFile, targetFile);
                        }
                        copyResult.add(sourceFile, targetFile, FileCopyResultType.COPIED);
                    } catch (IOException e) {
                        log.error("Could not copy file: '{}' -> '{}'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), e);
                        copyResult.add(sourceFile, targetFile, FileCopyResultType.FAILED_TO_COPY);
                    }
                } else {
                    try {
                        final String equal = fc.isEqual(sourceFile, targetFile);
                        if (equal != null) {
                            log.error("File already exists but is not equal({}): '{}' -> '{}'", equal, sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
                            copyResult.add(sourceFile, targetFile, FileCopyResultType.NOT_EQUAL,equal);
                        } else {
                            log.info("File already exists: '{}' -> '{}'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
                            copyResult.add(sourceFile, targetFile, FileCopyResultType.SKIPPED);
                        }
                    } catch (FileComparatorException e) {
                        log.error("Could not compare file: '{}' -> '{}'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), e);
                        copyResult.add(sourceFile, targetFile, FileCopyResultType.FAILED_TO_COMPARE);
                    }
                }
            }
        });

        return copyResult;
    }
}
