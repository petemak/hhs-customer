(ns hhs.customer.db
  (:require [mount.core :refer [defstate]]
            [datomic.api :as d]
            [clojure.tools.logging :as log]))



;; -------------------------------------------------------
;; The database uri
(def db-uri "datomic:mem://customer")

(def config {:datomic {:db-uri "datomic:mem://customer"}})



;; -------------------------------------------------------
;; Schema describing the Customer entity.
;; The antiy consists for the following attributes:
;; - :customer/id
;; - :customer/name
;; - :customer/street
;; - :customer/housenumber
;; - :customer/zipcode
;; - :customer/phone
;; - :customer/emai
;;
;; -------------------------------------------------------
(def schema
  [{:db/ident :customer/id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Unique identifier"}
   {:db/ident :customer/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Name of customer"}
   {:db/ident :customer/street
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Street of residence"}
   {:db/ident :customer/housenumber
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "House number"}   
   {:db/ident :customer/city
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "City of residence"}
   {:db/ident :customer/zipcode
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Zip code of residence"}
   {:db/ident :customer/phone
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Customers phone number"}
   {:db/ident :customer/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Customers email"}])


(defn create-connection
  "Intiates a connection to the database"
  [conf]
  (log/info "Creating connection to datomic...")
  (let [db-uri (get-in conf [:datomic :db-uri])]
    (log/info "Creating connection to datomic:" db-uri)
    (if-let [db (d/create-database db-uri)]
      (d/connect db-uri))))


(defn close-connection
  "Releases the specified connection"
  [conf conn]
  (log/info "Closing connection to datomic...")
  (let [db-uri (get-in conf [:datomic :db-uri])]
    (log/info "Releasing connection to:" db-uri)
    (.release conn)
    (log/info "Deleting database")
    (d/delete-database db-uri)))


(defstate conn :start (create-connection config)
               :stop (close-connection config conn))

;;--------------------------------------------------
;; Staging
;; Create datomic schem
(defn create-schema
  "Create datomic schema"
  []
  (log/info "Creating schema......")
  (d/transact conn schema))



