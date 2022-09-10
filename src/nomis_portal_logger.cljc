(ns nomis-portal-logger
  #?(:cljs (:require-macros portal.console))
  (:require [clojure.string :as str]))

;;;; Copied from
;;;; https://github.com/djblue/portal-talk/blob/main/src/url_shortener/console.cljc.
;;;; And then modified.

(defonce enabled (atom true))

(defn toggle [] (swap! enabled not))

(defn now []
  #?(:clj (java.util.Date.) :cljs (js/Date.)))

(defn run [f]
  (try
    [nil (f)]
    (catch #?(:clj Exception :cljs :default) ex#
      [:throw ex#])))

(defn runtime []
  #?(:bb :bb :clj :clj :cljs :cljs))

(defn log*
  ([level form expr]
   (log* level form expr nil))
  ([level form expr tag]
   (let [{:keys [line column]} (meta form)]
     `(if-not @enabled
        ~expr
        (let [[flow# result#] (run (fn [] ~expr))]
          (tap>
           {:form     (quote ~expr)
            :level    (if (= flow# :throw) :fatal ~level)
            :result   result#
            :ns       (quote ~(symbol (if tag
                                        (str/join " " [tag *ns*])
                                        (str *ns*))))
            :file     ~#?(:clj *file* :cljs nil)
            :line     ~line
            :column   ~column
            :time     (now)
            :runtime  (runtime)})
          (if (= flow# :throw) (throw result#) result#))))))

(defmacro log [expr]
  (log* :info &form expr))

(defmacro log-with-tag [tag expr]
  (log* :info &form expr tag))
