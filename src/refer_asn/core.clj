(ns refer-asn.core
  (:require [clj-time.coerce :as tmc])
  (:require [clj-time.local :as local-tm])
  (:use [refer-asn.ipv4])
  (:use [refer-asn.ipv6])
)

;
; one of example for gobgp-api
; using IP2Location[TM] LITE IP-COUNTRY Database
; aims to generate maxminddb format file
;

(def test-file-ipv4 "./IP2LOCATION-LITE-DB1.CSV")
(def test-file-ipv6 "./IP2LOCATION-LITE-DB1.IPV6.CSV")

(defn- nowepoch
  []
  (tmc/to-long (local-tm/local-now)))

(defn batch-run-ipv4
  [blocking-stub ip-cc-file output-csv]
  (let [start-time (nowepoch)
        cc-index   (create-index-ipv4 ip-cc-file)]
    (println "create-index " (- (nowepoch) start-time))
    (let [start-time  (nowepoch)
          all-prefixs (get-all-prefixs-ipv4 blocking-stub)]
      (println "get-all-prefixes " (- (nowepoch) start-time))
      (let [start-time (nowepoch)
            ip-asn-cc  (lookup-cc-ipv4 cc-index all-prefixs)]
        (println "count ip-asn-cc" (count ip-asn-cc))
        (println "lookup-cc " (- (nowepoch) start-time))
        (time (write-list-data-ipv4 ip-asn-cc output-csv))))))

(defn batch-run-ipv6
  [blocking-stub ip-cc-file output-csv]
  (let [start-time (nowepoch)
        cc-index   (create-index-ipv6 ip-cc-file)]
    (println "create-index " (- (nowepoch) start-time) "msec")
    (let [start-time  (nowepoch)
          all-prefixs (get-all-prefixs-ipv6 blocking-stub)]
      (println "get-all-prefixes " (- (nowepoch) start-time) "msec")
      (let [start-time (nowepoch)
            ip-asn-cc  (lookup-cc-ipv6 cc-index all-prefixs)]
        (println "count ip-asn-cc" (count ip-asn-cc))
        (println "lookup-cc " (- (nowepoch) start-time) "msec")
        (time (write-list-data-ipv6 ip-asn-cc output-csv))))))
