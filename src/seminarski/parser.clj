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


(defn get-moviecast-data [cast]
  (vec (doall (map #(select-keys % [:name :imdb_code]) cast)))
)

(defn get-parsed-movie [movie]
  {:_id (:id movie)
   :imdb_code (:imdb_code movie)
   :title (:title_long movie)
   :year (:year movie)
   :imdb_rating (:rating movie)
   :genres (:genres movie)
   :yt_trailer_code (:yt_trailer_code movie)
   :description (:description_full movie)
   :directors (get-moviecast-data (:directors movie))
   :actors (get-moviecast-data (:actors movie))
   }
)


(defn get-processed-movies [movies]
  (loop [mv movies mvfrompage []]
    (if (empty? mv)
      mvfrompage
      (let [mvfp (get-parsed-movie(json-parse (get-movie-details (first mv)) (get-movie-details-json-tag)))]
        (if-not (nil? mvfp)
          (recur (rest mv) (conj mvfrompage mvfp))
          (recur (rest mv) mvfrompage)))))
)

(defn get-movies []
  (loop [page 1 movies []]
    (let [moviesfrompage (json-parse (get-movies-from-page page) (get-movies-json-tag))]
      (if-not (nil? moviesfrompage)
        (if (empty? moviesfrompage)
          (do 
            (println "EMPTY")
            movies)
          (do
          
          (recur (inc page) (into movies (get-processed-movies moviesfrompage)))))
        (recur (inc page) movies))))
)









