(ns repo-tracker.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [repo-tracker.middleware.formats :as formats]
    [ring.util.http-response :refer :all]
    [repo-tracker.repos :as repos]
    [clojure.tools.logging :as log]))

(defn service-routes []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc  true
        :swagger {:info {:title       "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url    "/api/swagger.json"
              :config {:validator-url nil}})}]]
   ["/repos"
    ["/user"
     {:get {:summary   "fetch repos for the user"
            :responses {200 {:body {:repos coll?}}}
            :handler   (fn [{{{user-id :user-id} :user} :session}]
                         (when user-id
                           (ok {:repos (repos/fetch-user-repos user-id)})))}}]

    ["/refresh"
     {:get {:summary   "refresh watched repos"
            :responses {200 {:body {:repos coll?}}}
            :handler   (fn [{{{user-id :user-id} :user} :session}]
                         (when user-id
                           (ok {:repos (repos/refresh-repos user-id)})))}}]

    ["/add"
     {:post {:summary    "add repo to watched repos"
             :parameters {:body {:repo-id string?}}
             :responses  {200 {:body {:repo map?}}
                          404 {:body {:message string?}}}
             :handler    (fn [{{{:keys [repo-id]} :body}  :parameters
                               {{user-id :user-id} :user} :session}]
                           (when user-id
                             (if-let [repo (repos/add-repo user-id repo-id)]
                               (ok {:repo repo})
                               (not-found {:message (str "no repository found with id: " repo-id)}))))}}]

    ["/remove"
     {:post {:summary    "remove repo from watched repos"
             :parameters {:body {:repo-id string?}}
             :responses  {200 {:body {:repo-id string?}}}
             :handler    (fn [{{{:keys [repo-id]} :body}  :parameters
                               {{user-id :user-id} :user} :session}]
                           (when user-id
                             (repos/remove-repo user-id repo-id)
                             (ok {:repo-id repo-id})))}}]

    ["/mark-as-seen"
     {:post {:summary    "mark repo as seen to remove notification"
             :parameters {:body {:repo-id string?}}
             :responses  {200 {:body {:repo-id string?}}}
             :handler    (fn [{{{:keys [repo-id]} :body}  :parameters
                               {{user-id :user-id} :user} :session}]
                           (when user-id
                             (repos/mark-repo-seen user-id repo-id)
                             (ok {:repo-id repo-id})))}}]]

   ["/login"
    {:post {:summary    "set the user in session"
            :parameters {:body {:user-id string?}}
            :responses  {200 {:body {:user map?}}}
            :handler    (fn [{{{:keys [user-id]} :body} :parameters
                              session                   :session}]
                          (let [user {:user-id user-id}]
                            (-> (ok {:user user})
                                (assoc :session (assoc session :user user)))))}}]])
