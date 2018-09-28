(ns hhs.customer.core-test
  (:require [hhs.customer.core]))

;; Pedestal request map structure
;; 
;; protoco.://subdomain.domain.com:port/path?query
;;
;; query = patameter1=value&param2=value2
;;
(def pedestal-request
  {:headers {}     ;; Map - Request headers sent by the client. Header names are lower case
   :params {}      ;; Map - Merged map of path, query, and request parameters.
   :path-info ""   ;; String - Request path, below the context path.
                   ;; Always at least "/", never  empty string.

   ;; Map - Present if the router found any path parameters.
   :path-params { :pathparam1 "customer"
                  :pathparam2 "name"} 
   
   ;; Present if the query-params interceptor is used.
   ;; (It is one of the default interceptors.)
   :query-params {:query-param1 "101"
                  :query-param2 "email"
                  :flds "id,name,email"
                  :id 101} 
   :query-string "" ;; String - The part of the request’s URL after the '?' character.

   ;; Map - Present if the body-params interceptor is used and
   :form-params {:form-param1 "Book"
                 :form-param2 "Tablet"} 
                       ;; the client sent content type "application/x-www-form-urlencoded". 
   :edn-params {}      ;; Map - Present if the body-params interceptor is used and
                       ;; the client sent content type "application/edn" 
   })

(def ctx  {:request {:headers {}
                     :params {}
                     :path-info ""
                     :path-params {:pathparam1 "customer"
                                   :pathparam2 "name"}
                     :query-params {:query-param1 "101"
                                    :query-param2 "email"
                                    :flds "id,name,email"
                                    :id 101}
                     :query-string ""
                     :form-params {:form-param1 "Book"
                                   :form-param2 "Tablet"}
                     :edn-params {}}
           :tx-data {:form-param1 "Book"
                     :form-param2 "Tablet"
                     :query-param1 "101"
                     :query-param2 "email"
                     :id 101
                     :pathparam1 "customer"
                     :pathparam2 "name"
                     :flds []}})

(def s "id=101,name=peter,address=zugerstrasse")

(-> s
    (clojure.string/split #",")
    (map clojure.string/trim) )

(map clojure.string/trim (clojure.string/split s #","))

(vector (map clojure.string/trim (clojure.string/split s #",")) )




