package com.hajime.attendance;

import com.hajime.shared.RecordSource;
import java.time.Instant;
import java.util.UUID;

/** Reserved public concept; attendance writes and correction workflows are not implemented. */
public record AttendanceRecord(
    UUID id,
    UUID organizationId,
    UUID practitionerProfileId,
    UUID classId,
    Instant occurredAt,
    RecordSource source) {}
