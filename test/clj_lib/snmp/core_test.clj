(ns clj-lib.snmp.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clj-lib.snmp.core :refer :all]
))

;(deftest a-test
;  (testing "FIXME, I fail."
;    (is (= 0 1))))

;(defn timeout-example []
;  (println (doall (snmpget-v2c "127.0.0.2" "public" '("1.3.6.1.2.1.1.3.0" "1.3.6.1.2.1.1.4.0")))))
;
;(defn invalid-oid-example []
;  (println (doall (snmpget-v2c "127.0.0.1" "public" '("1.3.6.1.2.1.1.3.1" "1.3.6.1.2.1.1.4.0")))))

(deftest call-snmpget-v2c
  (testing "call snmpget-v2c"
    (let [snmp-result (clj-lib.snmp.core/snmpget-v2c "127.0.0.1" "public" '("1.3.6.1.2.1.1.3.0" "1.3.6.1.2.1.1.4.0") )]
      (is (= 2 (count snmp-result)))
      (is (= clojure.lang.PersistentArrayMap (type (first snmp-result))))
      (is (= "1.3.6.1.2.1.1.3.0" (first (first (first snmp-result)))))
    ))

  (testing "call snmpget-v2c timeout"
    (let [snmp-result (clj-lib.snmp.core/snmpget-v2c "127.0.0.2" "public" '("1.3.6.1.2.1.1.3.0" "1.3.6.1.2.1.1.4.0") )]
      (is (= 0 (count snmp-result)))
    ))

  (testing "call snmpbulkwalk-v2c"
    (let [snmp-result (clj-lib.snmp.core/snmpbulkwalk-v2c "localhost" "public" "1.3.6.1.2.1.31.1.1.1.1")]
      ;ans example {"1.3.6.1.2.1.31.1.1.1.1.1" "lo", "1.3.6.1.2.1.31.1.1.1.1.2" "ens3", "1.3.6.1.2.1.31.1.1.1.1.3" "docker0"}
      (is (= clojure.lang.PersistentArrayMap (type snmp-result)))
      (is (every? true? (map #(clojure.string/starts-with? % "1.3.6.1.2.1.31.1.1.1.1.") (keys snmp-result))))
    ))

  (testing "call snmpbulkwalk-v2c timeout"
    (let [snmp-result (clj-lib.snmp.core/snmpbulkwalk-v2c "127.0.0.2" "public" "1.3.6.1.2.1.31.1.1.1.1")]
      (is (= 0 (count snmp-result)))
    ))
)
