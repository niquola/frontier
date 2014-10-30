(defproject frontier "0.1.0-SNAPSHOT"
  :description "build front"
  :url "??"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.19"]
                 [hiccup "1.0.5"]
                 [cheshire "5.3.1"]
                 [garden "1.2.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [compojure "1.1.9"]]
  :profiles
  {:dev {:dependencies [[im.chit/vinyasa "0.2.2"]
                        [hiccup-bridge "1.0.1"]
                        [javax.servlet/servlet-api "2.5"]]}})
