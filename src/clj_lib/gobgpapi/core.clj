(ns clj-lib.gobgpapi.core
  ;(:require [clj-lib.gobgpapi.flowspec :as gobgpapi-flowspec])
  (:import [io.grpc ManagedChannelBuilder])
  (:import [gobgpapi GobgpApiGrpc])
)

(defn -main []
  (println "Hello clj-lib.gobgpapi.core"))

(def attr-origin
  (hash-map :igp 0 :egp 1 :incomplete 2 0 0 1 1 2 2))

(def known-community
  "https://www.iana.org/assignments/bgp-well-known-communities/bgp-well-known-communities.xhtml"
  (hash-map 
    :GRACEFUL_SHUTDOWN          0xFFFF0000 ;[RFC8326]
    :ACCEPT_OWN                 0xFFFF0001 ;[RFC7611]
    :ROUTE_FILTER_TRANSLATED_v4 0xFFFF0002 ;[draft-l3vpn-legacy-rtc]
    :ROUTE_FILTER_v4            0xFFFF0003 ;[draft-l3vpn-legacy-rtc]
    :ROUTE_FILTER_TRANSLATED_v6 0xFFFF0004 ;[draft-l3vpn-legacy-rtc]
    :ROUTE_FILTER_v6            0xFFFF0005 ;[draft-l3vpn-legacy-rtc]
    :LLGR_STALE                 0xFFFF0006 ;[draft-uttaro-idr-bgp-persistence]
    :NO_LLGR                    0xFFFF0007 ;[draft-uttaro-idr-bgp-persistence]
    :accept-own-nexthop         0xFFFF0008 ;[draft-agrewal-idr-accept-own-nexthop]
    :BLACKHOLE                  0xFFFF029A ;[RFC7999]
    :NO_EXPORT                  0xFFFFFF01 ;[RFC1997]
    :NO_ADVERTISE               0xFFFFFF02 ;[RFC1997]
    :NO_EXPORT_SUBCONFED        0xFFFFFF03 ;[RFC1997]
    :NOPEER                     0xFFFFFF04 ;[RFC3765]
  ))

(defn tr-communities
  [args]
  (map
    (fn [v]
      (cond (some? (get  known-community v))
            (get  known-community v) 
            (= 2 (and (= (type v) java.lang.String)
                      (count (re-seq #"[0-9]+" v)))) 
            (let [n (re-seq #"[0-9]+" v)]
              (+ (bit-shift-left (Integer/parseInt (nth n 0)) 16)
                 (Integer/parseInt (nth n 1))))
       :else v)) args))

(defn gobgp-client
  "obtains GobgpApiBlockingStub instance.
   target `host/ip-address:port` ex. `127.0.0.1:50051`"
  [target]
  (GobgpApiGrpc/newBlockingStub
    (.build
      (doto (ManagedChannelBuilder/forTarget target)
            (.usePlaintext)))))
