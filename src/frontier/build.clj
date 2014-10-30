(ns frontier.build
  (:require
    [clojure.java.io :as io]
    [clojure.core.async :as a])
  (:import java.io.BufferedReader
           java.io.InputStreamReader))

(defn buf-read [s]
  (-> s (InputStreamReader.) (BufferedReader.)))

(defn strm->chan [ch st wrap]
  (a/thread
    (let [buf (buf-read st)]
     (loop []
      (when
        (try (when-let [s (.readLine buf)] (a/put! ch (wrap s)))
             (catch Exception e (a/>!! ch (wrap (pr-str e))) nil))
        (recur))))))

(defn shell [& args]
  (let
    [out    (a/chan)
     ctrl   (a/chan)
     proc   (.exec (Runtime/getRuntime) ^"[Ljava.lang.String;"  (into-array args))
     inp-ch (strm->chan out (.getInputStream proc) (fn [x] [:out x]))
     err-ch (strm->chan out (.getErrorStream proc) (fn [x] [:error x]))
     end-ch (a/thread (try (.waitFor proc)) (a/>!! out [:exit (.exitValue proc)]))]

    (a/go (a/<! ctrl) (.destroy proc))
    (a/go-loop []
       (if (a/<! (a/merge [inp-ch err-ch end-ch]))
         (recur)
         (a/close! out)))
    [out ctrl]))


(comment
  (require '[frontier.projects :as ffp])
  (def logs (atom []))
  (defn log [s] (swap! logs conj s))

  (println (pr-str (filter #(= (first %) :error) @logs)))
  (println (pr-str (take-last 5 @logs)))
  (println (count @logs))

  (a/go (a/>! control :stop))

  (println control)

  (let [[out ctrl] (shell "bash" "-c" (ffp/build-cmd "fhir-terminology"))]
    (def control ctrl)
    (println out)
    (println ctrl)
    (a/go-loop []
               (when-let [s (a/<! out)]
                 (log s)
                 (recur)))))

