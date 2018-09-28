(defproject customer "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [io.pedestal/pedestal.service "0.5.4"]
                 [io.pedestal/pedestal.jetty "0.5.4"]
                 [com.datomic/datomic-free "0.9.5697"]
                 ;; [io.pedestal/pedestal.immutant "0.5.4"]
                 ;; [io.pedestal/pedestal.tomcat "0.5.4"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [com.taoensso/timbre "4.10.0"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]            
                 [mount "0.1.12"]]
  :min-lein-version "2.0.0"
  :source-paths ["src/clj"]
  :java-source-paths ["src/jvm"]
  :test-paths ["test/clj" "test/jvm"]
  :resource-paths ["config", "resources"]
  :plugins [[:lein-codox "0.10.3"]
            [:lein-cloverage "1.0.10"]  ;; Code coverage
            [test2junit "1.4.2"]]       ;; Output test results as JUnit XML format
  :codox {:namespaces :all}             ;; Generate API documentation from Clojure or ClojureScript source code.

  :test2junit-output-dir "target/test-reports"

  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "customer.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.4"]]}
             :doc {:dependencies [[codox-theme-rdash "0.1.2"]]
                   :codox {:metadata {:doc/format :markdown}
                           :themes [:rdash]}}
             :debug {:jvm-opts ["-server" (str "-agentlib:jdwp=transport=dt_socket,"
                                               "server=y,address=8000,suspend=n")]}             
             :uberjar {:aot [hhs.customer.server]}}
  :main ^{:skip-aot true} hhs.customer.server)
