-- :name add-repo*! :! :n
-- :doc adds new repo data
insert into repos
(id, user, seen, data)
values (:id, :user, :seen?, :data)
on conflict(user, id) do update set
    seen=excluded.seen,
    data=excluded.data

-- :name remove-repo*! :! :n
-- :doc removes the repo with the given id
delete from repos where user = :user and id = :id

-- :name update-repo-seen-status*! :! :n
-- :doc updates existing repo data
UPDATE repos
SET seen = :seen?
WHERE id = :id and user = :user

-- :name get-repos* :? :*
-- :doc retrieves a repo data for the user
select * from repos where user = :user
