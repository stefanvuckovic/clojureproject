(ns seminarski.recommendation.calculations
  (:require [seminarski.recommendation.tfidf :as tfidf]
            [seminarski.recommendation.similarity :as sim]
            [seminarski.db.db :as db]
            [clojure.set :as set]
            [seminarski.config.settings :as settings]))

(defn get-similarity-score 
  ([movie1 movie2]
   {:_id (:_id movie2)
    :score (sim/calculate-cosine-similarity 
            (:tokens movie1) 
            (:tokens movie2))})
  ([process-fn movie1 movie2]
   (if-let [[mv1 mv2] (process-fn movie1 movie2)]
     (get-similarity-score mv1 mv2))))

(defn create-vector-from-vocabulary 
  [movie vocabulary]
  (replace {nil 0} (map (:tokens movie) vocabulary)))

(defn prepare-vectors 
  "Preparing vectors for computing cosine similarity.
  Vectors should be of same length and with values in
  the corresponding positions"
  [movie1 movie2]
  (if (not= (:_id movie1) (:_id movie2))
    (let [terms (set/union 
                 (set (keys (:tokens movie1))) 
                 (set (keys (:tokens movie2))))
          v1 (assoc-in movie1 
                       [:tokens] 
                       (create-vector-from-vocabulary movie1 terms))
          v2 (assoc-in movie2 
                       [:tokens] 
                       (create-vector-from-vocabulary movie2 terms))]
      [v1 v2])))

(defn remove-nils [vec]
  (into [] (remove nil? vec)))

(defn get-similarities-for-movie 
  "Compute similarity between given movie and all other 
  movies in a corpus, then return top 10 similar movies"
  [movies movie]
  {:_id (:_id movie) 
   :similar
    (into 
     [] 
     (take 10 
           (sort-by 
            :score 
            > 
            (remove nil? 
             (map 
              (partial get-similarity-score 
                       prepare-vectors movie) 
              movies)))))})

(defn save-recommendations [movie]
  (try
    (db/update "movies" 
               (:_id movie) 
               {(keyword settings/similarity-db-field) 
                (:similar movie)})
    (catch Exception e
      (.printStackTrace e))))

(defn cosine-similarity-for-all 
  "Calculate similarities between each two movies" 
  ([movies]
   (cosine-similarity-for-all movies movies))
  ([movies movie-chunk]
   (doseq [movie movie-chunk]
     (save-recommendations 
      (get-similarities-for-movie movies movie)))))

(defn cosine-similarity-for-all-parallel 
  "Calculate similarities between each two movies in parallel" 
  [movies]
  (dorun (pmap 
          #(cosine-similarity-for-all movies %) 
          (partition-all 65 movies))))


(defn calculate-cosine-similarities 
  "Get tfidf values for all movies and apply cosine 
  similarity function to the results"
  [calculation-fn processed-data]
  (calculation-fn
    (tfidf/calculate-tfidf-for-all 
     processed-data 
     (keyword settings/tfidf-variation) 
     settings/cutoff)))


(defn calculate-similarities 
  "Takes preprocessed data and specifies function
  for calculating cosine similarity (parallel or
  parallel or sequential"
  [movies] 
  (calculate-cosine-similarities 
   cosine-similarity-for-all-parallel 
   (tfidf/get-preprocessed-data movies)))


(defn tfidf []
  (try
  (let [processed-data (tfidf/get-preprocessed-data (db/get-data-pagination "movies" 1 3 nil nil))]
    (tfidf/calculate-tfidf-for-all processed-data :classic 0.0))
  (catch Exception e
      (do
        (.printStackTrace e)))))

(defn test-preprocess []
  (tfidf/get-preprocessed-data (db/get-data-pagination "movies" 1 10 nil nil)))
 
