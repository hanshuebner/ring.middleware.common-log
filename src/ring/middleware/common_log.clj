(ns ring.middleware.common-log
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-time.core :as time]
            [clj-time.format :as time-format]))

(defonce flush-interval 100)

(def writer (agent {:filename "access.log"
                    :max-file-size (* 10 1000 1000)
                    :rotate? true}))

(defonce file-timestamp-format (time-format/formatter "yyyyMMddHHmm"))

(defn- open-stream [agent]
  (assoc agent
         :stream (io/writer (:filename agent) :append true)))

(defn- archive-current-file [filename]
  (let [timestamp (time-format/unparse file-timestamp-format (time/now))
        archive-filename (string/replace filename #"^(.*)[.](.*)$" (str  "$1-" timestamp ".$2"))]
    (if (.renameTo (io/file filename) (io/file archive-filename))
      (log/info "renamed log file" filename "to" archive-filename)
      (log/error "could not rename log file" filename "to" archive-filename))))

(defn- ensure-stream [agent]
  (let [stream (:stream agent)]
    (if (and stream
             (or (not (:rotate? agent))
                 (< (.length (io/file (:filename agent))) (:max-file-size agent))))
      agent
      (do (when stream
            (.close stream)
            (archive-current-file (:filename agent)))
          (open-stream agent)))))

(defn- flush-lines [agent]
  (if (:lines agent)
    (let [agent (ensure-stream agent)]
      (binding [*out* (:stream agent)]
        (doseq [line (:lines agent)]
          (println line))
        (flush))
      (dissoc agent :lines))
    agent))

(defn- log [agent line]
  (if (:lines agent)
    (update-in agent [:lines] conj line)
    (do (future (Thread/sleep flush-interval)
                (send writer flush-lines))
        (assoc agent
               :lines [line]))))

(defn- reopen [agent options]
  (when (:stream agent)
    (flush-lines agent)
    (.close (:stream agent)))
  options)

(def line-timestamp-format (time-format/formatter "dd/MMM/YYYY:HH:mm:ss Z"))

(def defaults {})

(defn wrap-with-common-log [handler & {:keys [filename max-file-size] :as options}]
  (when (and filename
             max-file-size
             (= (.lastIndexOf filename ".") -1))
    (throw (Exception. "log file name must contain a dot character if max-file-size is set so that the log timestamp can be filled in when reopening log file")))
  (send writer reopen (merge defaults options) options)
  (fn [request]
    (let [response (handler request)
          {:keys [remote-addr request-method uri query-string] :or {remote-addr "-"}} request
          {:keys [status headers]} response]
      (send-off writer log (print-str remote-addr
                                      "-"
                                      "-"
                                      (str "[" (time-format/unparse line-timestamp-format (time/now)) "]")
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

