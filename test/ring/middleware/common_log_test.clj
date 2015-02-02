(ns ring.middleware.common-log-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [ring.util.response :as response]
            [ring.middleware.common-log :refer :all])
  (:import [java.io File]))

(defn- make-wrapped-app [response options]
  (wrap-with-common-log (fn [req]
                          (merge (response/response "") response))
                        options))

(defn- create-temp-file []
  (let [file (File/createTempFile "common-log-test" ".log")]
    (.deleteOnExit file)
    (.getCanonicalPath file)))

(deftest logger-test
  (testing "log file contents as expected"
    (let [app (make-wrapped-app {:status 201, :body "he"}
                                {:filename nil})
          [line-1 line-2] (string/split (with-out-str
                                          (app (mock/request :get "/foo"))
                                          (app (mock/request :put "/bar"))
                                          (force-flush))
                                        #"\n")]
      (is (re-find #"^localhost - - \[../.../....:..:..:.. [-+]....\] \"GET /foo\" 201 -" line-1))
      (is (re-find #"^localhost - - \[../.../....:..:..:.. [-+]....\] \"PUT /bar\" 201 -" line-2))))

  (testing "log file is archived when size limit is reached"
    ;; FIXME: This test leaves qthe archived file behind.
    (let [temp-file (create-temp-file)
          app (make-wrapped-app {:status 200 :body "hey ho"}
                                {:filename temp-file
                                 :max-file-size 1})]
      (app (mock/request :get "/foo"))
      (force-flush)
      (app (mock/request :get "/foo"))
      (force-flush)
      (is (= (count (line-seq (io/reader temp-file))) 1)))))
