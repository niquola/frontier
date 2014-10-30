(ns frontier.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [org.httpkit.server :as ohs]
            [ring.util.response :as rur]
            [hiccup.core :as hc]
            [hiccup.util :as hu]
            [frontier.projects :as fp]
            [frontier.views :as jv]
            [compojure.route :as route]))


(defn index [{params :params}]
  {:body (jv/index {:projects (fp/projects)})
   :status 200})

(defn new-project [{params :params}]
  (let [p (fp/build-project (:url params))]
    (rur/redirect (hu/url "/spa/progress/" p))))

(defn rebuild-progress [{params :params}]
  (let [p (fp/build-project (:name params))]
    (rur/redirect (hu/url "/spa/progress/" p))))

(defn project-progress-channel [{{nm :name} :params :as req}]
  (ohs/with-channel req ch
    (fp/watch-build nm ch)
    #_(ohs/on-receive ch println)
    (ohs/on-close ch (fn [& _] (fp/unwatch-build nm ch)))))

(defn watch-builds [req]
  (ohs/with-channel req ch
    (fp/watch-builds ch)
    #_(ohs/on-receive ch println)
    (ohs/on-close ch (fn [& _] (fp/unwatch-builds ch)))))

(defn project-progress [{params :params :as req}]
  (rur/response (jv/watch-build params)))

(defroutes app-routes
  (GET "/" [] #'index)
  (GET "/builds/channel" [] #'watch-builds)
  (GET "/spa/progress/:name/channel" [] #'project-progress-channel)
  (GET "/spa/progress/:name" [] #'project-progress)
  (POST "/spa" [] #'new-project)
  (POST "/spa/:name" [] #'rebuild-progress)
  (route/resources "/assets/")
  (route/not-found "Not Found"))

(def app
  (handler/site #'app-routes))

(defn start []
  (def stop
    (ohs/run-server #'app {:port 8080})))

(comment
  (require '[vinyasa.pull :as vp])
  (vp/pull 'hiccup-bridge)
  (vp/pull 'cheshire)
  (start)
  (stop))
