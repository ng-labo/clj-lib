(ns clj-lib.gobgpapi.flowspec
  (:require [clj-lib.gobgpapi.core :as gobgpapi])
  (:import [com.google.protobuf Any])
  (:import [gobgpapi Gobgp$GetBgpRequest
                     Gobgp$AddPathRequest
                     Gobgp$DeletePathRequest
                     Gobgp$Path
                     Gobgp$Family Gobgp$Family$Afi Gobgp$Family$Safi Gobgp$TableType ])
  (:import [gobgpapi Attribute$FlowSpecIPPrefix
                     Attribute$FlowSpecComponent
                     Attribute$FlowSpecComponentItem
                     Attribute$FlowSpecNLRI
                     Attribute$TrafficRateExtended
                     Attribute$ExtendedCommunitiesAttribute
                     Attribute$MpReachNLRIAttribute
                     Attribute$OriginAttribute])
)

(defn- build-family
  [ver]
  (.build
    (doto (Gobgp$Family/newBuilder)
          (.setAfi (cond (= ver 4) (Gobgp$Family$Afi/AFI_IP)
                         (= ver 6) (Gobgp$Family$Afi/AFI_IP6)))
          (.setSafi Gobgp$Family$Safi/SAFI_FLOW_SPEC_UNICAST))))

(defn- build-prefix
  [arg]
  (Any/pack
    (.build
      (doto (Attribute$FlowSpecIPPrefix/newBuilder)
            (.setType (get arg :type))
            (.setPrefixLen (get arg :prefixlen))
            (.setPrefix (get arg :prefix))))))

(defn- componentitem
  [condition]
  (.build
    (doto (Attribute$FlowSpecComponentItem/newBuilder)
          (.setOp (get condition :op))
          (.setValue (get condition :value)))))

(defn- build-component
  [arg]
  (let [typeval (get arg :type)
        conditions (get arg :conditions)
        builder (Attribute$FlowSpecComponent/newBuilder)]
    (.setType builder typeval)
    (reduce #(.addItems %1 (componentitem %2)) builder conditions)
    (Any/pack (.build builder))))
     
(defn- build-nlri
  [rules]
  (let [ builder (Attribute$FlowSpecNLRI/newBuilder)]
    (reduce #(.addRules %1 %2) builder rules)
    (Any/pack (.build builder))))

(defn- build-extendedcommunities
  [rate]
  (Any/pack
    (.build
      (doto (Attribute$ExtendedCommunitiesAttribute/newBuilder)
            (.addCommunities
              (Any/pack
                (.build
                  (doto (Attribute$TrafficRateExtended/newBuilder)
                        (.setRate rate)))))))))

(defn- build-mpreachnlri
  [family nlri]
  (Any/pack
    (.build
      (doto (Attribute$MpReachNLRIAttribute/newBuilder)
            (.setFamily family)
            (.addNlris nlri)
            (.addNextHops "0.0.0.0")))))

(defn- build-origin
  [origin]
  (Any/pack
    (.build
      (doto (Attribute$OriginAttribute/newBuilder)
            (.setOrigin origin)))))

(defn- build-path
  [args]
  (let [origin     (get gobgpapi/attr-origin (get args :origin))
        rate       (get args :rate)
        family     (build-family (get args :ipver))
        asn        (get args :asn)
        prefixes   (get args :prefixes)
        conditions (get args :conditions)]
    (let [nlri (build-nlri (concat
                             (reduce into [] (vector (map build-prefix prefixes)))
                             (reduce into [] (vector (map build-component conditions)))))]
      (.build
        (doto (Gobgp$Path/newBuilder)
              (.setFamily family)
              (.setNlri nlri)
              (.setSourceAsn asn)
              (.addPattrs (build-extendedcommunities rate))
              (.addPattrs (build-mpreachnlri family nlri))
              (.addPattrs (build-origin origin))
      )))))

(defn add-flowspec
  [args blocking-stub]
  (let [asn (.getAs (.getGlobal (.getBgp blocking-stub (.build (Gobgp$GetBgpRequest/newBuilder)))))]
    (let [path (build-path (assoc args :asn asn))]
      (.addPath blocking-stub
        (.build
          (doto (Gobgp$AddPathRequest/newBuilder)
                (.setTableType Gobgp$TableType/GLOBAL)
                (.setPath path)))))))

(defn delete-flowspec
  [args blocking-stub]
  (let [asn (.getAs (.getGlobal (.getBgp blocking-stub (.build (Gobgp$GetBgpRequest/newBuilder)))))]
    (let [path (build-path (assoc args :asn asn))]
      (.deletePath blocking-stub
        (.build
          (doto (Gobgp$DeletePathRequest/newBuilder)
                (.setTableType Gobgp$TableType/GLOBAL)
                (.setPath path)))))))

(def ex-ipv4-1
  { :origin     0
    :rate       0
    :ipver      4
    :prefixes   [ {:type 1 :prefix "1.2.3.4" :prefixlen 32}
                  {:type 2 :prefix "5.6.7.8" :prefixlen 32} ]
    :conditions [ {:type 3 :conditions [{:op 1 :value 17}]}
                  {:type 4 :conditions [{:op 1 :value 1900}{:op 1 :value 11211}]} ]
  })

(def ex-ipv4-2
  { :origin     0
    :rate       0
    :ipver      4
    :prefixes   [ {:type 1 :prefix "5.6.7.89" :prefixlen 32} ]
    :conditions [ {:type 3 :conditions [{:op 1 :value 17}]}
                  {:type 4 :conditions [{:op 1 :value 11211}]} ]
  })

(def ex-ipv6-1
  { :origin     0
    :rate       0
    :ipver      6
    :prefixes   [ {:type 1 :prefix "2001:db8:cafe::1" :prefixlen 128}
                  {:type 2 :prefix "2001:db8:cafe::2" :prefixlen 128} ]
    :conditions [ {:type 3 :conditions [{:op 1 :value 17}]}
                  {:type 4 :conditions [{:op 1 :value 1900}{:op 1 :value 11211}]} ]
  })
