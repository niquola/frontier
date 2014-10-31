(ns frontier.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [org.httpkit.server :as ohs]
            [org.httpkit.client :as ohc]
            [ring.util.response :as rur]
            [hiccup.core :as hc]
            [hiccup.util :as hu]
            [cheshire.core :as json]
            [frontier.projects :as fp]
            [frontier.views :as jv]
            [compojure.route :as route]))


(defn index [{params :params}]
  {:body (jv/index {:projects (fp/projects)})
   :status 200})

(def auth-conf
  {:client_id "8f769967e01d7cfa2c2e"
   :redirect_uri "http://172.17.0.14:8080/oauth"
   :state "yx5k5oog43"
   :scope "user,gist"
   :client_secret "dc0264ce52582fdab09099561dfe171ea3392f69"})

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


(defn get-access-token [code cb]
  (ohc/post
    "https://github.com/login/oauth/access_token"
    {:headers  {"Accept" "application/json"}
     :form-params (merge auth-conf {:code code })}
    cb))

(defn load-user-info [a-token cb]
  (ohc/get
    (str (hu/url "https://api.github.com/user" {:access_token a-token}))
    {:headers  {"Accept" "application/json" }}
    cb))

(defn auth [{{_ :state code :code} :params :as req}]
  (ohs/with-channel req ch
    (get-access-token
      code
      (fn [{bd :body}]
        (let [resp (json/parse-string bd keyword)
              a-token (:access_token resp)]
          (when a-token
            (load-user-info
              a-token
              (fn [info]
                (println info)
                (ohs/send! ch (rur/response (pr-str (:body info))))))))))))

(defroutes app-routes
  (GET "/" [] #'index)
  (GET "/builds/channel" [] #'watch-builds)
  (GET "/spa/progress/:name/channel" [] #'project-progress-channel)
  (GET "/spa/progress/:name" [] #'project-progress)
  (GET "/oauth" [] #'auth)
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
