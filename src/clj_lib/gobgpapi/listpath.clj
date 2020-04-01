(ns clj-lib.gobgpapi.listpath
  (:require [clojure.set :refer :all]
            [clj-lib.gobgpapi.core :as gobgpapi])
  (:import [com.google.protobuf Any])
  (:import [gobgpapi Gobgp$GetBgpRequest
                     Gobgp$ListPathRequest
                     Gobgp$Path
                     Gobgp$Family Gobgp$Family$Afi Gobgp$Family$Safi])
  (:import [gobgpapi Attribute$FlowSpecIPPrefix
                     Attribute$FlowSpecComponent
                     Attribute$FlowSpecComponentItem
                     Attribute$FlowSpecNLRI
                     Attribute$TrafficRateExtended
                     Attribute$CommunitiesAttribute
                     Attribute$ExtendedCommunitiesAttribute
                     Attribute$MpReachNLRIAttribute
                     Attribute$OriginAttribute
                     Attribute$NextHopAttribute
                     Attribute$MultiExitDiscAttribute
                     Attribute$LocalPrefAttribute
                     Attribute$IPAddressPrefix ])
)

(defn- build-family 
  "ver  : 4 or 6
   safi : :unicast or :flowspec"
  [ver safi]
  (.build
    (doto (Gobgp$Family/newBuilder)
          (.setAfi (cond (= ver 4) (Gobgp$Family$Afi/AFI_IP)
                         (= ver 6) (Gobgp$Family$Afi/AFI_IP6)))
          (.setSafi (cond (= safi :flowspec) Gobgp$Family$Safi/SAFI_FLOW_SPEC_UNICAST
                          (= safi :unicast)  Gobgp$Family$Safi/SAFI_UNICAST)))))

(defn- tr-ipaddressprefix
  [attr] ; yet unpacked
  (let [ipaddr (.unpack attr Attribute$IPAddressPrefix)]
    { :prefix (.getPrefix ipaddr) :prefixlen (.getPrefixLen ipaddr) }))

(defn- dissect-Nlris
  [nlri-list]
  (reduce into (map (fn [nlri]
                      (assert (= (type nlri) com.google.protobuf.Any))
                      (cond (.is nlri Attribute$IPAddressPrefix)
                            {:IPAddresPrefix (tr-ipaddressprefix nlri) }
                     )) nlri-list)))

(defn- dissect-MpReachNLRI
  [attr]
  { :family    { :afi  (.toString (.getAfi (.getFamily attr)))
                 :safi (.toString (.getSafi (.getFamily attr))) }
    :next_hops (.getNextHopsList attr)
    :nlris     (dissect-Nlris (.getNlrisList attr))
  })

(defn- tr-communities
  "refers on known communities table, or forms XXX:YYY with decimal"
  [attr] ; yet unpacked
  (map (fn [c]
         (let [v (Integer/toUnsignedLong c)]
           (cond (get (clojure.set/map-invert gobgpapi/known-community) v)
                 (str (get (clojure.set/map-invert gobgpapi/known-community) v))
            :else (apply str (concat (str (bit-shift-right v 16)) ":" (str (bit-and v 0xffff))))
         )))
       (.getCommunitiesList (.unpack attr Attribute$CommunitiesAttribute))))

(defn- tr-extendedcommunities
  [attr]
  (map (fn [c]
         (cond (.is c Attribute$TrafficRateExtended)
               {:rate (.getRate (.unpack c Attribute$TrafficRateExtended))} ; AS is not used
          :else c))
       (.getCommunitiesList (.unpack attr Attribute$ExtendedCommunitiesAttribute))))

(defn- dissect-paths
  " The items which is needed are alternatively picked up.
    Please refer 'message Path' in gobgpapi.proto
  "
  [paths] 
  (map (fn [p]
         { :path (reduce into (map
                   (fn [a]
                     (assert (= (type a) com.google.protobuf.Any))
                     (cond (.is a Attribute$MpReachNLRIAttribute)
                           {:MpReachNLRI (dissect-MpReachNLRI (.unpack a Attribute$MpReachNLRIAttribute))}
                           (.is a Attribute$OriginAttribute)
                           {:Origin (.getOrigin (.unpack a Attribute$OriginAttribute))}
                           (.is a Attribute$NextHopAttribute)
                           {:NextHop (.getNextHop (.unpack a Attribute$NextHopAttribute))}
                           (.is a Attribute$MultiExitDiscAttribute)
                           {:MED (.getMed (.unpack a Attribute$MultiExitDiscAttribute))}
                           (.is a Attribute$LocalPrefAttribute)
                           {:LocalPref (.getLocalPref (.unpack a Attribute$LocalPrefAttribute))}
                           (.is a Attribute$CommunitiesAttribute)
                           {:Communities (tr-communities a)}
                           (.is a Attribute$ExtendedCommunitiesAttribute)
                           {:ExtendedCommunities (tr-extendedcommunities a)}))
                 (.getPattrsList p)))
           :best (.getBest p)
           :age (.getSeconds (.getAge p))
           :source_asn (.getSourceAsn p)
         }
) paths))

(defn- build-request
  [table-type ver safi & lookup-prefix]
  (.build
    (doto (Gobgp$ListPathRequest/newBuilder)
          (.setFamily (build-family ver safi))
          (.setTableType (gobgpapi/table-type table-type)))))
  
(defn list-path
  [blocking-stub table-type ver safi & lookup-prefix]
  ; {:pre ...}
  (let [list-path (.listPath blocking-stub (build-request table-type ver safi lookup-prefix))]
    (loop [counter 0
           ret     []]
      (if (and (.hasNext list-path) (< counter 100))
          (recur (inc counter)
                 (let [dest (.getDestination (.next list-path))]
                   (let [prefix (.getPrefix dest)
                         paths  (.getPathsList dest)]
                     (conj ret {:prefix prefix :paths (dissect-paths paths)}))))
          ret))))
