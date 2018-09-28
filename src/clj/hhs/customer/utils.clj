(ns hhs.customer.utils
  (:require [clojure.tools.logging :as log]))

(defn print-logger
  "Accepts a an instance of java.io.writer for writing charachter 
   data to a device and returns a function that binds *out* with the writer
   and then prints out a single argument"
  [writer]
  #(binding [*out* writer]
     (println %)))


(defn file-logger
  "Accepts a file name, creates a new writer for appending and 
   returns a function which writes to the file"
  [file]
  #(with-open [f (clojure.java.io/writer file :append true)]
     ((print-logger f) %)))
