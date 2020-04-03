(ns clj-lib.snmp.core
  "snmpget -v2c
   snmpbulkwalk -v2c"
  (:require [clojure.tools.logging :as logger])
  (:import [org.snmp4j Snmp PDU CommunityTarget])
  (:import [org.snmp4j.mp SnmpConstants])
  (:import [org.snmp4j.smi OID VariableBinding OctetString GenericAddress])
  (:import [org.snmp4j.transport DefaultUdpTransportMapping])
  (:import [org.snmp4j.util DefaultPDUFactory TreeEvent TreeUtils])
  (:import [java.io IOException])
)

(defn- build-target-v2c
  [host community]
  (doto (CommunityTarget.)
    (.setCommunity (OctetString. community))
    (.setAddress (GenericAddress/parse (format "udp:%s/%d" host 161)))
    (.setVersion SnmpConstants/version2c)
    (.setTimeout 2000)
    (.setRetries 0)
))

(defn- build-pdu
  [oids]
  (doto (PDU.)
    (.setType PDU/GETBULK)
    (.addAll (into-array (map #(VariableBinding. (OID. (str %))) oids)))
))

(defn snmpget-v2c [host community oid-list]
  "user=> (snmpget-v2c `192.168.0.100` `public` '(`1.3.6.1.2.1.31.1.1.1.6.1`)
   ({1.3.6.1.2.1.31.1.1.1.6.1 16438153754})"
  (let [snmp (Snmp. (DefaultUdpTransportMapping.))]
    (try
      (. snmp listen)
      (let [event (. snmp get (build-pdu oid-list) (build-target-v2c host community))]
        (let [response (. event getResponse)]
          (when (and response (== (. response getType) PDU/RESPONSE))
            (map (fn [a] {(str (. a getOid)) (str (. a getVariable))}) (. response getVariableBindings)))))
    (catch IOException e nil) ; timeout,..
    (catch Exception e nil)   ; wrong host,..
    (finally (. snmp close))
)))

(defn- p2
  [& more]
  (.write *out* (str (clojure.string/join " " more) "\n"))
  (.flush *out*))

(defn- decodeEvent
  "return { oid(dropped prefix) value, ... }, when isError return nil"
  [tree-event host oid]
  (cond (. tree-event isError) nil
        :else
        (loop [var-bind (. tree-event getVariableBindings) ret {}]
          (if (first var-bind)
               (recur (next var-bind)
                      (assoc ret (str (. (first var-bind) getOid)) (str (. (first var-bind) getVariable))))
               ret))))

(defn- decodeEvents
  [events host oid]
  (loop [ev events ret {}]                                                                                                  
    (if (first ev)
        (recur (next ev)
               (conj ret (decodeEvent (first ev) host oid)))
               ret)))

(defn snmpbulkwalk-v2c
  [host community oid]
  (let [transport (DefaultUdpTransportMapping.)]
    (try
      (. transport listen)
      (let [snmp (Snmp. transport)]
        (let [events (. (TreeUtils. snmp (DefaultPDUFactory.))
                     (getSubtree (build-target-v2c host community) (OID. oid)))]
          (. snmp close)
          (cond  (= 0 (count events)) { oid nil }
           :else (decodeEvents events host oid))))
    (catch IOException e { oid nil })
    (catch Exception e { oid nil }) 
)))

(defn- get-deref-list
  [thread-results]
  (map deref (vals thread-results))
)

(defn- get-deref-map
  [thread-results]
  (let [ keys-result (keys thread-results)
         derefed-result (map deref (vals thread-results))]
    (into {} (map vector keys-result derefed-result))
))

;;; (run-snmp-get '("192.168.0.100", "192.168.0.102") "public" '("1.3.6.1.2.1.1.3.0" "1.3.6.1.2.1.1.4.0" ))
(defn run-snmp-get
  [host-list community oid-list]
  (loop [ threads {} host host-list ]
    (if (first host)
        (recur (assoc threads
                      (first host)
                      (future (snmpget-v2c (first host) community oid-list)))
               (next host))
        (get-deref-map threads)
)))

;;; (run-snmp-bulkwalk '("192.168.0.100" "192.168.0.102") "public" "1.3.6.1.2.1.31.1.1.1.1")
(defn run-snmp-bulkwalk
  [ host-list community oid ]
  (loop [ threads {} host host-list ]
    (if (first host)
        (recur (assoc threads
                      (first host)
                      (future (snmpbulkwalk-v2c (first host) community oid)))
                      ;(future (test-fn (first host) community oid)))
               (next host))
        (get-deref-map threads)
)))

