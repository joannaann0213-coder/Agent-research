-- The sweep closes the tasks that waited too long for the user's confirmation; this finds them.
CREATE INDEX delegated_task_unconfirmed ON delegated_task (expires_at) WHERE status = 'proposed';
