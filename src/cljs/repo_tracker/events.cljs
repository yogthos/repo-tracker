(ns repo-tracker.events
  (:require
    [re-frame.core :as rf]
    [ajax.core :as ajax]))

(rf/reg-sub
  ::loading?
  (fn [db _]
    (:loading? db)))

(rf/reg-sub
  ::repos
  (fn [db _]
    (:repos db)))

(rf/reg-event-db
  ::set-repos
  (fn [db [_ {repos :repos}]]
    (assoc db :loading? false
              :repos (reduce
                       (fn [m repo]
                         (assoc m (-> repo :meta :id) repo))
                       {}
                       repos))))

(rf/reg-sub
  ::selected-repo
  (fn [{:keys [selected-repo-id] :as db} _]
    (-> (get-in db [:repos selected-repo-id :data]) reverse not-empty)))

(rf/reg-event-db
  ::set-selected-repo
  (fn [db [_ repo-id]]
    (assoc db :selected-repo-id repo-id)))

(rf/reg-event-db
  ::set-repo
  (fn [db [_ {repo :repo}]]
    (-> db
        (assoc :loading? false)
        (assoc-in [:repos (-> repo :meta :id)] repo))))

(defn with-ajax-defaults [opts]
  (merge
    {:format          (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-failure      [:common/set-error]}
    opts))

(rf/reg-event-fx
  ::add-repo
  (fn [{db :db} [_ repo-id]]
    {:db         (assoc db :loading? true)
     :http-xhrio (with-ajax-defaults
                   {:method     :post
                    :uri        "/api/repos/add"
                    :params     {:repo-id repo-id}
                    :on-success [::set-repo]})}))

(rf/reg-event-db
  ::set-repo-as-seen
  (fn [db [_ {repo-id :repo-id}]]
    (assoc-in db [:repos repo-id :meta :seen?] true)))

(rf/reg-event-fx
  ::mark-repo-as-seen
  (fn [_ [_ repo-id]]
    {:http-xhrio (with-ajax-defaults
                   {:method     :post
                    :uri        "/api/repos/mark-as-seen"
                    :params     {:repo-id repo-id}
                    :on-success [::set-repo-as-seen]})}))

(rf/reg-event-db
  ::set-repo-removed
  (fn [db [_ {repo-id :repo-id}]]
    (update db :repos dissoc repo-id)))

(rf/reg-event-fx
  ::remove-repo
  (fn [_ [_ repo-id]]
    {:http-xhrio (with-ajax-defaults
                   {:method     :post
                    :uri        "/api/repos/remove"
                    :params     {:repo-id repo-id}
                    :on-success [::set-repo-removed]})}))

(rf/reg-event-fx
  ::refresh-repos
  (fn [{db :db} _]
    {:db         (assoc db :loading? true)
     :http-xhrio (with-ajax-defaults
                   {:method     :get
                    :uri        "/api/repos/refresh"
                    :on-success [::set-repos]})}))

(rf/reg-event-fx
  ::fetch-repos
  (fn [{db :db} _]
    {:db         (assoc db :loading? true)
     :http-xhrio (with-ajax-defaults
                   {:method     :get
                    :uri        "/api/repos/user"
                    :on-success [::set-repos]})}))

(rf/reg-sub
  ::user
  (fn [db _]
    (:user db)))

(rf/reg-event-fx
  ::handle-login
  (fn [db [_ {user :user}]]
    {:db       (assoc db :user user)
     :dispatch [::fetch-repos]}))

(rf/reg-event-fx
  ::login
  (fn [_ [_ user-id]]
    {:http-xhrio (with-ajax-defaults
                   {:method     :post
                    :uri        "/api/login"
                    :params     {:user-id user-id}
                    :on-success [::handle-login]})}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ {{message :message} :response}]]
    (assoc db :loading? false :common/error message)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))
