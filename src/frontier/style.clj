(ns frontier.style)

(def main-style
  [:body {:padding 0}
   [:.navbar
    {:border-bottom "1px solid #ddd"}
    [:a { :font-weight "bold" :color "black"}]]
   [:.item {:border-bottom "1px solid #ddd"
            :padding "0.5em 0" }]
   [:tr.building
    {:background-color "#BFFF00"}]
   [:form {:margin 0}]
   [:pre.line
    {:border "none"
     :margin 0
     :font-size "14px"
     :color "#eee"
     :padding "2px 0"
     :border-radius 0
     :background-color "transparent"}]
   [:pre.line.err {:color "red"}]
   [:pre.line.block {:color "green" :padding "1em 0"}]
   [:.build {:background-color "black"
             :color "white"
             :padding "1em"}]])
