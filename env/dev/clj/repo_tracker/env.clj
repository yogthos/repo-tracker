(ns repo-tracker.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [repo-tracker.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[repo-tracker started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[repo-tracker has shut down successfully]=-"))
   :middleware wrap-dev})
