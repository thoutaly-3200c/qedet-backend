-- delivery_location_id was only receiving workflow state. Its foreign key
-- points to organization_locations, which remains unchanged for other domains.
ALTER TABLE receiving_requests
    DROP FOREIGN KEY fk_receiving_request_delivery_location,
    DROP COLUMN delivery_location_id;
