(ns frontier.views
  (:require
    [garden.core :as gc]
    [frontier.style :as jc]
    [hiccup.page :as hp]
    [hiccup.form :as hf]
    [hiccup.core :as hc]))

(defn style [g]
  [:style (gc/css g)])

(defn js [pth]
  [:script {:src pth}])

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
      #_[:li.active [:a {:href "/"} "Frontier"]]
      cnt
      ]]])

(defn layout [title & cnt]
  (hc/html
    [:html
     [:head
      [:title title]
      (hp/include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")
      (style jc/main-style)]
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
               [:button.btn.btn-success.form-control "Save"]]])]
          [:table.table
           (for [p (:projects data)]
             [:tr.status {:id (str (:name p))}
              [:td [:a {:href (str "/assets/apps/" (:name p) "/dist/index.html")} [:b (:name p)]]]
              [:td [:form {:action (str "/spa/" (:name p)) :method :POST}
                    [:button.pull-right.btn.btn-success {:type :submit} "rebuild"]]]])]
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


