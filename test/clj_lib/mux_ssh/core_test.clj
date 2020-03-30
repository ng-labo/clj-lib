(ns clj-lib.mux-ssh.core-test
  (:require [clojure.test :refer :all]
            [clojure.set :refer :all]
            [clojure.data.json :as json]
            [clj-lib.mux-ssh.core :refer :all]
))

(deftest call-sshdriver
  (testing "test just fail-action"
      (is (= false (check-host "not.exist.host")))

))
