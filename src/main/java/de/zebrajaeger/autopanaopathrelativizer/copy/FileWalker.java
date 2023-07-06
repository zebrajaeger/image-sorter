package de.zebrajaeger.autopanaopathrelativizer.copy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FileWalker {
    public void scan(File file, Consumer<File> fileConsumer) {
        if (file.isFile()) {
            fileConsumer.accept(file);
        }

        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files == null) {
                return;
            }

            List<File> directories = new ArrayList<>();
            // process files direct, but collect folders
            for (File f : files) {
                if (f.isFile()) {
                    scan(f, fileConsumer);
                }
                if (f.isDirectory()) {
                    directories.add(f);
                }
            }

            // after all files have been processed, we care about the directories
            directories.forEach(d -> scan(d, fileConsumer));
        }
    }
}
