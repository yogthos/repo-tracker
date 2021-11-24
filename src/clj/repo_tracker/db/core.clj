(ns repo-tracker.db.core
  (:require
    [clojure.edn :as edn]
    [clojure.set :refer [rename-keys]]
    [next.jdbc.date-time]
    [next.jdbc.result-set]
    [conman.core :as conman]
    [mount.core :refer [defstate]]
    [repo-tracker.config :refer [env]]))

(defstate db
  :start (conman/connect! {:jdbc-url (env :database-url)})
  :stop (conman/disconnect! db))

(conman/bind-connection db "sql/queries.sql")

(defn add-repo! [user-id {{:keys [id seen?]} :meta data :data}]
  (add-repo*!
    {:id           id
     :user         user-id
     :seen?        (if seen? 1 0)
     :data         (pr-str data)}))

(defn get-repos [user-id]
  (map (fn [{:keys [id new seen data]}]
         {:meta {:id           id
                 :seen?        (not (zero? seen))}
          :data (->> (edn/read-string data))})
       (get-repos* {:user user-id})))

(defn update-repo-seen-status! [user-id repo-id seen?]
  (update-repo-seen-status*! {:id repo-id :user user-id :seen? (if seen? 1 0)}))
