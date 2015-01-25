(ns ring.middleware.common-log
  (:require [clojure.string :as string]
            [clj-time.core :as time]
            [clj-time.format :as time-format]))

(def logfile-format (time-format/formatter "dd/MMM/YYYY:HH:mm:ss Z"))

(defn wrap-with-common-log [handler]
  (fn [request]
    (let [response (handler request)
          {:keys [remote-addr request-method uri query-string] :or {remote-addr "-"}} request
          {:keys [status headers]} response]
      (println remote-addr
               "-"
               "-"
               (str "[" (time-format/unparse logfile-format (time/now)) "]")
               (str \"
                    (string/upper-case (name request-method))
                    " "
                    uri
                    (when query-string
                      (str \? query-string))
                    \")
               status
               (or (headers "Content-Length") "-"))
      response)))

