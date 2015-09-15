(ns seminarski.routes.home
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [seminarski.views.layout :as layout]
            [seminarski.controller :as controller]
            [clojure.string :as cs]
            [hiccup.form :refer :all]
            [hiccup.element :refer :all]
            [hiccup.page :refer :all]))

(defroutes home-routes 
  (GET "/" [] (redirect "/movies"))
)