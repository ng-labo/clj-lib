(ns refer-asn.core
  (:require [clojure.string :as str])
  (:use [clj-lib.gobgpapi.listpath])
  (:import [java.io BufferedReader FileReader FileWriter])
)

;
; one of example for gobgp-api
; using IP2Location[TM] LITE IP-COUNTRY Database
; aims to generate maxminddb format file
;

(def test-file-sample "./IP2LOCATION-LITE-DB1.CSV")

(defn- inet4-aton [a]
  (reduce #(+ (bit-shift-left %1 8) %2)
          (map #(Integer/parseInt %)
               (clojure.string/split a #"\.")))
)

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

(defn- first-octet
  [line]
  (let [items (map #(str/replace % "\"" "") (str/split line #","))]
    (bit-shift-right (Long/parseLong (nth items 0))24)))

(defn- range-data
  [line]
  (let [items (map #(str/replace % "\"" "") (str/split line #","))]
    (list (Long/parseLong (nth items 0)) (Long/parseLong (nth items 1)) (nth items 2))))

(defn create-index ;-from-ip2location-lite-db1
  [file-name]
  (let [reader (BufferedReader. (FileReader. file-name))]
    (let [ret (loop [ret (reduce conj (map #(hash-map % []) (range 1 256)))]
      (let [line (.readLine reader)]
        (if (nil? line)
            ret
            (recur (assoc ret (first-octet line)
                              (conj (get ret (first-octet line)) (range-data line)))) )))]
    (.close reader)
    ret
)))

(defn- get-prefixlen
  [prefix]
  (Integer/parseInt (last (clojure.string/split prefix #"/"))))

(defn- chop-prefixlen
  [prefix]
  (first (clojure.string/split prefix #"/")))

(defn find-cc
  [idx-cc prefix]
  (let [octet-key (Integer/parseInt (first (clojure.string/split prefix #"\.")))
        ip-val (inet4-aton (chop-prefixlen prefix))]
    ;(println "..." prefix)
    (let [idx (get idx-cc octet-key)]
      (assert (some? idx))
      (loop [rec idx]
        (if (first rec)
            (if (and (<= (nth (first rec) 0) ip-val)(<= ip-val (nth (first rec) 1)))
                (nth (first rec) 2) ;; CC
                (recur (next rec)))
            nil)))))

(defn lookup-cc
  [idx-cc ipasn-list]
  (map (fn [a]
         (let [prefix (first (keys a))
               asn    (first (vals a))]
           [(get-prefixlen prefix) prefix asn (find-cc idx-cc prefix)]))
       ipasn-list))
  
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
  (def all-blocks (map #(apply str (concat [ (str %) ".0.0.0/8"]))(range 1 256)))

  (reduce into [] (map #(process-a-block (call-listpath blocking-stub %)) all-blocks)))

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
