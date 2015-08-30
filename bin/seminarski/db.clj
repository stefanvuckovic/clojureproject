(ns seminarski.db
  
  (:require [monger.core :as mo]
            [monger.collection :as mcol]))
  

(def connection (mo/connect {:host "127.0.0.1" :port 27017}))

(def db (mo/get-db connection "moviedatabase"))

(defn insert [col doc] 
  (mcol/insert db col doc)                
)    

(defn insert-batch [col docs] 
  (mcol/insert-batch db col docs)                
)  
  
(defn get-data [col condition] 
  (if(nil? condition)
    (mcol/find-maps db col) 
    (mcol/find-maps db col condition))                               
)
 
(defn isempty [col] 
  (mcol/empty? db col)
)

(defn disconnect []
  (mo/disconnect connection)
)

