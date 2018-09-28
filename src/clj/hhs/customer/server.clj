(ns hhs.customer.server
  (:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [hhs.customer.service :as service]
            [mount.core :as mount]
            [io.pedestal.test :as test]))


;;==================================================================================
;; Dev server
;;==================================================================================
(defonce server (atom nil))

;; Consumed by io.pedestal.http/create-server
;; See http/default-interceptors for additional options you can configure
(def server-map
  {::http/routes service/routes
   ::http/type :jetty
   ::http/resource-path "/public"
   ::http/port 8890
   ::http/container-options {:h2c? true
                             :H2? false
                             ;:keystore "test/hp/keystore.jks"
                             ;:key-password "password"
                             ;:ssl-port 8443
                             :ssl? false}})

(defn start-dev
  "Start dev instance of the server"
  []
  (mount/start)
  (reset! server
          (http/start (http/create-server (assoc server-map
                                                 ::http/join? false)))))


(defn stop-dev
  "Stop dev instance of server"
  []
  (mount/stop)
  (http/stop @server))

(defn test-request
  [verb url]
  (io.pedestal.test/response-for (::http/service-fn @server) verb url))
;;=========================================================================



;;=========================================================================
;; This is an adapted service map, that can be started and stopped
;; From the REPL you can call server/start and server/stop on this service
(defonce runnable-service (http/create-server service/service))


;; ========================================================================
;;
;; 
(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (println "\nCreating your [DEV] server...")
  (-> service/service ;; start with production configuration
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::http/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::http/routes service/routes
              ;; all origins are allowed in dev mode
              ::http/allowed-origins {:creds true
                                      :allowed-origins (constantly true)}
              ;; Content Security Policy (CSP) is mostly turned off
              ;; in dev mode
              ::http/secure-headers {:content-security-policy-settings
                                                  {:object-src "'none'"}}})
      ;; Wire up interceptor chains
      http/default-interceptors
      http/dev-interceptors
      http/create-server
      http/start))

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (mount/start)
  (http/start runnable-service))


