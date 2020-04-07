(ns clj-lib.mux-ssh.core
  (:require [clojure.core.async :as async]
            [clj-time.core :as t])
  (:import [java.io BufferedReader InputStreamReader PipedReader PipedWriter])
)

(def sshcmdtmpl
  ["/usr/bin/ssh" "-q" "-F/dev/null"
   "-oPasswordAuthentication=no"
   "-oKbdInteractiveAuthentication=no"
   "-oUseRoaming=no"
   "-oStrictHostKeyChecking=no"
   "-oConnectTimeout=10"
   "-oServerAliveCountMax=120"
   "-oServerAliveInterval=1"
   "-oControlPath=/tmp/%r@%h:%p"
   "-oControlMaster=auto"
   "-oControlPersist=yes"])

(defn- fork-cmd [host args]
  {:pre [ (= (type host) java.lang.String)
          (not-any? #(= (type args) %)
                    '(clojure.lang.PersistentList clojure.lang.PersistentVector))] }
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

(defn run-cmd [ host cmd-args func-process-line ]
  (let [proc-reader (fork-cmd host cmd-args)]
    (let [ proc   (first proc-reader)
           reader (second proc-reader)]
      (loop []
        (let [line (.readLine reader)]
          (when (some? line)
            (func-process-line line)
            (recur))))
      (.waitFor proc)
      (.exitValue proc))))

;(defn start-thread [ thread-body ]
;  (.start (Thread. thread-body)))

(defn run-cmd-th [ host cmd-args func-process-line ]
  (let [proc-reader (fork-cmd host cmd-args)]
    (let [ proc   (first proc-reader)
           reader (second proc-reader)
           signal (atom true)]
      (.start (Thread.
        (fn []
          (loop []
            (let [line (.readLine reader) ]
              (when (and (some? line) @signal)
                (func-process-line line)
                (recur))))
          (.close reader)
          (.waitFor proc)
          (.exitValue proc))))
    signal)))

