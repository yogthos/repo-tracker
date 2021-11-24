(ns repo-tracker.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[repo-tracker started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[repo-tracker has shut down successfully]=-"))
   :middleware identity})
