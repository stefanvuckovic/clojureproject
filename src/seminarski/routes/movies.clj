(ns seminarski.routes.movies
  (:require [compojure.core :refer :all]
            [seminarski.views.layout :as layout]
            [seminarski.controller :as controller]
            [clojure.string :as cs]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [hiccup.page :refer :all]))

(defn display-movies []
  (let [movies (controller/get-movies-from-db 1 20 nil nil)]
    [:div
    [:h1 "Movies"]
    [:table
      [:tr [:th "Title"] [:th "Year"] [:th "Genre"] [:th "IMDB Rating"]]
      (for [mv movies]
        [:tr [:td (link-to {:align "left"} (str "/movie/" (:_id mv)) (:title mv))] [:td (:year mv)] [:td (:genres mv)] [:td (:imdb_rating mv)]])]])
)

(defn display-movie-details [id]
  (let [movie (controller/get-movie-by-id id)]
    (layout/template
      [:p (str "Title" (:title movie))]
      [:p (str "Year" (:year movie))]
      [:p (str "Genre" (:genres movie))]
      [:p (str "IMDB Rating" (:imdb_rating movie))]
      [:p (str "Description" (:description movie))]
      [:p (str "Directors" (:directors movie))]
      [:p (str "Actors" (:actors movie))]))
)

(defn display-browse-movies-page []
  (layout/template
    (label :search "Browse movies")
    (text-field :search)
    (display-movies))
)

(defroutes movie-routes 
  (GET "/movies" [] (display-browse-movies-page))
  (GET "/movie/:id" [id] (display-movie-details id))
)

