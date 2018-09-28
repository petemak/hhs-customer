(ns hhs.customer.core
  (:require [clojure.string :as s]
            [io.pedestal.interceptor.chain :as chain]
            [hhs.customer.persistence :refer [customerdb]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [taoensso.timbre :as timbre]
            [hhs.customer.utils :as utils])
  (:import [java.io IOException]
           [java.util UUID]))


(def file-logger  (utils/file-logger "core-log.log"))

;;------------------------------------------
;;
;;------------------------------------------
(defn gen-resp
  "Generate a resonse with the given status and body"
  [s b]
  (log/info "Generating response for status " s " and body " b)
  {:status s
   :body b})

;;-----------------------------------------
;; Non public UID generator
;; Dummy implementation just returns "hhsuser"x
;;-----------------------------------------
(defn- get-uid
  [token]
  (when (and (string? token) (not (empty? token)))
    {"uid" (str "hhsuser")}))


;;-----------------------------------------
;; Aurhentication service implemented as and interceptor
;; Looks up for token in request headersy
;; If found then looks up user detials and
;; populates the transaction data map :tx-data
;; under the :user key
;;-----------------------------------------
(def auth
  {:name ::auth
   :enter (fn [context]
            (let [token (-> context :request :headers (get "token"))]
              (log/info "Auth token:" token)
              (if-let [uid (and (not (nil? token))
                                (get-uid token))]
                (assoc-in context [:request :tx-data :user] uid)
                (chain/terminate
                 (assoc context :response (gen-resp 401 "Authentication token not found!"))))))
   :error (fn [context ex-info]
            (assoc context
                   :response
                   (gen-resp context 500 (.getMessage ex-info))))})


(defn merge-parans
  "Merge path, request and form parameters into one map"
  [context]
  (let [req (:request context)]
    (merge (:path-params req)
           (:query-params req)
           (:form-params req))))

;; ---------------------------------------
;; 
;;
;; ---------------------------------------
(defn extract-request-params
  "Extract path, query and form parameters inot a single map. "
  [context]
  (let [params (merge (-> context :request :form-params)
                      (-> context :request :query-params)
                      (-> context :request :path-params))
        contacts-found (or (params :id)
                           (params :name)
                           (params :phone)
                           (params :email)
                           (params :adress))]

    (file-logger "Extracting parameters....")
    (file-logger (str  "Params: " params))
    (if (and (not (empty? params)) contacts-found)
      (let [flds (if-let [fls (:flds params)]
                   (map s/trim (s/split fls #",") )
                   (vector))
            params (assoc params :flds flds)]
        
        (assoc context :tx-data params))
      (chain/terminate
       (assoc context
              :respomse (gen-resp 400
                                  "One of the following contact details is obligatory: 
                                   address, email or mobile number"))))))

;;----------------------------------------
;; Implementation of interceptors for validation
;;
;; srevice.clj/routes/customer/:d :get -> core/validate-id -> core.clj/get-customer
;; ---------------------------------------

;; Intercpetor checks if a valif ID was
;; submitted in form, query or path params
;; If processing continues
;; if not terminates chain processing with HTTP 400 BAD REQUEST
;; :query-params - after ? - https://domain.cxy/path?queryparma=value
;; :path-params - after domain https://domain.cxy/user/:id
(def validate-customer-id
  {:name ::validate-customer-id
   :enter (fn [context]
            (if-let [cid (or (-> context :request :path-params :id)
                             (-> context :request :form-params :id)
                             (-> context :request :query-params :id))]
              ;; Validate and return a context object with tx-data
              (extract-request-params context)
              (chain/terminate (assoc context
                                      :response (gen-resp 400
                                                          "Invalid customer id")))))
   :error (fn [context ex-inf]
            (assoc context
                   :response (gen-resp 500
                                       (.getMessage ex-info))))})

;; Check if form parameters are provided, collects
;; and maps these to :tx-data 
(def validat-form-data
  {:name ::validate-form-data
   
   :enter (fn [context]
            (if-let [params (-> context
                                :request
                                :form-params )]
              ;; Validate and return form data bound to tx-dta
              (extract-request-params context)
              ;;Stop chain
              (chain/terminate
               (assoc context
                      :response (gen-resp 400
                                          "Invalid parameters provided"))) ))
   :error (fn [context ex-info]
            (assoc context
                   :response (gen-resp 500
                                       (.getMessage ex-info))))})


;; -------------------------------------------------------------------------
;;
;; Interceptors for business logic
;; -------------------------------------------------------------------------
(def get-customer
  {:name ::customer-get

   :enter (fn [context]
            (file-logger (str "get-customer: using tx-data " (:tx-data context)))
            (let [tx-data (:tx-data context)
                  entity (.fetch customerdb (:id tx-data) (:flds tx-data))]
              (file-logger (str "get-customer: Customer entity found: " entity))
              (if (empty? entity)
                (assoc context :response (gen-resp 404
                                                   (str  "Customer with id '" (:id tx-data) "' was not found!")))
                (assoc context :response (gen-resp 200
                                                   (json/write-str entity)))) ))
   :error (fn [context ex-info]
            (assoc context
                   :response (gen-resp 500
                                       (json/write-str ex-info))))})

;;
;; For the POST routes
;; Modifies/updates a customer
(def update-customer
  {:name ::customer-update

   :enter (fn [context]
            (file-logger (str "update-customer: using tx-data " (:tx-data context)))
            (let [tx-data (:tx-data context)
                  id (:id tx-data)
                  db (.insert customerdb
                              id
                              (:name tx-data)
                              (:street tx-data)
                              (:housenumber tx-data)
                              (:city tx-data)
                              (:phone tx-data)
                              (:email tx-data))]
              (file-logger (str "Updated-customer: id = " id))
              (file-logger (str "update-customer: db = " @db))
              (if (nil? @db)
                (throw (IOException.
                        (str "Failed to find a customer with id '" id "'")))
                (assoc context
                       :response (gen-resp 200
                                           (json/write-str
                                             (.fetch customerdb id [])))))) )
   :error (fn [context ex-info]
            (assoc context 
                   :response (gen-resp 200
                                       (.getMessage ex-info))))})

;; For the PUT route
;;
;; Create or overwrite customer
;;
(def create-customer
  {:name ::customer-create

   :enter (fn [context]
            (file-logger (str "create-customer: using tx-data " (:tx-data context)))
            (let [tx-data (:tx-data context)
                  id (:id tx-data)
                  tx-data (if (or (nil? id) (empty? id))
                            (assoc tx-data :id (str (UUID/randomUUID)))
                            tx-data)
                  db (.insert customerdb
                              (:id tx-data)
                              (:name tx-data)
                              (:street tx-data)
                              (:housenumber tx-data)
                              (:city tx-data)
                              (:phone tx-data)
                              (:email tx-data))]
              (file-logger (str "create-customer: id = " (:id tx-data)))
              (file-logger (str "create-customer: db = " @db))
              (if (nil? @db)
                (throw (IOException. (str "Failed to create customer with id '" (:id tx-data) "'")))
                (assoc context
                       :response (gen-resp 200
                                           (json/write-str
                                             (.fetch customerdb (:id tx-data) [])))))))


   :error (fn [context ex-info]
            (assoc context
                   :response (gen-resp  500  
                                        (.getMessage ex-info))))})


;;
;; Delete a customer
;;
(def delete-customer
  {:name ::customer-delete

   :enter (fn [context]
            (file-logger (str "delete-customer: using tx-data " (:tx-data context)))
            (file-logger (str "create-customer:  id =  " (:id (:tx-data context))))
            (let [tx-data (:tx-data context)
                  id (:id tx-data)
                  db (.delete customerdb id)]
              (if (nil? db)
                (assoc context
                       :response (gen-resp 404
                                         (str "A customer with id " id " was not found!!") ))
                (assoc context
                       :response (gen-resp 200
                                           (str "Customer with id " id " was successfuly removed!"))))))
   :error (fn [context ex-info]
            (assoc context
                   :response (gen-resp 500
                                       (.getMessage ex-info))))})
