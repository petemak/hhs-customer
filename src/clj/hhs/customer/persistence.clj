(ns hhs.customer.persistence
  (:require [datomic.api :as d]
            [hhs.customer.db :as cdb]
            [mount.core :refer [defstate]]))



;; -------------------------------------------------------
;; implementaion for customer port
;; -------------------------------------------------------
(defprotocol CustomerDBPort
  "Customer database port to be implemented by adpters"
  
  (insert [this id name street hnum city phone email]
    "Adds or update a customer entity")

  (fetch [this id fs]
    "Gets the specified customer with all required fields")

  (delete [this id]
    "Retract fact")
  
  (close [this]
    "Close database connection"))

;; -------------------------------------------------------
;; Datomic adapter implementaion for the customer port
;;  [find-spec with-clause? inputs? where-clauses?]
;; -------------------------------------------------------
(defn- get-entity-id
  [conn id]
  (-> (d/q '[:find ?e
             :in $ ?id
             :where [?e :customer/id ?id]]
           (d/db conn)
           (str id))
      ffirst))

(defn get-entity
  [conn id]
  (let [eid (get-entity-id conn id)]
    (->> (d/entity (d/db conn) eid)
         seq
         (into {}))))


(defrecord DatomicCustomerDB [conn]
  CustomerDBPort

  (insert [this id name street hnum city phone email]
    (d/transact conn
                (vector (into {} (filter (comp some? val)
                                         {:db/id id
                                          :customer/id id
                                          :customer/name name
                                          :customer/street street
                                          :customer/housenumber hnum
                                          :customer/city city
                                          :customer/phone phone
                                          :customer/email email})))))
  (fetch [this id fs]
    (when-let [customer (get-entity conn id)]
      (if (empty? fs)
        customer
        (select-keys customer (map keyword fs)))))

  (delete [this id]
    (when-let [eid (get-entity-id conn id)]
      (d/transact conn
                  [[:db.fn/retractEntity eid]])))
  
  (close [this]
    (d/shutdown true)))

(defn create-customer-db
  "Intiates a connection to the databse
   transacts the schema and "
  []
  (cdb/create-schema)
  (DatomicCustomerDB. cdb/conn))


;;-----------------------------------------
;; Manges state of the database component
;;-----------------------------------------
(defstate customerdb
  :start (create-customer-db)
  :stop (.stop customerdb))
