-- A conversation's line takes one turn at a time, and on a main agent's line many turns are work in
-- the background. A user who writes must not wait behind such a turn: it gives way, and starts over
-- when the user has their answer.
--
-- The line notes the position of the latest message of the user that is to get a turn of its own,
-- and how far its turns have taken the user's messages. A message beyond that waits.
ALTER TABLE thread
    ADD COLUMN posted_seq bigint      NOT NULL DEFAULT 0,
    ADD COLUMN posted_at  timestamptz,
    ADD COLUMN taken_seq  bigint      NOT NULL DEFAULT 0;

-- How often a turn has given way. A turn gives way only so often, so that it cannot be kept from
-- its work for ever.
ALTER TABLE turn ADD COLUMN yields integer NOT NULL DEFAULT 0;
