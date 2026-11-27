-- People write the way they talk: a sentence, then the rest of it, then a correction. An agent that
-- answers each message by itself answers the first one before it knows what the user means. So a
-- turn of a conversation takes everything the user has written that no turn has yet, and when the
-- user writes again before the answer is there, the turn is ended and gives its messages back: the
-- next turn answers all of them, in one reply.
--
-- For that a message says for itself whether it waits for a turn, and which turn has it.
--
-- announced: the message is to get a turn. Not so the user's answer to a question that a turn
--            waits for: that goes to the turn that asked.
-- taken_by:  the run whose turn has the message; NULL while it waits.
ALTER TABLE message
    ADD COLUMN announced boolean NOT NULL DEFAULT false,
    ADD COLUMN taken_by  text    COLLATE "C";

CREATE INDEX message_waiting ON message (app_id, user_id, conversation_id, seq) WHERE announced AND taken_by IS NULL;

-- How many replies in a row the user's new messages have ended on the line. Only so many are
-- ended, so that an answer comes whatever the user does; the count starts again when one has.
ALTER TABLE thread ADD COLUMN superseded integer NOT NULL DEFAULT 0;

-- The line no longer notes how far the user's messages were posted and taken: each message says it.
ALTER TABLE thread
    DROP COLUMN posted_seq,
    DROP COLUMN posted_at,
    DROP COLUMN taken_seq;
