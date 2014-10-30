(ns frontier.projects
  (:require
    [clojure.string :as cs]
    [clojure.java.io :as io]
    [org.httpkit.server :as ohs]
    [hiccup.core :as hc]
    [frontier.bash :as fb]
    [cheshire.core :as cc]
    [clojure.core.async :as a])
  (:import java.io.BufferedReader
           java.io.InputStreamReader))

(def log-level :builds)
(defn log [tags & msg]
  (when log-level
    (when (and (keyword? log-level) (contains? tags log-level))
      (apply println tags ":" msg))))

(def base-dir (.getPath (io/resource "public/apps")))

(defn url-to-name [url]
  (-> url
      (cs/split #"/") last
      (cs/split #"\.") first))

(defn project-path [nm]
  (str base-dir "/" nm))

(defn projects []
  (->> (io/file base-dir)
       (.listFiles)
       (filter #(.isDirectory %))
       (map (fn [x] {:name (.getName x)}))))

(defn project-exists? [nm]
  (-> nm project-path io/file .exists))


(defn docker-run [nm]
  [:sudo :docker.io :run :--rm
   {:-v (str (project-path nm) ":/project")}
   "fbuildr"])

(defn build-new-cmd [nm url]
  (fb/shell
    [:and
     [:cd base-dir]
     [:sudo :rm :-rf nm]
     [:git :clone url nm]
     [:cd nm]
     (docker-run nm)]))

(defn build-cmd [nm]
  (fb/shell
    [:and
     [:cd (project-path nm)]
     [:git :pull]
     [:ls :-lah]
     (docker-run nm)]))

(defn buf-read [s]
  (-> s (InputStreamReader.) (BufferedReader.)))

(defn fmt-line [s]
  (hc/html [:pre.line  s]))

(defn fmt-block [s]
  (hc/html [:pre.line.block s]))

(defn fmt-error-line [s]
  (hc/html [:pre.line.err s]))

(defn stream-to-channel [ch st wrap]
  (a/thread
    (when-let [s (.readLine st)]
      (a/put! ch (wrap s))
      (recur))))

(defn cmd [ch c]
  (log #{:build} "Execute command" c)
  (let
    [proc (.exec (Runtime/getRuntime)
                 ^"[Ljava.lang.String;"  (into-array c))
     inp  (buf-read (.getInputStream proc))
     err  (buf-read (.getErrorStream proc))]
    (stream-to-channel ch inp fmt-line)
    (stream-to-channel ch err fmt-error-line)
    (a/thread (.waitFor proc) (.exitValue proc))))


(def current-builds (atom {}))

(def builds-watchers (atom #{}))

(defn notify-builds []
  (let [bld @current-builds
        vs (map (fn [m] (select-keys m [:name :url])) (vals bld))
        json (cc/encode vs)]
    (log #{:builds :ws} "NOTIFY: " (pr-str vs))
    (doseq [x @builds-watchers]
      (ohs/send! x (str json)))))

(defn watch-builds [soc]
  (log #{:builds :ws} "Watch builds " soc)
  (swap! builds-watchers conj soc)
  (notify-builds))

(defn unwatch-builds [soc]
  (log #{:builds :ws} "Unwatch builds " soc)
  (swap! builds-watchers disj soc))


(defn read-ch [ch f & [end-f]]
  (a/go-loop
    []
    (if-let [msg (a/<! ch)]
      (do (f msg) (recur))
      (when end-f (end-f)))))



(defn rm-build [nm]
  (swap! current-builds dissoc nm))

(defn get-build [nm]
  (get @current-builds nm))

(defn build-log [nm msg]
  (swap! current-builds
         (fn [bs]
           (when (contains? bs nm)
             (update-in bs [nm :log] conj msg)))))

(defn mk-build [nm url ch]
  (swap! current-builds
         (fn [bs]
           (let [pub (a/pub ch (fn [_] :topic))
                 log (a/chan)]
             (a/sub pub :topic log)
             (read-ch log #(build-log nm %))
             (assoc bs nm
                    {:url url
                     :name nm
                     :log []
                     :pub pub})))))

(defn build-project [url]
  (if-not (get-build (url-to-name url))
    (let [nm     (url-to-name url)
          ch     (a/chan)
          log    (a/chan)]
      (mk-build nm url ch)
      (a/go
        (notify-builds)
        (let [script (if (project-exists? nm) (build-cmd nm) (build-new-cmd nm url))
              res (a/>! ch (fmt-block script))
              status (a/<! (cmd ch ["/bin/bash" "-c" script]))]
          (a/>! ch (fmt-block (str "EXIT STATUS: " status)))
          (a/close! ch) ;; close all listeners
          (rm-build nm)
          (notify-builds)))
      nm)
    (url-to-name url)))

(defn watch-build [nm soc]
  (log #{:build :ws} "Watch build " nm " with " soc)
  (if-let [bld (get-build nm)]
    (let [sub (a/chan)]
      (ohs/send! soc (cs/join (:log bld)))
      (try (when (:pub bld)
        (a/sub (:pub bld) :topic sub)
        (read-ch sub
                 (fn [msg] (try (ohs/send! soc (str msg))))
                 (fn [& _] (when soc (ohs/close soc)))))))
    (do
      (ohs/send! soc "No active builds"))))

(defn unwatch-build [nm soc]
  (log #{:build :ws} "Close " soc " for " nm))
