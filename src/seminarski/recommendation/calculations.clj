(ns seminarski.calculations
  (:require [seminarski.tfidf :as tfidf]
            [seminarski.similarity :as sim]
            [seminarski.db :as db]
            [clojure.set :as set]
            [seminarski.settings :as settings]))

(defn get-db-field-for-similarities []
  "similar")

(defn get-similarity-score 
  ([movie1 movie2]
    {:_id (:_id movie2)
     :score (sim/calculate-cosine-similarity (:tokens movie1) (:tokens movie2))})
  ([process-fn movie1 movie2]
    (if-let [[mv1 mv2] (process-fn movie1 movie2)]
      (get-similarity-score mv1 mv2))))

(defn create-vector-from-vocabulary [movie vocabulary]
  (replace {nil 0} (map (:tokens movie) vocabulary)))

(defn prepare-vectors [movie1 movie2]
  (if (not= (:_id movie1) (:_id movie2))
    (let [terms (set/union (set (keys (:tokens movie1))) (set (keys (:tokens movie2))))
          v1 (assoc-in movie1 [:tokens] (create-vector-from-vocabulary movie1 terms))
          v2 (assoc-in movie2 [:tokens] (create-vector-from-vocabulary movie2 terms))]
      [v1 v2])))

(defn remove-nils [vec]
  (into [] (remove nil? vec)))


(defn get-similarities-for-movie [movies movie]
  {:_id (:_id movie) 
   :similar
      (into [] (take 10 (sort-by :score > (remove nil? (map (partial get-similarity-score prepare-vectors movie) movies)))))})

(defn save-recommendations [movie]
  (try
      (db/update "movies" (:_id movie) {(keyword settings/similarity-db-field) (:similar movie)})
  (catch Exception e
     (.printStackTrace e))))

(defn cosine-similarity-for-all2 "Calculate similarities for every two movies" 
  ([movies]
    (cosine-similarity-for-all2 movies movies))
  ([movies movie-chunk]
    (map #(get-similarities-for-movie movies %) movie-chunk)))


(defn cosine-similarity-for-all "Calculate similarities for every two movies" 
  ([movies]
    (cosine-similarity-for-all movies movies))
  ([movies movie-chunk]
    (doseq [movie movie-chunk]
       (save-recommendations (get-similarities-for-movie movies movie)))))

(defn cosine-similarity-for-all-parallel "Calculate similarities for every two movies" [movies]
  (dorun (pmap #(cosine-similarity-for-all movies %) (partition-all 65 movies))))

(defn cosine-similarity-for-all-parallel2 "Calculate similarities for every two movies" [movies]
  (pmap #(cosine-similarity-for-all2 movies %) (partition-all 65 movies)))


(defn calculate-cosine-similarities [calculation-fn processed-movies processed-vocabulary]
  (calculation-fn
    (tfidf/calculate-tfidf-for-all processed-movies processed-vocabulary (keyword settings/tfidf-variation))))


(defn calculate-similarities 
  ([movies] 
    (let [processed-data (tfidf/get-preprocessed-data movies)]
      (calculate-cosine-similarities cosine-similarity-for-all-parallel processed-data processed-data)))
  ([movies vocabulary]
    (calculate-cosine-similarities cosine-similarity-for-all-parallel 
                                   (tfidf/get-preprocessed-data movies) 
                                   (tfidf/get-preprocessed-data vocabulary))))


(defn tfidf []
  (let [processed-data (tfidf/get-preprocessed-data (db/get-data "movies"))]
    (tfidf/calculate-tfidf-for-all processed-data processed-data)))

(defn test-preprocess []
  (tfidf/get-preprocessed-data (db/get-data-pagination "movies" 1 10 nil nil)))
 
