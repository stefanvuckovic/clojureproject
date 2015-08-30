(ns seminarski.db
  
  (:require [monger.core :as mo]))
  

(def connection (mg/connect {:host "127.0.0.1" :port 27017}))

(def db (mg/get-db connection "database"))

(defn insert [col doc] 
  (mo/insert db col doc)                
)    

(defn insert-batch [col docs] 
  (mo/insert-batch db col docs)                
)  
  
(defn get-data [col condition] 
  (if(nil? condition)
    (mo/find-maps db col) 
    (mo/find-maps db col condition))                               
)
 
(defn isempty [col] 
  (mo/empty? db col)
)

