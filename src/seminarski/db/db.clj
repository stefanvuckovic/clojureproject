(ns seminarski.db.db
  (:require [monger.core :as mo]
            [monger.collection :as mcol]
            [monger.query :as mquery]
            [monger.operators :refer :all])
  (:import [com.mongodb MongoOptions ServerAddress]))
  

(comment (def connection 
           (let [^MongoOptions mongo-op (mo/mongo-options {:connections-per-host 25 :threads-allowed-to-block-for-connection-multiplier 100})
       ^ServerAddress sa  (mo/server-address "127.0.0.1" 27017)]
    (mo/connect sa mongo-op))))

(def connection (mo/connect {:host "127.0.0.1" :port 27017}))

(def db (mo/get-db connection "moviedatabase"))

  (defn insert [col doc] 
    (mcol/insert db col doc))    

  (defn insert-batch [col docs] 
  (mcol/insert-batch db col docs))  
  
  (defn get-data [col & {:keys [condition projection] :or {condition {} projection []}}]
    (mcol/find-maps db col condition projection))

(defn get-data-pagination [col page limit & {:keys [condition srt fields] :or {condition {} srt {} fields []}}]
  (mquery/with-collection db col
    (mquery/find condition)
      (mquery/fields fields)
    (mquery/sort srt)
    (mquery/paginate :page page :per-page limit)))

  (defn get-by-id [col id & {:keys [fields] :or {fields []}}]
    (let [id (String/valueOf id)]  
      (mcol/find-map-by-id db col (Integer/parseInt id) fields)))

  (defn update [col id data]
    (mcol/update db col {:_id id} {$set data}))

  (defn delete-data [col & {:keys [condition] :or {condition {}}}]
    (mcol/remove db col condition))
 
  (defn isempty [col] 
    (mcol/empty? db col))

  (defn disconnect []
    (mo/disconnect connection))

