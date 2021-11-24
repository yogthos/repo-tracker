(ns repo-tracker.repos
  (:require
    [repo-tracker.db.core :as db]
    [clj-http.client :as client]))

(defn load-repo [repo-id]
  (let [{:keys [status body]} (client/get (str "https://api.github.com/repos/" repo-id "/releases")
                                          {:as               :json
                                           :throw-exceptions false})]
    (when (= 200 status) body)))

(defn add-repo [user-id repo-id]
  (when-let [data (load-repo repo-id)]
    (let [repo {:meta {:id    repo-id
                       :seen? false}
                :data data}]
      (db/add-repo! user-id repo)
      repo)))

(defn remove-repo [user-id repo-id]
  (db/remove-repo*! {:user user-id :id repo-id}))

(defn set-repo-seen-status [user-id repo-id status]
  (db/update-repo-seen-status! user-id repo-id status))

(defn mark-repo-seen [user-id repo-id]
  (set-repo-seen-status user-id repo-id true))

(defn mark-repo-unseen [user-id repo-id]
  (set-repo-seen-status user-id repo-id false))

(defn parse-date [date-str]
  (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss") date-str))

(defn refresh-repos [user-id]
  (doseq [{data :data {id :id} :meta :as repo} (db/get-repos user-id)]
    (let [new-data (load-repo id)]
      (db/add-repo! user-id
                    (assoc
                      (cond-> repo
                              (.after (->> new-data last :created_at parse-date)
                                      (-> data last :created_at parse-date))
                              (assoc-in [:meta :seen?] false))
                      :data new-data))))
  (db/get-repos user-id))

(defn fetch-user-repos [user-id]
  (db/get-repos user-id))
