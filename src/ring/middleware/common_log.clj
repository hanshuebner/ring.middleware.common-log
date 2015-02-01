(ns ring.middleware.common-log
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [clj-time.format :as time-format]))

(defonce flush-interval 100)

(defonce writer (agent {}))

(defonce logfile-name (atom nil))

(defn open-stream [agent]
  (assoc agent
         :stream (io/writer @logfile-name :append true)))

(defn ensure-stream [agent]
  (if (:stream agent)
    agent
    (open-stream agent)))

(defn flush-lines [agent]
  (let [agent (ensure-stream agent)]
    (binding [*out* (:stream agent)]
      (doseq [line (:lines agent)]
        (println line))
      (flush))
    (dissoc agent :lines)))

(defn log [agent line]
  (if (:lines agent)
    (update-in agent [:lines] conj line)
    (do (future (Thread/sleep flush-interval)
                (send writer flush-lines))
        (assoc agent
               :lines [line]))))

(def timestamp-format (time-format/formatter "dd/MMM/YYYY:HH:mm:ss Z"))

(defn wrap-with-common-log [handler & {:keys [filename]}]
  (reset! logfile-name filename)
  (fn [request]
    (let [response (handler request)
          {:keys [remote-addr request-method uri query-string] :or {remote-addr "-"}} request
          {:keys [status headers]} response]
      (send-off writer log (print-str remote-addr
                                      "-"
                                      "-"
                                      (str "[" (time-format/unparse timestamp-format (time/now)) "]")
                                      (str \"
                                           (string/upper-case (name request-method))
                                           " "
                                           uri
                                           (when query-string
                                             (str \? query-string))
                                           \")
                                      status
                                      (or (headers "Content-Length") "-")))
      response)))

