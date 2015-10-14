(ns seminarski.handler
  (:require [compojure.core :refer [defroutes routes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [seminarski.routes.movies :refer [movie-routes]]
            [seminarski.routes.home :refer [home-routes]]
            [seminarski.config.config :as conf]))

(defn init []
  (println "starting...")
  (conf/init)
  (println "finished initialization"))

(defn destroy []
  (println "shutting down..."))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes movie-routes app-routes)
      (handler/site)
      (wrap-base-url)))