CREATE TABLE organization_invitation (
 id uuid PRIMARY KEY,
 organization_id uuid NOT NULL REFERENCES organization,
 recipient_user_id uuid NOT NULL REFERENCES app_user,
 role varchar(30) NOT NULL CHECK(role IN ('PRACTITIONER','INSTRUCTOR','SENSEI')),
 created_by uuid NOT NULL REFERENCES app_user,
 created_at timestamptz NOT NULL,
 expires_at timestamptz NOT NULL CHECK(expires_at > created_at),
 status varchar(20) NOT NULL CHECK(status IN ('PENDING','ACCEPTED','REVOKED'))
);
CREATE INDEX invitation_recipient ON organization_invitation(recipient_user_id,expires_at) WHERE status='PENDING';
CREATE INDEX invitation_organization ON organization_invitation(organization_id,recipient_user_id) WHERE status='PENDING';
