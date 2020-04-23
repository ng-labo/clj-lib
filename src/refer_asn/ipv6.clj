(ns refer-asn.ipv6
  (:require [clojure.string :as str])
  (:use [clj-lib.gobgpapi.listpath])
  (:import [java.io BufferedReader FileReader FileWriter])
  (:import [java.net Inet6Address])
)

;
; one of example for gobgp-api
; using IP2Location[TM] LITE IP-COUNTRY Database
; aims to generate maxminddb format file
;

; thread number for lookup-cc, get-all-prefixs
(def ^{:private true} thread-num 4)

(defn- inet6-aton [a]
  (reduce #(+ (* (bigint %1) 256) %2)
          (map #(bit-and 0xff %) (vec (.getAddress (java.net.Inet6Address/getByName a))))))

(defn inet6-ntoa [n]
  (.getHostAddress
    (Inet6Address/getByAddress ""
      (byte-array
        (loop [c 0 xn n ret []]
          (if (not= c 16)
            (recur (inc c) (/ xn 256) (conj ret (mod xn 256)))
            (map int (reverse ret))))))))

(defn- calc-prefixlen
  [ip-start ip-end]
  (loop [range (inc (- ip-end ip-start))
         ans-prefixlen 129]
    (if (zero? range) ans-prefixlen (recur (int (/ range 2))(dec ans-prefixlen)))))

(defn octet-value
  ; `line` is expected following
  ; "nnnnnnnnnn","nnnnnnnnnn","XX","...."
  ; 1st and 2nd column means integer as IP values
  ; return first 8bit integer in one of them
  [line item-index]
  (let [items (map #(str/replace % "\"" "") (str/split line #","))]
    (bit-and 0x0000ffff (int (/ (bigint (nth items item-index)) 0x1000000000000000000000000)))))

(defn- range-data
  [line]
  (let [items (map #(str/replace % "\"" "") (str/split line #","))]
    (list (bigint (nth items 0)) (bigint (nth items 1)) (nth items 2))))

(defn- get-cc
  [line]
  (let [items (map #(str/replace % "\"" "") (str/split line #","))]
    (nth items 2)))

(defn- set-range-data
  [line src]
  (let [cc (get-cc line)]
    (cond (= cc "-") src
     :else
  (let [i1 (octet-value line 0)
        i2 (octet-value line 1)]
    (loop [o (range i1 (inc i2)) ret src]
      (if (first o)
          (recur (next o)
                 (assoc ret (first o) (conj (get ret (first o)) (range-data line))))
          ret)))
)))

(defn create-index-ipv6 ;-from-ip2location-lite-db1
  [file-name]
  (let [reader (BufferedReader. (FileReader. file-name))]
    (let [ret (loop [ret {}]
      (let [line (.readLine reader)]
        (if (nil? line)
            ret
            (recur (set-range-data line ret)))))]
    (.close reader)
    ret
)))

(defn- get-prefixlen
  [prefix]
  (Integer/parseInt (last (str/split prefix #"/"))))

(defn- chop-prefixlen
  [prefix]
  (first (str/split prefix #"/")))

(defn find-cc
  [idx-cc prefix]
  (let [ip-val (inet6-aton (chop-prefixlen prefix))]
   (let [octet-key (bit-and 0x0000ffff (int (/ ip-val 0x1000000000000000000000000)))]
    ;(println "..." prefix)
    (let [idx (get idx-cc octet-key)]
      (loop [rec idx]
        (if (first rec)
            (if (and (<= (nth (first rec) 0) ip-val)(<= ip-val (nth (first rec) 1)))
                (nth (first rec) 2) ;; CC
                (recur (next rec)))
            nil))))))

(defn- partitionize
  [whole-list part-num]
  (let [c (count whole-list)]
    (let [list-sz (if (zero? (mod c part-num)) (/ c part-num) (inc (int (/ c part-num))))]
      (partition list-sz list-sz nil whole-list))))

(defn lookup-cc-ipv6
  [idx-cc ipasn-list]
  (reduce into []
    (pmap (fn [part-ipasn-list]
            (map (fn [a]
                   (let [prefix (first (keys a))
                         asn    (first (vals a))]
                     [(get-prefixlen prefix) prefix asn (find-cc idx-cc prefix)]))
                 part-ipasn-list))
          (partitionize ipasn-list thread-num))))
  
(defn- process-a-block
  [listpath-result]
  (map (fn[x]
         (let [path (get (first (get x :paths)) :path)]
           (let [asn (last (first (get path :AsPath)))
                 atomic-aggregate (get path :AtomicAggregate)]
             {(get x :prefix) (if (nil? atomic-aggregate) asn nil)})))listpath-result))

(defn- call-listpath
  [blocking-stub prefix]
  (clj-lib.gobgpapi.listpath/list-path blocking-stub :global 6 :unicast :longer prefix))

(defn get-all-prefixs-ipv6
  [blocking-stub]
  (process-a-block (call-listpath blocking-stub "::/0")))

(defn write-list-data-ipv6
  ; sort records by prefix and write to output-file
  [records output-file]
  (let [writer  (FileWriter. output-file)]
    (reduce (fn [w e]
                (let [prefix-len (nth e 0)
                      prefix (nth e 1)
                      asn (nth e 2)
                      cc (if (some? (nth e 3)) (nth e 3) "")]
                  (if (and (<= prefix-len 64) (<= asn 4199999999)); TODO exclude private range
                      (.write w (apply str (concat [prefix "," cc ","  (str asn) "\n"])))))
                w)
             writer
             (sort #(compare (nth %1 0)(get %2 0)) records))
      (.close writer)))

