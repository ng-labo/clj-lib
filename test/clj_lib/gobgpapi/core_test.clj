(ns clj-lib.gobgpapi.core-test
  (:require [clojure.test :refer :all]
            [clojure.set :refer :all]
            [clojure.data.json :as json]
            [clj-lib.gobgpapi.core :refer :all]
            [clj-lib.gobgpapi.listpath :refer :all]
            [clj-lib.gobgpapi.unicast :refer :all]
            [clj-lib.gobgpapi.flowspec :refer :all]))

;(deftest a-test
;  (testing "FIXME, I fail."
;    (is (= 0 1))))
(def flowspec-ipv4-args
  { :origin     :igp
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

(def unicast-ipv4-args
  { :origin     0
    :ipver      4
    :nexthop    "1.2.1.2"
    :prefixes   {:prefix "1.2.3.0" :prefixlen 24}
    :communities [ "65432:666" ]
    :localpref  200
    :med 100
  })

(def unicast-ipv6-args
  { :origin     0
    :ipver      6
    :nexthop    "2001:db8:1:2::1"
    :prefixes   {:prefix "2001:db8:1:2::" :prefixlen 64}
  })

(deftest call-unicast-path
  (testing "create connection, call api addPath for ipv4/6-unicast"
    (let [gcli (clj-lib.gobgpapi.core/gobgp-client "localhost:50051")]
      (is (some? gcli))

      (is (some? (clj-lib.gobgpapi.unicast/add-path unicast-ipv4-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 4 :unicast)]
        (is (= 1 (count lp)))
        (is (some? (get (first lp) :prefix)))
        (is (some? (get (first lp) :paths)))
        (let [prefix (get (first lp) :prefix)
              paths (get (first lp) :paths)]
          (is (= "1.2.3.0/24" prefix))

          (let [path (get (first paths) :path)]
            (is (= 0 (get path :Origin)))
            (is (= 100 (get path :MED)))
            (is (= 200 (get path :LocalPref)))
            (is (= "1.2.1.2" (get path :NextHop)))
            (is (= "65432:666" (first (get path :Communities))))
      )))
      (is (some? (clj-lib.gobgpapi.unicast/delete-path unicast-ipv4-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 4 :unicast)]
        (is (= 0 (count lp))))

      (is (some? (clj-lib.gobgpapi.unicast/add-path unicast-ipv6-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 6 :unicast)]
        (is (= 1 (count lp))))
      (is (some? (clj-lib.gobgpapi.unicast/delete-path unicast-ipv6-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 6 :unicast)]
        (is (= 0 (count lp))))
)))

(deftest call-flowspec
  (testing "create connection, call api addPath for ipv4/6-flowspec"
    (let [gcli (clj-lib.gobgpapi.core/gobgp-client "localhost:50051")]
      (is (some? gcli))
      (is (some? (clj-lib.gobgpapi.flowspec/add-flowspec flowspec-ipv4-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 4 :flowspec)]
        (is (= 1 (count lp))))
      (is (some? (clj-lib.gobgpapi.flowspec/delete-flowspec flowspec-ipv4-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 4 :flowspec)]
        (is (= 0 (count lp))))
      (is (some? (clj-lib.gobgpapi.flowspec/add-flowspec flowspec-ipv6-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 6 :flowspec)]
        (is (= 1 (count lp))))
      (is (some? (clj-lib.gobgpapi.flowspec/delete-flowspec flowspec-ipv6-args gcli)))
      (let [lp (clj-lib.gobgpapi.listpath/list-path gcli 6 :flowspec)]
        (is (= 0 (count lp))))
)))
