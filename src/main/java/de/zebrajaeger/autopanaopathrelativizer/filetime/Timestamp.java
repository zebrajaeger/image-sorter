package de.zebrajaeger.autopanaopathrelativizer.filetime;

import java.time.LocalDateTime;

public record Timestamp(TimestampType type, LocalDateTime localDateTime) {

}
