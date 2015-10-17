(ns seminarski.recommendation.tfidf
  (:require [opennlp.nlp :as nlp]
            [clj-fuzzy.stemmers :as stemmer]
            [clojure.string :as string]
            [clojure.set :as cljset]
            [seminarski.data.scraping :as scrape]
            [seminarski.config.settings :as settings]))

(def tokenizer (nlp/make-tokenizer "models/en-token.bin"))
(def name-finder (nlp/make-name-finder "models/en-ner-person.bin"))

(defn stem [word]
  (stemmer/porter word))

(defn get-stop-words [file] 
  (set (tokenizer (slurp file))))

(defn transform-to-lowercase [sentence] 
  (string/lower-case sentence))

(defn filter-stop-words [stop-word-list tokens]
  (remove stop-word-list tokens))

(defn filter-person-names [tokens]
  (remove 
   (set (name-finder tokens)) 
   tokens))

(defn process-single-document 
  "Preprocessing function for tfidf algorithm"
  [doc]
  (->> doc
       (tokenizer)
       (filter-person-names)
       (map transform-to-lowercase)
       (filter-stop-words (get-stop-words "util/stopwords.txt"))
       (map stem)
       (frequencies)))

(defn partial-process [doc]
  (->> doc
       (map transform-to-lowercase)
       (map stem)
       (frequencies)))

(defn calculate-tf-classic 
  "Standard formula for tf calculation"
  [frequence scale]
  (let [frequence (or frequence 0)]
    (/ frequence scale)))

(defn calculate-tf-log 
  "Logarithm formula for tf calculation"
  [frequence scale]
  (if frequence
    (+ 1 (Math/log10 frequence))
    0))

(defn calculate-tf-aug 
  "Augmentative formula for tf calculation"
  [frequence scale]
  (if frequence
    (+ 0.4 (* 0.6 (/ frequence scale)))
    0))

(defn calculate-idf [all-docs term]
  (Math/log10
    (/ (count all-docs) 
       (count (filter 
               #((:tokens %) term) 
               all-docs)))))

(defn get-all-terms 
  "Get corpus of terms from all documents"
  [all-docs]
  (reduce cljset/union #{} 
    (map #(set (keys (:tokens %))) all-docs)))

(defn get-all-terms-with-frequencies 
  "Get corpus of terms with frequences"
  [all-docs]
  (reduce 
   #(merge-with + %1 (:tokens %2)) 
   {} all-docs))

(defn calculate-idf-key-value 
  [all-docs result-map term]
  (assoc result-map term
    (calculate-idf all-docs term)))

(defn get-all-idfs 
  "Get all terms with idf values bound to them" 
  [all-docs all-terms]
  (reduce 
   (partial calculate-idf-key-value all-docs) 
   {} all-terms))

(defn calculate-tfidf 
  "Calculate tfidf value"
  [idf frequence scale tf-fn]
  (* (tf-fn frequence scale) idf))
 
(defn get-cast-list [casts]
  (reduce #(assoc %1 (:imdb_code %2) 1) {} casts))
 
 (defn add-key-value-pair 
   [significance weight-by result-map key-value]
   (let [[k v] key-value] 
     (assoc result-map k (* significance weight-by v))))
 
 (defn get-weighted-attributes 
   "Weighting term frequency with
   supplied weight factor"
   [weight-by terms significance]
   (reduce 
    (partial add-key-value-pair significance weight-by) 
    {} terms))
 
 (defn merge-vectors [& maps]
   (apply (partial merge-with +) maps))

 (defn create-vector-for-tfidf 
   "Complete preprocessing algorithm for tfidf"
   [movie]
   (let [description (process-single-document (:description movie))
         reviews (let [revs (:reviews movie)]
                   (if (empty? revs)
                     {}
                     (process-single-document 
                      (scrape/remove-stop-words 
                       (string/join " " (take 5 (:reviews movie)))))))
         base (merge-vectors description reviews)
         sum (reduce + (vals base))
         actors (get-weighted-attributes sum (get-cast-list (:actors movie)) 0.05)
         directors (get-weighted-attributes sum (get-cast-list (:directors movie)) 0.05)
         title (get-weighted-attributes sum (process-single-document (:title movie)) 0.03)
         genres (get-weighted-attributes sum (partial-process (:genres movie)) 0.05)
         final-vector (merge-vectors base actors directors title genres)]
     {:_id (:_id movie) :tokens final-vector}))

(defn get-preprocessed-data [data]
  (map create-vector-for-tfidf data))

(defn tfidf-for-movie 
  "Returning movie with map of terms and their tfidf values"
  [tf-fn scale vocabulary movie]
  (try
    (assoc-in movie [:tokens] 
      (into {}
        (map 
          (fn [[term freq]] 
            [term (calculate-tfidf 
                   (vocabulary term) freq scale tf-fn)])
          (:tokens movie))))
    (catch Exception e
      (.printStackTrace e))))

(defn tfidf-scale-by [scale-fn tf-fn vocabulary movie]
  "Calculating scale value needed in some tfidf formulas"
  (let [scale (reduce scale-fn (vals (:tokens movie)))
        scale-final (if(= 0 scale) 1 scale)]
    (tfidf-for-movie tf-fn scale-final vocabulary movie)))
 

(defn tfidf-for-all-without-cutoff 
  "Calculate tfidf values for all movies without cutoff" 
  [movies tfidf-fn]
  (let [all-terms (get-all-terms movies)
        idfs (get-all-idfs movies all-terms)
        tfidf-map (map (partial tfidf-fn idfs) movies)]
    (spit 
     (str "conf/tfidf" settings/tfidf-variation ".edn") 
     (prn-str tfidf-map))
    tfidf-map))

(defn remove-words [col words-to-remove]
  (apply dissoc col words-to-remove))

(defn sort-fn [map-to-sort]
  (into (sorted-map-by 
         (fn [key1 key2]
             (compare 
              [(get map-to-sort key1) key1]
              [(get map-to-sort key2) key2])))
         map-to-sort))

(defn tfidf-for-all-with-cutoff 
  "Calculate tfidf values for all movies with cutoff percentage" 
  [movies tfidf-fn cutoff]
  (let [all-terms (sort-fn (get-all-terms-with-frequencies movies))
        removal-percentage (Math/ceil (* cutoff (count all-terms)))
        words-to-remove (set (into (keys (take removal-percentage all-terms)) 
                                   (keys (take-last removal-percentage all-terms))))
        voc (cljset/difference (set (keys all-terms)) words-to-remove)
        movies-final (map #(update-in % [:tokens] remove-words words-to-remove) movies)
        idfs (get-all-idfs movies-final voc)
        tfidf-map (map (partial tfidf-fn idfs) movies-final)]
    (spit (str "conf/idf" cutoff ".edn") (prn-str idfs))
    (spit (str "conf/tfidf" settings/tfidf-variation ".edn") (prn-str tfidf-map))
    tfidf-map))

(defmulti tfidf-for-all 
  (fn [movies tfidf-fn cutoff]
    cutoff))

(defmethod tfidf-for-all 0.0
  [movies tfidf-fn cutoff] 
  (tfidf-for-all-without-cutoff movies tfidf-fn))

(defmethod tfidf-for-all :default
  [movies tfidf-fn cutoff] 
  (tfidf-for-all-with-cutoff movies tfidf-fn cutoff))

(defmulti calculate-tfidf-for-all 
  (fn [movies variation cutoff]
    variation))

(defmethod calculate-tfidf-for-all :classic
  [movies variation cutoff] 
  (tfidf-for-all 
   movies 
   (partial tfidf-scale-by + calculate-tf-classic) 
   cutoff))

(defmethod calculate-tfidf-for-all :aug
  [movies variation cutoff] 
  (tfidf-for-all 
   movies 
   (partial tfidf-scale-by max calculate-tf-aug) 
   cutoff))

(defmethod calculate-tfidf-for-all :log
  [movies variation cutoff] 
  (tfidf-for-all
   movies 
   (partial tfidf-for-movie calculate-tf-log 0) 
   cutoff))



