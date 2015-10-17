(ns seminarski.controller.controller
  (:require [seminarski.db.db :as db]
            [seminarski.recommendation.calculations :as calc]
            [monger.operators :refer :all]))


(defn get-movies-from-db [page limit title sort]
  (let [condition 
        (if-not (nil? title)
          {:title {$regex (str ".*" title ".*") $options "i"}}
          title)]
    (db/get-data-pagination 
     "movies" 
     page 
     limit 
     :condition condition 
     :srt sort 
     :fields ["title" "year" "genres" "imdb_rating"])))

(defn get-movie-by-id [id]
  (let [mv (db/get-by-id "movies" id)]
    (do
      (println (str "ID " id))
      (println (str "Naslov " (:title mv)))
      mv)))

(defn get-similar-movie [id]
  (db/get-by-id 
   "movies" 
   id 
   :fields ["title" "genres" "imdb_rating"]))
 



