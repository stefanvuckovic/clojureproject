(ns seminarski.recommendation.t_calculations
  (:require [midje.sweet :refer :all]
            [seminarski.recommendation.calculations :refer :all]))

(fact "Testing vector normalization and preparation for cosine similarity measure"
  (prepare-vectors 
   {:_id 1 
    :tokens {"term1" 1 "term2" 2}}
   {:_id 2 
    :tokens {"term2" 1.5 "term3" 2.5}}) 
  => [{:_id 1 
       :tokens [2 1 0]}
      {:_id 2
       :tokens [1.5 0 2.5]}])

                                                    

