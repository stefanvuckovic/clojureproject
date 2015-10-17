(ns seminarski.data.parser
  (:require [seminarski.data.getdatafromurl :as gd]
            [cheshire.core :refer :all :as ch]))

(defn get-url-listmoviesapi [] 
  (str "https://yts.to/api/v2/list_movies.json"))

(defn get-limit-listmoviesapi [] 
  50)

(defn get-url-moviedetailsapi [] 
  (str "https://yts.to/api/v2/movie_details.json"))

(defn get-url-moviereviewsapi []
  (str "https://yts.to/api/v2/movie_reviews.json"))


(defn get-movies-json-tag []
  [:data :movies])


(defn get-movie-details-json-tag []
  [:data])

(defn get-reviews-json-tag []
  [:data :reviews])

(defn json-parse [data json-tag]
  (if-not (= data nil)
    (let [mv (ch/parse-string data true)]
      (if(= (:status mv) "ok")
        (get-in mv json-tag)))))

(defn get-movies-from-page [page] 
  (gd/get-data 
   (get-url-listmoviesapi) 
   {"limit" (get-limit-listmoviesapi), 
    "page" page}))


(defn get-movie-details [movie]
  (gd/get-data 
   (get-url-moviedetailsapi) 
   {"movie_id" (:id movie), "with_cast" "true"}))

(defn get-movie-reviews-from-api [movie-id]
  (gd/get-data 
   (get-url-moviereviewsapi) 
   {"movie_id" movie-id}))

(defn get-parsed-movie-reviews [movie-id]
  (let [reviews (get-movie-reviews-from-api movie-id)]
    (if (= reviews nil)
      (throw (Exception. "Error"))
      (json-parse 
       (get-movie-reviews-from-api movie-id) 
       (get-reviews-json-tag)))))

(defn get-movie-reviews [movie-id]
  (reduce 
   #(conj %1 (:review_text %2)) 
     [] 
     (get-parsed-movie-reviews movie-id))) 

(defn get-moviecast-data [cast]
  (vec (doall (map #(select-keys % [:name :imdb_code]) cast))))

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
   })


(defn get-processed-movies [movies]
  (loop [mv movies mvfrompage []]
    (if (empty? mv)
      mvfrompage
      (if-let [mvfp 
               (json-parse 
                (get-movie-details (first mv)) 
                (get-movie-details-json-tag))]
        (recur (rest mv) (conj 
                          mvfrompage 
                          (get-parsed-movie mvfp)))
        (recur (rest mv) mvfrompage)))))

(defn get-movies []
  (loop [page 1 movies []]
    (if-let [moviesfrompage 
             (json-parse 
              (get-movies-from-page page) 
              (get-movies-json-tag))]
      (if (empty? moviesfrompage)
        movies
        (recur (inc page) 
               (into movies 
                     (get-processed-movies 
                      moviesfrompage))))
      (recur (inc page) movies))))







