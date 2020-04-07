(ns clj-lib.gobgpapi.listpath
  (:require [clojure.set :refer :all]
            [clj-lib.gobgpapi.core :as gobgpapi])
  (:import [com.google.protobuf Any])
  (:import [gobgpapi Gobgp$GetBgpRequest
                     Gobgp$ListPathRequest
                     Gobgp$TableLookupOption Gobgp$TableLookupPrefix
                     Gobgp$Path
                     Gobgp$Family Gobgp$Family$Afi Gobgp$Family$Safi])
  (:import [gobgpapi Attribute$FlowSpecIPPrefix
                     Attribute$FlowSpecComponent
                     Attribute$FlowSpecComponentItem
                     Attribute$FlowSpecNLRI
                     Attribute$TrafficRateExtended
                     Attribute$CommunitiesAttribute
                     Attribute$LargeCommunitiesAttribute
                     Attribute$ExtendedCommunitiesAttribute
                     Attribute$MpReachNLRIAttribute
                     Attribute$OriginAttribute
                     Attribute$NextHopAttribute
                     Attribute$MultiExitDiscAttribute
                     Attribute$LocalPrefAttribute
                     Attribute$IPAddressPrefix
                     Attribute$LabeledIPAddressPrefix
                     Attribute$AsSegment
                     Attribute$AsPathAttribute
                     Attribute$AggregatorAttribute ])
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

(defn- tr-aspath
  [attr] ; yet unpacked
  (map (fn [as-seg]
         (.getNumbersList as-seg))
    (.getSegmentsList (.unpack attr Attribute$AsPathAttribute))))

(defn- tr-ipaddressprefix
  [attr] ; yet unpacked
  (let [ipaddr (.unpack attr Attribute$IPAddressPrefix)]
    { :prefix (.getPrefix ipaddr) :prefixlen (.getPrefixLen ipaddr) }))

(defn- dissect-Nlris
  " not yet implement
    ; EVPNEthernetAutoDiscoveryRoute
    ; EVPNMACIPAdvertisementRoute
    ; EVPNInclusiveMulticastEthernetTagRoute
    ; EVPNEthernetSegmentRoute
    ; EVPNIPPrefixRoute
    ; EVPNIPMSIRoute
    ; LabeledVPNIPAddressPrefix
    ; RouteTargetMembershipNLRI
    ; FlowSpecNLRI
    ; VPNFlowSpecNLRI
    ; OpaqueNLRI
    ; LsAddrPrefix
  "
  [nlri-list]
  (reduce into
          (map (fn [nlri]
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

(defn- tr-aggregator
  [attr]
  (let [o (.unpack attr Attribute$AggregatorAttribute)]
    { :As (.getAs o) :Address (.getAddress o) }))

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

(defn- tr-largecommunities
  [attr]
  (map (fn [lc]
         (apply str (concat
           (str (Integer/toUnsignedLong (.getGlobalAdmin lc))) ":"
           (str (Integer/toUnsignedLong (.getLocalData1 lc))) ":"
           (str (Integer/toUnsignedLong (.getLocalData2 lc))))))
        (.getCommunitiesList (.unpack attr Attribute$LargeCommunitiesAttribute))))

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
                     (cond 
                           (.is a Attribute$OriginAttribute)
                           {:Origin (.getOrigin (.unpack a Attribute$OriginAttribute))}
                           (.is a Attribute$AsPathAttribute)
                           {:AsPath (tr-aspath a)}
                           (.is a Attribute$NextHopAttribute)
                           {:NextHop (.getNextHop (.unpack a Attribute$NextHopAttribute))}
                           (.is a Attribute$MultiExitDiscAttribute)
                           {:MED (.getMed (.unpack a Attribute$MultiExitDiscAttribute))}
                           (.is a Attribute$LocalPrefAttribute)
                           {:LocalPref (.getLocalPref (.unpack a Attribute$LocalPrefAttribute))}
                           ; AtomicAggregateAttribute
                           (.is a Attribute$AggregatorAttribute)
                           {:Aggregator (tr-aggregator a)}
                           (.is a Attribute$CommunitiesAttribute)
                           {:Communities (tr-communities a)}
                           ; OriginatorIdAttribute
                           ; ClusterListAttribute
                           (.is a Attribute$IPAddressPrefix)
                           {:Prefix (.unpack a Attribute$IPAddressPrefix)} ; TODO
                           (.is a Attribute$LabeledIPAddressPrefix)
                           {:Prefix (.unpack a Attribute$LabeledIPAddressPrefix)} ;TODO
                           (.is a Attribute$MpReachNLRIAttribute)
                           {:MpReachNLRI (dissect-MpReachNLRI (.unpack a Attribute$MpReachNLRIAttribute))}
                           (.is a Attribute$LargeCommunitiesAttribute)
                           {:LargeCommunities (tr-largecommunities a)}
                           (.is a Attribute$ExtendedCommunitiesAttribute)
                           {:ExtendedCommunities (tr-extendedcommunities a)}
                      :else {:not-yet-implement a}))
                 (.getPattrsList p)))
           :best (.getBest p)
           :age (.getSeconds (.getAge p))
           :source_asn (.getSourceAsn p)
         }
) paths))

(def lookup-options
  { :exact Gobgp$TableLookupOption/LOOKUP_EXACT,
    :longer Gobgp$TableLookupOption/LOOKUP_LONGER,
    :shorter Gobgp$TableLookupOption/LOOKUP_SHORTER } )

(defn- build-tablelookupprefix
  [prefix option]
  (.build
    (doto (Gobgp$TableLookupPrefix/newBuilder)
          (.setPrefix prefix)
          (.setLookupOption (get lookup-options option))
          )))

(defn- build-request
  [table-type ver safi lu-option prefixs]
  (let [builder (Gobgp$ListPathRequest/newBuilder)]
    (doto builder
          (.setFamily (build-family ver safi))
          (.setTableType (gobgpapi/table-type table-type)))
    (reduce #(.addPrefixes %1 (build-tablelookupprefix %2 lu-option)) builder prefixs)
    (.build builder)))
  
(defn list-path
  [blocking-stub table-type ver safi lookup & lookup-prefix]
  {:pre [ (not (not-any? #(= ver %) '(4 6)))
          (not (not-any? #(= safi %) '(:unicast :flowspec)))
          (not (not-any? #(= table-type %) '(:global :local :adj-in :adj-out)))
          (cond (= safi :unicast)
                (and (some? lookup-prefix) (not (not-any? #(= lookup %) '(:exact :shorter :longer))))
                :else true)
          ]}
  (let [list-path (.listPath blocking-stub (build-request table-type ver safi lookup lookup-prefix))]
    (loop [ ret     []]
      (if (.hasNext list-path)
          (recur (let [dest (.getDestination (.next list-path))]
                   (let [prefix (.getPrefix dest)
                         paths  (.getPathsList dest)]
                     (conj ret {:prefix prefix :paths (dissect-paths paths)}))))
          ret))))
