package com.hajime.media;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/** Future S3 adapter owns authorization, content type/size validation, and signed URLs. */
public interface MediaPort {
  record Upload(UUID mediaId, URI uploadUrl, java.time.Instant expiresAt) {}

  Upload prepareUpload(UUID ownerUserId, String contentType, long bytes, Duration lifetime);

  URI readUrl(UUID ownerUserId, UUID mediaId, Duration lifetime);
}
