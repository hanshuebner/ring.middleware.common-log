(defproject ring.middleware.common-log "0.1.0-SNAPSHOT"
  :description "A ring middleware to log requests in Common Log format"
  :url "https://github.com/hanshuebner/ring.middleware.common-log"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.9.0"]
                 [org.clojure/tools.logging "0.3.1"]]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.2.0"]
                                  [ring/ring-core "1.3.2"]]}})
