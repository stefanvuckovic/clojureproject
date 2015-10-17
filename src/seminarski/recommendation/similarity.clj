(ns seminarski.recommendation.similarity)

(defn get-cosine-similarity-values [val1 val2]
  {:dot (* val1 val2), :mag1 (Math/pow val1 2), :mag2 (Math/pow val2 2)})

(defn sum [map1 map2]
  (let [dotv1 (:dot map1) 
        dotv2 (:dot map2)
        mag1v1 (:mag1 map1)
        mag1v2 (:mag1 map2)
        mag2v1 (:mag2 map1)
        mag2v2 (:mag2 map2)]
    {:dot (+ dotv1 dotv2)
     :mag1 (+ mag1v1 mag1v2)
     :mag2 (+ mag2v1 mag2v2)}))

(defn calculate-parameters-for-cosine-similarity [v1 v2]
  (reduce sum 
          (map get-cosine-similarity-values v1 v2)))

(defn calculate-cosine-similarity [v1 v2]
  (let [{:keys[dot mag1 mag2]} (calculate-parameters-for-cosine-similarity v1 v2)]
    (if (or (= mag1 0.0) (= mag2 0.0))
      0
      (/ dot (* (Math/sqrt mag1) (Math/sqrt mag2))))))
