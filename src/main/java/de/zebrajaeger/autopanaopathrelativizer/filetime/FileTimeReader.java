package de.zebrajaeger.autopanaopathrelativizer.filetime;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FileTimeReader {
    // TODO:
    //      [MP4] Creation Time - Fri Dec 16 11:05:49 CET 2022
    //      [MP4] Modification Time - Fri Dec 16 11:05:49 CET 2022
    private final static Pattern DEFAULT_DATETIME_PATTERN = Pattern.compile("\\s*(\\d{4}):(\\d{2}):(\\d{2})\\s+(\\d{2}):(\\d{2}):(\\d{2}).*");
    private final static Pattern GPS_DATE_PATTERN = Pattern.compile("\\s*(\\d{4}):(\\d{2}):(\\d{2}).*");
    private final static Pattern GPS_TIME_PATTERN = Pattern.compile("\\s*(\\d{2}):(\\d{2}):(\\d{2})(?:,(\\d{1,3}))?.*");

    public final static List<TimestampType> DEFAULT_ORDER = List.of(new TimestampType[]{
            TimestampType.IFD0,
            TimestampType.SubIFD_ORIGINAL,
            TimestampType.SubIFD_DIGITIZED,
            TimestampType.GPS,
            TimestampType.FS_EARLIEST
    });

    private <T extends Directory> List<Tag> find(Metadata metadata, Class<T> clazz, int tag) {
        List<Tag> result = new ArrayList<>();
        metadata.getDirectoriesOfType(clazz).forEach(d -> {
            d.getTags().forEach(t -> {
                if (t.getTagType() == tag) {
                    result.add(t);
                }
            });
        });
        return result;
    }

    private Optional<LocalDateTime> parse(String dateTime) {
        // 2018:07:29 09:37:58
        final Matcher matcher = DEFAULT_DATETIME_PATTERN.matcher(dateTime);
        if (matcher.matches()) {
            try {
                int year = Integer.parseUnsignedInt(matcher.group(1));
                int month = Integer.parseUnsignedInt(matcher.group(2));
                int day = Integer.parseUnsignedInt(matcher.group(3));
                int hour = Integer.parseUnsignedInt(matcher.group(4));
                int minute = Integer.parseUnsignedInt(matcher.group(5));
                int second = Integer.parseUnsignedInt(matcher.group(6));
                return Optional.of(LocalDateTime.of(year, month, day, hour, minute, second));
            } catch (NumberFormatException ignore) {
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> parseGpsDate(String date) {
        // 2018:07:29
        final Matcher matcher = GPS_DATE_PATTERN.matcher(date);
        if (matcher.matches()) {
            try {
                int year = Integer.parseUnsignedInt(matcher.group(1));
                int month = Integer.parseUnsignedInt(matcher.group(2));
                int day = Integer.parseUnsignedInt(matcher.group(3));
                return Optional.of(LocalDate.of(year, month, day));
            } catch (NumberFormatException ignore) {
            }
        }
        return Optional.empty();
    }

    private Optional<LocalTime> parseGpsTime(String time) {
        // 09:37:45,897 UTC
        final Matcher matcher = GPS_TIME_PATTERN.matcher(time);
        if (matcher.matches()) {
            try {
                int hour = Integer.parseUnsignedInt(matcher.group(1));
                int minute = Integer.parseUnsignedInt(matcher.group(2));
                int second = Integer.parseUnsignedInt(matcher.group(3));
                int ms = (matcher.groupCount() >= 4)
                        ? Integer.parseUnsignedInt(matcher.group(4))
                        : 0;
                return Optional.of(LocalTime.of(hour, minute, second, ms * 1000000));
            } catch (NumberFormatException ignore) {
            }
        }
        return Optional.empty();
    }


    private Optional<LocalDateTime> parseGps(String date, String time) {
        final Optional<LocalDate> localDate = parseGpsDate(date);
        final Optional<LocalTime> localTime = parseGpsTime(time);
        if (localDate.isPresent() && localTime.isPresent()) {
            return Optional.of(LocalDateTime.of(localDate.get(), localTime.get()));
        }
        return Optional.empty();
    }

    private Optional<LocalDateTime> lastModified(File f) {
        try {
            BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
            final FileTime fileTime = attr.lastModifiedTime();

            return Optional.of(LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault()));

        } catch (IOException e) {
            log.warn("Could not access last modified time", e);
        }
        return Optional.empty();
    }

    private List<Timestamp> tagToList(List<Tag> source, Function<String, Optional<LocalDateTime>> mapper, TimestampType timestampType) {
        return source.stream()
                .map(Tag::getDescription)
                .map(mapper)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ldt -> new Timestamp(timestampType, ldt))
                .toList();
    }

    public Optional<Metadata> readMetadata(File f) {
        try {
            return Optional.ofNullable(ImageMetadataReader.readMetadata(f));
        } catch (ImageProcessingException | IOException e) {
            log.info("Could not read metadata from file " + f.getAbsolutePath());
        }
        return Optional.empty();
    }

    public List<Timestamp> getFileTime(File f) {
        return getFileTime(f, DEFAULT_ORDER);
    }

    public List<Timestamp> getFileTime(File f, List<TimestampType> order) {
        final ArrayList<Timestamp> result = new ArrayList<>();
        Optional<Metadata> metadata = readMetadata(f);
        for (TimestampType t : order) {
            switch (t) {
                case GPS -> {
                    metadata.ifPresent(md -> {
                        // [GPS] GPS Time-Stamp - 09:37:45,897 UTC / 7/ 0x0007
                        final Optional<Tag> gpsTime = find(md, GpsDirectory.class, 7).stream().findFirst();

                        // [GPS] GPS Date Stamp - 2018:07:29 / 29/ 0x001d
                        final Optional<Tag> gpsDate = find(md, GpsDirectory.class, 29).stream().findFirst();

                        if (gpsTime.isPresent() && gpsDate.isPresent()) {
                            final Optional<LocalDateTime> gpsDT = parseGps(gpsDate.get().getDescription(), gpsTime.get().getDescription());
                            gpsDT
                                    .map(ldt -> new Timestamp(TimestampType.GPS, ldt))
                                    .ifPresent(result::add);
                        }
                    });
                }
                case IFD0 -> metadata.ifPresent(md -> {
                    // [Exif IFD0] Date/Time - 2018:07:29 09:37:58 / 306/ 0x0132
                    result.addAll(tagToList(find(md, ExifIFD0Directory.class, 306), this::parse, TimestampType.IFD0));
                });
                case SubIFD_ORIGINAL -> {
                    metadata.ifPresent(md -> {
                        // [Exif SubIFD] Date/Time Original - 2018:07:29 09:37:58 / 36867/ 0x9003
                        result.addAll(tagToList(find(md, ExifSubIFDDirectory.class, 36867), this::parse, TimestampType.SubIFD_ORIGINAL));

                    });
                }
                case SubIFD_DIGITIZED -> {
                    metadata.ifPresent(md -> {
                        // [Exif SubIFD] Date/Time Digitized - 2018:07:29 09:37:58 / 36868/ 0x9004
                        result.addAll(tagToList(find(md, ExifSubIFDDirectory.class, 36868), this::parse, TimestampType.SubIFD_DIGITIZED));

                    });
                }
                case FS_LAST_MODIFIED -> {
                    lastModified(f)
                            .map(ldt -> new Timestamp(TimestampType.FS_LAST_MODIFIED, ldt))
                            .ifPresent(result::add);
                }
                case FS_EARLIEST -> {
                    earliestFileTime(f)
                            .map(ldt -> new Timestamp(TimestampType.FS_EARLIEST, ldt))
                            .ifPresent(result::add);
                }
            }
        }

        return result;
    }

    /**
     * Get the youngest of 'last modified', 'last access' and 'creation time'.<br>
     * Zero values are ignored.<br>
     * If all values are zero, Optional.empty() returns.
     */
    private Optional<LocalDateTime> earliestFileTime(File f) {
        BasicFileAttributes attr = null;
        try {
            attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            log.error("Could not read file attributes of file: '{}'", f.getAbsolutePath(), e);
        }

        final FileTime fileTimeLastModified = attr.lastModifiedTime();
        long r = fileTimeLastModified != null ? fileTimeLastModified.toMillis() : 0;

        final FileTime fileTimeLastAccessed = attr.lastAccessTime();
        final long lastAccessed = fileTimeLastAccessed != null ? fileTimeLastAccessed.toMillis() : 0;
        if (lastAccessed != 0 && (r==0 ||r > lastAccessed)) r = lastAccessed;

        final FileTime fileTimeCreation = attr.creationTime();
        final long created = fileTimeCreation != null ? fileTimeCreation.toMillis() : 0;
        if (created != 0 && (r==0|| r > created)) r = created;

        if (r != 0) {
            final LocalDateTime localDateTime = Instant.ofEpochMilli(r).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return Optional.of(localDateTime);
        }
        return Optional.empty();
    }

    public void dumpMetaData(Metadata metadata) {
        for (Directory d : metadata.getDirectories()) {
            System.out.println(d);
            for (Tag t : d.getTags()) {
                System.out.println("  " + t);
            }

        }
    }
}
