(ns repo-tracker.middleware
  (:require
    [repo-tracker.env :refer [defaults]]
    [clojure.tools.logging :as log]
    [repo-tracker.layout :refer [error-page]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [repo-tracker.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [repo-tracker.config :refer [env]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.util.http-response :refer :all]))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (internal-server-error! {:message (str "Error handling the request: " (.getMessage t))})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title  "Invalid anti-forgery token"})}))


(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-internal-error))
