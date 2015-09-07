(ns seminarski.db
  
  (:require [monger.core :as mo]
            [monger.collection :as mcol]
            [monger.query :as mquery]
            [monger.operators :refer :all]))
  

(def connection (mo/connect {:host "127.0.0.1" :port 27017}))

(def db (mo/get-db connection "moviedatabase"))

(defn insert [col doc] 
  (mcol/insert db col doc)                
)    

(defn insert-batch [col docs] 
  (mcol/insert-batch db col docs)                
)  
  
(defn get-data [col & {:keys [condition] :or {condition {}}}]
    (mcol/find-maps db col condition)                               
)

(defn get-data-pagination [col page limit & {:keys [condition srt] :or {condition {} srt {}}}]
  (mquery/with-collection db col
    (mquery/find condition)
    (mquery/sort srt)
    (mquery/paginate :page page :per-page limit)) 
)

(defn delete-data [col & {:keys [condition] :or {condition {}}}]
  (mcol/remove db col condition)
)
 
(defn isempty [col] 
  (mcol/empty? db col)
)

(defn disconnect []
  (mo/disconnect connection)
)

