(ns ring.middleware.common-log
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-time.core :as time]
            [clj-time.format :as time-format]))

(defonce flush-interval 100)

(defonce writer (agent {}))

(defonce file-timestamp-format (time-format/formatter "yyyyMMddHHmmss"))

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
  (let [{:keys [stream max-file-size filename]} agent]
    (cond
      (and max-file-size
           (<= max-file-size (.length (io/file filename))))
      (do (when stream
            (.close stream)
            (archive-current-file (:filename agent)))
          (open-stream agent))

      (not stream)
      (open-stream agent)

      true agent)))

(defn- flush-lines [agent p]
  (let [agent (if (:lines agent)
                (let [agent (ensure-stream agent)]
                  (binding [*out* (:stream agent)]
                    (doseq [line (:lines agent)]
                      (println line))
                    (flush))
                  (dissoc agent :lines))
                agent)]
    (when p
      (deliver p true))
    agent))

(defn- log [agent line]
  (if (empty? (:filename agent))
    (do (println line)
        agent)
    (if (:lines agent)
      (update-in agent [:lines] conj line)
      (do (future (Thread/sleep flush-interval)
                  (send writer flush-lines nil))
          (assoc agent
                 :lines [line])))))

(defn- reopen [agent options]
  (when (:stream agent)
    (flush-lines agent nil)
    (.close (:stream agent)))
  options)

(def line-timestamp-format (time-format/formatter "dd/MMM/YYYY:HH:mm:ss Z"))

(defn force-flush []
  (let [p (promise)]
    (send writer flush-lines p)
    (deref p)))

(defn wrap-with-common-log [handler {:keys [filename max-file-size] :as options}]
  (when (and filename
             max-file-size
             (= (.lastIndexOf filename ".") -1))
    (throw (Exception. ":filename must contain a dot character if :max-file-size is set so that the log timestamp can be filled in when reopening log file")))
  (send writer reopen options)
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

