(ns sbm.routes
  (:use compojure.core
        [ring.middleware reload stacktrace]
        [hiccup.middleware :only (wrap-base-url)])
  (:require compojure.route
            compojure.handler
            sbm.views
            sbm.controllers
            ))

(defroutes main-routes
           (GET "/" [] (sbm.controllers/index))
           (GET "/search" params  (sbm.controllers/search  params))
           (GET "/show/:url" [url] (sbm.controllers/show url))
           (compojure.route/resources "/")
           (compojure.route/not-found "Page not found"))

(def app
  (-> (compojure.handler/site main-routes)
    (wrap-reload '[compojure.test.routes])
    (wrap-base-url)))
