(ns repo-tracker.app
  (:require [repo-tracker.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
