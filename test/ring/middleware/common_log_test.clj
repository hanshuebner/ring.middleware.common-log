(ns ring.middleware.common-log-test
  (:require [ring.mock.request :as mock]
            [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.test :refer :all]
            [ring.middleware.common-log :refer :all]))

(defn make-wrapped-app [response options]
  (wrap-with-common-log (fn [req]
                          (merge (response/response "") response))
                        options))

(deftest logger-test
  (testing "log file contents as expected"
    (let [app (make-wrapped-app {:status 200, :body "he"}
                                {:filename nil})
          [line-1 line-2] (string/split (with-out-str
                                          (app (mock/request :get "/foo"))
                                          (app (mock/request :put "/bar"))
                                          (force-flush)) #"\n")]
      (is (re-find #"^localhost - - \[../.../....:..:..:.. [-+]....\] \"GET /foo\" 200 -" line-1))
      (is (re-find #"^localhost - - \[../.../....:..:..:.. [-+]....\] \"PUT /bar\" 200 -" line-2)))))
