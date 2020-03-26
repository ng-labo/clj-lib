(ns clj-lib.gobgpapi.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clj-lib.gobgpapi.core :refer :all]
            [clj-lib.gobgpapi.flowspec :refer :all]))

;(deftest a-test
;  (testing "FIXME, I fail."
;    (is (= 0 1))))
(def flowspec-ipv4-args
  { :origin     0
    :rate       0
    :ipver      4
    :prefixes   [ {:type 1 :prefix "1.2.3.4" :prefixlen 32}
                  {:type 2 :prefix "5.6.7.8" :prefixlen 32} ]
    :conditions [ {:type 3 :conditions [{:op 1 :value 17}]}
                  {:type 4 :conditions [{:op 1 :value 1900}{:op 1 :value 11211}]} ]
  })

(def flowspec-ipv6-args
  { :origin     0
    :rate       0
    :ipver      6
    :prefixes   [ {:type 1 :prefix "2001:db8:cafe::1" :prefixlen 128}
                  {:type 2 :prefix "2001:db8:cafe::2" :prefixlen 128} ]
    :conditions [ {:type 3 :conditions [{:op 1 :value 17}]}
                  {:type 4 :conditions [{:op 1 :value 1900}{:op 1 :value 11211}]} ]
  })

(deftest call-flowspec
  (testing "create connection, call api addPath for ipv4-flowspec"
    (let [gcli (clj-lib.gobgpapi.core/gobgp-client "localhost:50051")]
      (is (some? gcli))
      (is (some? (clj-lib.gobgpapi.flowspec/add-flowspec flowspec-ipv4-args gcli)))
      (is (some? (clj-lib.gobgpapi.flowspec/delete-flowspec flowspec-ipv4-args gcli)))
      (is (some? (clj-lib.gobgpapi.flowspec/add-flowspec flowspec-ipv6-args gcli)))
      (is (some? (clj-lib.gobgpapi.flowspec/delete-flowspec flowspec-ipv6-args gcli)))
)))
