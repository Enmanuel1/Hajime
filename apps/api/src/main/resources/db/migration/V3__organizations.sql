CREATE TABLE organization(id uuid PRIMARY KEY, name varchar(160) NOT NULL);
CREATE TABLE organization_martial_art(organization_id uuid NOT NULL REFERENCES organization, martial_art_id uuid NOT NULL REFERENCES martial_art, PRIMARY KEY(organization_id,martial_art_id));
CREATE TABLE organization_membership(id uuid PRIMARY KEY, organization_id uuid NOT NULL REFERENCES organization, user_id uuid NOT NULL REFERENCES app_user, role varchar(30) NOT NULL CHECK(role IN ('PRACTITIONER','INSTRUCTOR','SENSEI','DIRECTOR','ADMIN')), active boolean NOT NULL DEFAULT true, UNIQUE(organization_id,user_id,role));
CREATE INDEX membership_scope ON organization_membership(organization_id,user_id) WHERE active;
ALTER TABLE curriculum ADD CONSTRAINT curriculum_organization_fk FOREIGN KEY(organization_id) REFERENCES organization;
