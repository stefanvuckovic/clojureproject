(ns seminarski.parser
  (:require [seminarski.getdatafromurl :as gd]
            [cheshire.core :refer :all :as ch]
            [clojure.java.io :as wr]))

(defn get-url-listmoviesapi [] 
  (str "https://yts.to/api/v2/list_movies.json")
)

(defn get-limit-listmoviesapi [] 
  50
)

(defn get-url-moviedetailsapi [] 
  (str "https://yts.to/api/v2/movie_details.json")
)

(defn get-movies-json-tag []
  [[:data :movies] []]
)

(defn get-movie-details-json-tag []
  [[:data] {}]
)

(defn json-parse [data [json-tag col]] 
 (let [mv (ch/parse-string data true)]
   (if(= (:status mv) "ok")
      (get-in mv json-tag) 
       col))
)

(defn get-movies-from-page [page] 
  (do
    (println (str (get-url-listmoviesapi) page))
    (gd/get-data (get-url-listmoviesapi) {"limit" (get-limit-listmoviesapi), "page" page}) )
)


(defn get-movie-details [movie]
  (gd/get-data (get-url-moviedetailsapi) {"movie_id" (:id movie), "with_cast" "true"})  
)






