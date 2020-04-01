(ns clj-lib.mux-ssh.core
  (:require [clojure.core.async :as async]
            [clj-time.core :as t])
  (:import [java.io BufferedReader InputStreamReader PipedReader PipedWriter])
)

(def sshcmdtmpl
  ["/usr/bin/ssh" "-q" "-F/dev/null" "-oPasswordAuthentication=no" "-oKbdInteractiveAuthentication=no" "-oUseRoaming=no" "-oStrictHostKeyChecking=no" "-oConnectTimeout=20" "-oServerAliveCountMax=120" "-oServerAliveInterval=1" "-oControlPath=/tmp/%r@%h:%p" "-oControlMaster=auto" "-oControlPersist=yes"])

(defn run-cmd-and-get-reader [host args]
  (assert (type host) java.lang.String)
  (assert (type args) clojure.lang.PersistentVector)
  ; TODO check each element...

  (let [ cmd    (concat sshcmdtmpl ["-p" "22"] [host] args)
         proc   (.start (ProcessBuilder. (into-array String cmd)))
         reader (BufferedReader. (InputStreamReader. (.getInputStream proc))) ]
    [ proc reader ]))


(defn check-host [host]
  (assert (type host) java.lang.String)
  (let [ cmd (concat sshcmdtmpl ["-p" "22"] [host] ["exit" "0"])
         proc (.start (ProcessBuilder. (into-array String cmd)))]
    (.waitFor proc)
    (= (.exitValue proc) 0)))

(defn p2 [& more]
  (.write *out* (str (clojure.string/join " " more) "\n"))
  (.flush *out*))

(defn read-lines [ get-reader-arg func-process-line ]
  (let [ proc   (first get-reader-arg)
         reader (second get-reader-arg)]
    (loop []
      (let [line (.readLine reader)]
        (when (some? line)
          (func-process-line line)
          (recur))))
    (.waitFor proc)
    (.exitValue proc)))

(defn start-thread [ thread-body ]
  (.start (Thread. thread-body)))

(defn read-lines-th [ get-reader-arg func-process-line ]
  (let [proc (first get-reader-arg)
        reader (second get-reader-arg)
        signal-q (atom false)]
    (start-thread
      (fn []
        (loop []
          (let [line (.readLine reader) ]
            (when (and (some? line) (some? @signal-q))
              (func-process-line line)
              (recur))))
        (.close reader)
        (.waitFor proc)
        (.exitValue proc)
      )
    )signal-q))

