(ns clj-lib.gobgpapi.unicast
  (:require [clj-lib.gobgpapi.core :as gobgpapi])
  (:import [com.google.protobuf Any])
  (:import [gobgpapi Gobgp$GetBgpRequest
                     Gobgp$AddPathRequest
                     Gobgp$DeletePathRequest
                     Gobgp$Path
                     Gobgp$Family Gobgp$Family$Afi Gobgp$Family$Safi Gobgp$TableType ])
  (:import [gobgpapi Attribute$IPAddressPrefix
                     Attribute$NextHopAttribute
                     Attribute$MultiExitDiscAttribute
                     Attribute$LocalPrefAttribute
                     Attribute$CommunitiesAttribute
                     Attribute$MpReachNLRIAttribute
                     Attribute$OriginAttribute])
)

(defn- build-family
  [ver]
  (.build
    (doto (Gobgp$Family/newBuilder)
          (.setAfi (cond (= ver 4) (Gobgp$Family$Afi/AFI_IP)
                         (= ver 6) (Gobgp$Family$Afi/AFI_IP6)))
          (.setSafi Gobgp$Family$Safi/SAFI_UNICAST))))

(defn- build-nlri
  [arg]
  (Any/pack
    (.build
      (doto (Attribute$IPAddressPrefix/newBuilder)
            (.setPrefixLen (get arg :prefixlen))
            (.setPrefix (get arg :prefix))))))

(defn- build-nexthop
  [nexthop]
  (Any/pack
    (.build
      (doto (Attribute$NextHopAttribute/newBuilder)
            (.setNextHop nexthop)))))

(defn- build-nexthop-mp
  [family nlri nexthop]
  (Any/pack
    (.build
      (doto (Attribute$MpReachNLRIAttribute/newBuilder)
            (.setFamily family)
            (.addNlris nlri)
            (#(if (some? nexthop) (.addNextHops % nexthop)))))))

(defn- build-origin
  [origin]
  (Any/pack
    (.build
      (doto (Attribute$OriginAttribute/newBuilder)
            (.setOrigin origin)))))
     
(defn- build-multiexitdisc
  [med]
  (Any/pack
    (.build
      (doto (Attribute$MultiExitDiscAttribute/newBuilder)
            (.setMed med)))))

(defn- build-localpref
  [localpref]
  (Any/pack
    (.build
      (doto (Attribute$LocalPrefAttribute/newBuilder)
            (.setLocalPref localpref)))))

(defn- build-communities
  [communities]
  (let [builder (Attribute$CommunitiesAttribute/newBuilder)]
    (reduce #(.addCommunities %1 %2) builder communities)
    (Any/pack (.build builder))))

(defn- build-path
  [args]
  (let [origin      (get gobgpapi/attr-origin (get args :origin))
        nexthop     (get args :nexthop)
        asn         (get args :asn)
        prefix      (get args :prefixes)
        med         (get args :med)
        localpref   (get args :localpref)
        communities (gobgpapi/tr-communities (get args :communities))]
    (let [nlri   (build-nlri prefix)
          family (build-family (get args :ipver))]
      (.build 
        (doto (Gobgp$Path/newBuilder)
              (.setFamily family)
              (.setNlri nlri)
              (.setSourceAsn asn)
              (.addPattrs (build-nexthop-mp family nlri nexthop))
              (.addPattrs (build-origin origin))
              (#(if (some? communities)
                    (.addPattrs % (build-communities communities))))
              (#(if (some? med)
                    (.addPattrs % (build-multiexitdisc med))))
              (#(if (some? localpref)
                    (.addPattrs % (build-localpref localpref)))))
      ))))

(defn- build-withdraw-path
  [args]
  (let [ nexthop     (get args :nexthop)
         asn         (get args :asn)
         prefix      (get args :prefixes)]
    (let [nlri   (build-nlri prefix)
          family (build-family (get args :ipver))]
      (.build 
        (doto (Gobgp$Path/newBuilder)
              (.setFamily family)
              (.setNlri nlri)
              (.setSourceAsn asn)
              (.addPattrs (build-nexthop-mp family nlri nexthop))
      )))))

(defn add-path
  [args blocking-stub]
  (let [asn (.getAs (.getGlobal (.getBgp blocking-stub (.build (Gobgp$GetBgpRequest/newBuilder)))))]
    (let [path (build-path (assoc args :asn asn))]
      (.addPath blocking-stub
        (.build
          (doto (Gobgp$AddPathRequest/newBuilder)
                (.setTableType Gobgp$TableType/GLOBAL)
                (.setPath path)))))))

(defn delete-path
  [args blocking-stub]
  (let [asn (.getAs (.getGlobal (.getBgp blocking-stub (.build (Gobgp$GetBgpRequest/newBuilder)))))]
    (let [path (build-withdraw-path (assoc args :asn asn))]
      (.deletePath blocking-stub
        (.build
          (doto (Gobgp$DeletePathRequest/newBuilder)
                (.setTableType Gobgp$TableType/GLOBAL)
                (.setPath path)))))))

(def example-call-args
  { :origin     :egp
    :nexthop    "2001:db8:cafe::1"
    :ipver      6
    :prefixes   {:prefix "2001:db8:cafe::" :prefixlen 64}
    :communities [ 0xffff029a "65432:999" ]
    :asn  65001
    :med 100
    :localpref 200
  })
(def example-call-args2
  { :origin     :incomplete
    :nexthop    "2001:db8:cafe::1"
    :ipver      6
    :prefixes   {:prefix "2001:db8:cafe::ffff" :prefixlen 128}
    :communities [ 0xffff029a "65432:999" ]
    :asn  65001
  })
