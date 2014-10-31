(ns frontier.views
  (:require
    [garden.core :as gc]
    [frontier.style :as jc]
    [hiccup.page :as hp]
    [hiccup.form :as hf]
    [hiccup.util :as hu]
    [hiccup.core :as hc]))

(defn style [s] [:style s])

(defn js [pth] [:script {:src pth}])

(defn gh-auth []
  (hu/url "https://github.com/login/oauth/authorize"
          {:client_id "8f769967e01d7cfa2c2e"
           :redirect_uri "http://172.17.0.14:8080/oauth"
           :scope "user,gist"
           :state "yx5k5oog43"}))

(defn menu [& cnt]
  [:nav.navbar {:role "navigation"}
   [:div.navbar-header
    [:button.navbar-toggle.collapsed {:aria-controls "navbar", :aria-expanded "false", :data-target "#navbar", :data-toggle "collapse", :type "button"}
     [:span.sr-only "Toggle navigation"]
     [:span.icon-bar]
     [:span.icon-bar]
     [:span.icon-bar]]
    [:a.navbar-brand {:href "/"} "Frontier"]]
   [:div#navbar.collapse.navbar-collapse
    [:ul.nav.navbar-nav
     [:li.active [:a {:href (gh-auth)} "login"]]
     cnt
     ]]])

(defn layout [title & cnt]
  (hc/html
    [:html
     [:head
      [:title title]
      (hp/include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")
      (hp/include-css "/assets/app.css")
      (style (jc/main-style))]
     [:body
      (menu #_[:li [:a "ups"]])
      [:div.container-fluid
       cnt]]]))

(defn index [data]
  (layout "Frontier"
          [:div.well
           (hf/form-to
             [:post "/spa"]
             [:div.row
              [:div.col-xs-10 (hf/text-field {:class "form-control" :placeholder "github repo url" :required true} :url)]
              [:div.col-xs-2
               [:button.btn.btn-default.form-control "Save"]]])]
          [:table.table
           (for [p (:projects data)]
             [:tr.status {:id (str (:name p))}
              [:td [:a {:href (str "/assets/apps/" (:name p) "/dist/index.html")}
                    [:div.spinner]
                    [:b (:name p)]]]
              [:td [:form {:action (str "/spa/" (:name p)) :method :POST}
                    [:button.pull-right.btn.btn-success.btn-sm {:type :submit} "rebuild"]]]])]
          [:div#builds]
          (js "/assets/index.js")
          ))

(defn watch-build [{nm :name}]
  (layout (str "Building " nm)
          [:div#out.build]
          (js "/assets/watch.js")))

(comment
  (require '[hiccup-bridge.core :as hbc])
  (hbc/html->hiccup " "))


