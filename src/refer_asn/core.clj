(ns refer-asn.core
  (:require [clojure.string :as str])
  (:require [clj-time.coerce :as tmc])
  (:require [clj-time.local :as local-tm])
  (:use [clj-lib.gobgpapi.listpath])
  (:import [java.io BufferedReader FileReader FileWriter])
)

;
; one of example for gobgp-api
; using IP2Location[TM] LITE IP-COUNTRY Database
; aims to generate maxminddb format file
;

(def test-file-sample "./IP2LOCATION-LITE-DB1.CSV")

; thread number for lookup-cc, get-all-prefixs
(def thread-num 4)

(defn- inet4-aton [a]
  (reduce #(+ (bit-shift-left %1 8) %2)
          (map #(Integer/parseInt %)
               (str/split a #"\."))))

(defn- inet4-ntoa [n]
  (apply str
    (concat
      (str (bit-shift-right (bit-and n 0xff000000) 24)) "."
      (str (bit-shift-right (bit-and n 0xff0000)   16)) "."
      (str (bit-shift-right (bit-and n 0xff00)      8)) "."
      (str                  (bit-and n 0xff)          ))))

(defn- calc-prefixlen
  [ip-start ip-end]
  (loop [range (inc (- ip-end ip-start))
         ans-prefixlen 33]
    (if (zero? range) ans-prefixlen (recur (int (/ range 2))(dec ans-prefixlen)))))

(defn- octet-value
  ; `line` is expected following
  ; "nnnnnnnnnn","nnnnnnnnnn","XX","...."
  ; 1st and 2nd column means integer as IP values
  ; return first 8bit integer in one of them
  [line item-index]
  (let [items (map #(str/replace % "\"" "") (str/split line #","))]
    (bit-shift-right (Long/parseLong (nth items item-index))24)))

(defn- range-data
  [line]
  (let [items (map #(str/replace % "\"" "") (str/split line #","))]
    (list (Long/parseLong (nth items 0)) (Long/parseLong (nth items 1)) (nth items 2))))

(defn set-range-data
  [line src]
  (let [i1 (octet-value line 0)
        i2 (octet-value line 1)]
    (loop [o (range i1 (inc i2)) ret src]
      (if (first o)
          (recur (next o)
                 (assoc ret (first o) (conj (get ret (first o)) (range-data line))))
          ret))))

(defn create-index ;-from-ip2location-lite-db1
  [file-name]
  (let [reader (BufferedReader. (FileReader. file-name))]
    (let [ret (loop [ret (reduce conj (map #(hash-map % []) (range 1 256)))]
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
  (let [octet-key (Integer/parseInt (first (str/split prefix #"\.")))
        ip-val (inet4-aton (chop-prefixlen prefix))]
    ;(println "..." prefix)
    (let [idx (get idx-cc octet-key)]
      (loop [rec idx]
        (if (first rec)
            (if (and (<= (nth (first rec) 0) ip-val)(<= ip-val (nth (first rec) 1)))
                (nth (first rec) 2) ;; CC
                (recur (next rec)))
            nil)))))

(defn- partitionize
  [whole-list part-num]
  (let [c (count whole-list)]
    (let [list-sz (if (zero? (mod c part-num)) (/ c part-num) (inc (int (/ c part-num))))]
      (partition list-sz list-sz nil whole-list))))

(defn lookup-cc
  [idx-cc ipasn-list]
  (reduce into []
    (pmap (fn [part-ipasn-list]
            (map (fn [a]
                   (let [prefix (first (keys a))
                         asn    (first (vals a))]
                     [(get-prefixlen prefix) prefix asn (find-cc idx-cc prefix)]))
                 part-ipasn-list))
          (partitionize ipasn-list thread-num))))
  
(defn process-a-block
  [listpath-result]
  (map (fn[x]
         (let [path (get (first (get x :paths)) :path)]
           (let [asn (last (first (get path :AsPath)))
                 atomic-aggregate (get path :AtomicAggregate)]
             {(get x :prefix) (if (nil? atomic-aggregate) asn nil)})))listpath-result))

(defn call-listpath
  [blocking-stub prefix]
  (clj-lib.gobgpapi.listpath/list-path blocking-stub :global 4 :unicast :longer prefix))

(defn get-all-prefixs
  [blocking-stub]
  (let [partitioned-blocks (partitionize (map #(apply str (concat [(str %) ".0.0.0/8"])) (range 1 256)) thread-num)]
    (reduce into []
      (pmap (fn [blocks]
              (reduce into [] (map #(process-a-block (call-listpath blocking-stub %))
                                   blocks)))
       partitioned-blocks))))

(defn write-list-data
  ; sort records by prefix and write to output-file
  [records output-file]
  (let [writer  (FileWriter. output-file)]
    (reduce (fn [w e]
                (let [prefix-len (nth e 0)
                      prefix (nth e 1)
                      asn (nth e 2)
                      cc (if (some? (nth e 3)) (nth e 3) "")]
                  (if (and (<= prefix-len 24) (<= asn 4199999999)); TODO exclude private range
                      (.write w (apply str (concat [prefix "," cc ","  (str asn) "\n"])))))
                w)
             writer
             (sort #(compare (nth %1 0)(get %2 0)) records))
      (.close writer)))

(defn- nowepoch
  []
  (tmc/to-long (local-tm/local-now)))

(defn batch
  [blocking-stub ip-cc-file output-csv]
  (let [start-time (nowepoch)
        cc-index   (create-index ip-cc-file)]
    (println "create-index " (- (nowepoch) start-time))
    (let [start-time  (nowepoch)
          all-prefixs (get-all-prefixs blocking-stub)]
      (println "get-all-prefixes " (- (nowepoch) start-time))
      (let [start-time (nowepoch)
            ip-asn-cc  (lookup-cc cc-index all-prefixs)]
        (println "count ip-asn-cc" (count ip-asn-cc))
        (println "lookup-cc " (- (nowepoch) start-time))
        (time (write-list-data ip-asn-cc output-csv))))))
