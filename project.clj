(defproject clj-lib "0.1.0-SNAPSHOT"
  :description "clojure library in network programing like snmp,gobgpapi,..."
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repl-options {:init-ns clj-lib.core}
  :main ^skip-aot clj-lib.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "1.0.0"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/core.async "0.7.559"]
                 [clj-time "0.15.2"] 
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [org.slf4j/slf4j-api "1.7.30"]
                 [org.slf4j/log4j-over-slf4j "1.7.30"] ; Log4j to SLF4J    
                 [javax.annotation/javax.annotation-api "1.3.2"]
                 [com.google.protobuf/protobuf-java "3.11.4"]
                 [io.grpc/grpc-api "1.28.0"]
                 [io.grpc/grpc-core "1.28.0" :exclusions [io.grpc/grpc-api]]
                 [io.grpc/grpc-netty-shaded "1.28.0" :exclusions [io.grpc/grpc-api io.grpc/grpc-core]]
                 [io.grpc/grpc-protobuf "1.28.0"]
                 [io.grpc/grpc-stub "1.28.0"]
                 [org.snmp4j/snmp4j "3.4.0"]]


  :plugins [[lein-protoc "0.5.0"]
            [lein-cljfmt "0.6.7"]
            [jonase/eastwood "0.3.10"]
            [lein-kibit "0.1.8"]
           ]

  ; for lein-protoc
  :protoc-version "3.11.4"
  :proto-source-paths ["ext/proto"]
  :protoc-grpc {:version "1.28.0"}
  :proto-target-path "target/generated-sources/protobuf"
  :java-source-pathes ["target/generated-sources/protobuf"]

  ; for lein-javac
  :java-source-paths ["target/generated-sources/protobuf"]

)
