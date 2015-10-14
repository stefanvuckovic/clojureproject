(ns seminarski.routes.movies
  (:require [compojure.core :refer :all]
            [seminarski.views.layout :as layout]
            [seminarski.controller.controller :as controller]
            [clojure.string :as cs]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [hiccup.page :refer :all]
            [seminarski.config.settings :as settings]))


(defn get-base-template [] 
  [:nav.navbar.navbar-inverse.navbar-fixed-top
    [:div.container
      [:div.navbar-header
        (link-to {:class "navbar-brand"} "/movies" "Home")]
      [:div#navbar.navbar-collapse.collapse
        [:div.navbar-form.navbar-right
          (text-field {:class "form-control" :placeholder "Search..."} :search)]]]])

(defn display-start-page []
  (layout/template
    ["/css/movies.css"]
    ["/js/movies.js"]
    
    (get-base-template)
    
    [:div.jumbotron 
      [:div.container
       [:h1 "Movie database"]
       [:p "Here you can browse movies and get advanced movie recommendation"]]]
    
    [:div#movies.container]))

(defn get-paginator [page title] 
  (remove nil? 
    (list
      [:ul.pager
      (if (> page 1)
        [:li (link-to {:align "left", :onclick (str "getMovies(" (- page 1) (if-not (nil? title) (str ",\"" title "\"") "") ")")} "javascript:void(0);" "PREVIOUS")])
      " "
      [:li (link-to {:align "left", :onclick (str "getMovies(" (+ page 1) (if-not (nil? title) (str ",\"" title "\"") "") ")")} "javascript:void(0);" "NEXT")]])))


(defn display-movies2 [page title]
  (let [page (Integer/parseInt page)
        movies (controller/get-movies-from-db page 12 title nil)]
    (layout/to-html
      (for [mv movies]
          [:div.col-md-4
           [:div.panel.panel-default
            [:div.panel-heading
             [:h4
              [:i.fa.fa-fw.fa-check]
              (:title mv)]]
            [:div.panel-body
             [:div.row
             [:div.col-md-4 
              [:p "Year: "]]
             [:div.col-md-8
              [:p (:year mv)]]]
             [:div.row
             [:div.col-md-4 
              [:p "Rating: "]]
             [:div.col-md-8
              [:p (:imdb_rating mv)]]]
             [:div.row
             [:div.col-md-4 
              [:p "Genre: "]]
             [:div.col-md-8
              [:p (apply str (interpose ", " (:genres mv)))]]]
             (link-to {:class "btn btn-sm btn-primary"} (str "/movie/" (:_id mv)) "Details")]]])
      [:br]
      [:div (get-paginator page title)])))

(defn display-movies [page title]
  (let [page (Integer/parseInt page)
        movies (controller/get-movies-from-db page 12 title nil)
        grouped-movies (partition-all 3 movies)]
    (layout/to-html
      
      (for [three-movies grouped-movies]
        [:div.row
        (for [mv three-movies]
          [:div.col-md-4
           [:div.panel.panel-default
            [:div.panel-heading
             [:h4
              [:i.fa.fa-fw.fa-check]
              (:title mv)]]
            [:div.panel-body
             [:div.row
             [:div.col-md-4 
              [:p "Year: "]]
             [:div.col-md-8
              [:p (:year mv)]]]
             [:div.row
             [:div.col-md-4 
              [:p "Rating: "]]
             [:div.col-md-8
              [:p (:imdb_rating mv)]]]
             [:div.row
             [:div.col-md-4 
              [:p "Genre: "]]
             [:div.col-md-8
              [:p (apply str (interpose ", " (:genres mv)))]]]
             (link-to {:class "btn btn-sm btn-primary"} (str "/movie/" (:_id mv)) "Details")]]])])
      [:br]
      [:div (get-paginator page title)])))

(defn get-cast-names [casts]
  (apply str (interpose ", " (map :name casts))))


(defn show-similar-movies [movie] 
  (let [grouped-movies (partition-all 3 ((keyword settings/similarity-db-field) movie))]
    (for [three-movies grouped-movies]
      [:div.row
      (for [mv three-movies]
        (let [sm (controller/get-similar-movie (:_id mv))]
        [:div.col-md-4
          [:div.panel.panel-default
            [:div.panel-heading
              [:h4
                [:i.fa.fa-fw.fa-check]
                (:title sm)]]
            [:div.panel-body
              [:div.row
                [:div.col-md-4 
                  [:p "Rating: "]]
                [:div.col-md-8
                  [:p (:imdb_rating sm)]]]
              [:div.row
                [:div.col-md-4 
                  [:p "Genre: "]]
                [:div.col-md-8
                  [:p (apply str (interpose ", " (:genres sm)))]]]
              (link-to {:class "btn btn-sm btn-primary"} (str "/movie/" (:_id sm)) "Details")]]]))])))


(defn display-movie-details [id]
  (let [movie (controller/get-movie-by-id id)]
    (layout/template
      ["/css/movies.css"]
      nil
      (let [base-template (get-base-template)
            base-without-search (subvec (get-in base-template [1]) 0 2)]
          (conj (subvec base-template 0 1) base-without-search))
      [:div.container
        [:div.jumbotron.movie-details 
          [:h2 (:title movie)]
          [:hr]
          [:p.description (:description movie)]
          [:div.row
            [:div.col-md-2 
              [:p "Year: "]]
            [:div.col-md-10
              [:p (:year movie)]]]
          [:div.row
            [:div.col-md-2 
              [:p "Rating: "]]
            [:div.col-md-10
              [:p (:imdb_rating movie)]]]
          [:div.row
            [:div.col-md-2 
              [:p "Genre: "]]
            [:div.col-md-10
              [:p (apply str (interpose ", " (:genres movie)))]]]
          [:div.row
            [:div.col-md-2 
              [:p "Directors: "]]
            [:div.col-md-10
              [:p (get-cast-names (:directors movie))]]]
          [:div.row
            [:div.col-md-2 
              [:p "Actors: "]]
            [:div.col-md-10
              [:p (get-cast-names (:actors movie))]]]]
        
        [:hr]
        [:h2 "Similar movies"]
        (show-similar-movies movie)])))


(defroutes movie-routes 
  (GET "/movies" [] (display-start-page))
  (GET "/movie/:id" [id] (display-movie-details id))
  (GET "/pagination" [page title] (display-movies page title)))

