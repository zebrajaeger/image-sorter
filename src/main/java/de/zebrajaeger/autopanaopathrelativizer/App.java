package de.zebrajaeger.autopanaopathrelativizer;

import de.zebrajaeger.autopanaopathrelativizer.copy.CopyResult;
import de.zebrajaeger.autopanaopathrelativizer.copy.CopyTask;
import de.zebrajaeger.autopanaopathrelativizer.filecomparator.FileComparators;
import de.zebrajaeger.autopanaopathrelativizer.filetime.FileTimeReader;
import de.zebrajaeger.autopanaopathrelativizer.settings.MainSettings;
import de.zebrajaeger.autopanaopathrelativizer.settings.SettingStore;
import dev.dirs.ProjectDirectories;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

@Slf4j
public class App {

    public static void main(String[] args) throws ParseException {
        log.info("START");

//        // TODO remove me:
//        args = new String[]{
//                "-s", "C:/tmp/pano-silpion/a-1/1E6A9126.JPG",
//                "-t", "C:/tmp/_target",
//                "-f", "F",
//                "-x",
//                "-d"
//        };

        ProjectDirectories myProjDirs = ProjectDirectories.from("de", "zebrajaeger", "image-sorter");
        final File settingsFile = new File(myProjDirs.configDir, "main.json");
        log.info("Settingsfile: '{}'", settingsFile.getAbsolutePath());
        final MainSettings mainSettings = new MainSettings();
        final SettingStore<MainSettings> settingStore = new SettingStore<>(settingsFile, mainSettings, 10);
        settingStore.load();
        log.info("Default-settings: {}", mainSettings);

        Options options = new Options();
        options.addOption("u", "user-interface", false, "Show GUI");
        options.addOption("h", "help", false, "Print help");
        options.addOption("s", "source", true, "Source: file or folder");
        options.addOption("t", "target-root", true, "Target Root");
        options.addOption("f", "folder-struct-template", true, "'F', 'H' or take a look at java.text.SimpleDateFormat");
        options.addOption("n", "no-folder-struct", false, "confirm that I want to put everything in one folder. Only used for -f");
        options.addOption("x", "set-as-default", false, "Set the current configuration as default");
        options.addOption("d", "dry-run", false, "Just see, what would happen");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Image Sorter", options);
            return;
        }

        if (cmd.hasOption("u")) {
            new Ui().setVisible(true);
        } else {
            // target
            String targetRoot = mainSettings.getTargetRoot();
            if (cmd.hasOption("t")) {
                targetRoot = cmd.getOptionValue("t");
            }
            if (StringUtils.isBlank(targetRoot)) {
                log.error("Error: No target-root in settings or as commandline option");
                return;
            }

            // source
            if (!cmd.hasOption("s") || StringUtils.isBlank(cmd.getOptionValue("s"))) {
                log.error("Error: No source as commandline option");
                return;
            }
            File source = new File(cmd.getOptionValue("s"));
            if (!source.exists()) {
                log.error("Error: Source does not exist");
                return;
            }

            // folder-struct-template
            String folderStructTemplateString = mainSettings.getFolderStructTemplate();
            if (cmd.hasOption("s")) {
                final String v = cmd.getOptionValue("f");
                if ("F".equals(v)) {
                    folderStructTemplateString = "yyyy-MM-dd";
                } else if ("H".equals(v)) {
                    folderStructTemplateString = "yyyy%sMM%sdd";
                } else {
                    folderStructTemplateString = v;
                }
            }

            if (StringUtils.isBlank(folderStructTemplateString)) {
                if (cmd.hasOption("n")) {
                    folderStructTemplateString = "";
                } else {
                    log.error("Error: No target-root in settings or as commandline option");
                    return;
                }
            }

            DateTimeFormatter folderStructTemplate;
            try {
                folderStructTemplate = DateTimeFormatter.ofPattern(folderStructTemplateString);
            } catch (IllegalArgumentException e) {
                log.error("Error: Folder structure Template is invalid: '{}'", folderStructTemplateString);
                return;
            }

            // set as default
            if (cmd.hasOption("x")) {
                mainSettings.setTargetRoot(targetRoot);
                mainSettings.setFolderStructTemplate(folderStructTemplateString);
                log.info("Settings: {}", mainSettings);
                settingStore.saveImmediately();
            }

            // dry run
            boolean dryRun = cmd.hasOption("d");

            // run it
            final CopyTask copyTask = new CopyTask(
                    source,
                    new File(targetRoot),
                    FileComparators.newDefault10MB(),
                    folderStructTemplate,
                    new FileTimeReader(),
                    dryRun);
            Future<CopyResult> result = null;
            try {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                result = executorService.submit(copyTask);
                executorService.shutdown();
                while (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.info("Running...");
                }
            } catch (Exception e) {
                log.info("Stopped by Exception", e);
            }
            if (result != null && result.isDone()) {
                try {
                    log.info("Result: {}", result.get());
                } catch (InterruptedException | ExecutionException ignore) {
                }
            }

            log.info("Done");
        }

//        File targetRoot = new File("C:/tmp/_target");
//        File source = new File("C:\\tmp\\pano-silpion\\a-1\\1E6A9126.JPG");
////        final DateTimeFormatter pathPattern = PATTERN_FLAT;
//        final DateTimeFormatter pathPattern = HIERARCHICAL_PATTERN;
//
//
//        final FileTimeReader fileTimeReader = new FileTimeReader();
//
//        source = source.getAbsoluteFile().getCanonicalFile();
//        targetRoot = targetRoot.getAbsoluteFile().getCanonicalFile();
//        Path targetRootPath = targetRoot.toPath();
//        FileUtils.forceMkdir(targetRoot);
//
//        CompoundFileComparator fc = createFileComparator();
//
//        final FileWalker fileWalker = new FileWalker();
//        fileWalker.scan(source, sourceFile -> {
//            final List<Timestamp> fileTimes = fileTimeReader.getFileTime(sourceFile);
//            if (!fileTimes.isEmpty()) {
//                final Timestamp timestamp = fileTimes.get(0);
//                final String relPath = pathPattern.format(timestamp.localDateTime());
//                final Path targetPath = targetRootPath.resolve(relPath);
//                final File targetFile = new File(targetPath.toFile(), sourceFile.getName());
//                System.out.println(targetFile);
//
//                try {
//                    if (!targetFile.exists()) {
//                        FileUtils.copyFile(sourceFile, targetFile);
//                    } else {
//                        final String equal = fc.isEqual(sourceFile, targetFile);
//                        if (equal != null) {
//                            log.error("File already exists but is not equal({}): '{}' -> '{}'", equal, sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
//                        }
//                    }
//                } catch (IOException e) {
//                    log.error("Could not copy file: '{}' -> '{}'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), e);
//                } catch (FileComparatorException e) {
//                    log.error("Could not compare file: '{}' -> '{}'", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), e);
//                }
//            }
//        });

//        File f = new File("1E6A1660.CR2");
//        File f = new File("C:\\tmp\\pano-silpion\\a2.config.json");
//        fileTimeReader.getFileTime(f).forEach(System.out::println);


//        if (args.length == 0) {
//            args = new String[]{"-u"};
//        }
//
//        Options options = new Options();
//        options.addOption("u", "user-interface", false, "Show GUI");
//        options.addOption("h", "help", false, "Print help");
//
//        CommandLineParser parser = new DefaultParser();
//        CommandLine cmd = parser.parse(options, args);
//
//        if (cmd.hasOption("h")) {
//            HelpFormatter formatter = new HelpFormatter();
//            formatter.printHelp("Image Sorter", options);
//            return;
//        }
//
//        if (cmd.hasOption("u")) {
//            new Ui().setVisible(true);
//        } else {
//            // TODO
//        }
    }
}
