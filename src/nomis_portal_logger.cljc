(ns nomis-portal-logger
  #?(:cljs (:require-macros [nomis-portal-logger]
                            [portal.console]))
  (:refer-clojure :exclude [tap>])
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

(defn tap>* [tag level form expr]
  (let [{:keys [line column]} (meta form)]
    `(if-not @enabled
       ~expr
       (let [[flow# result#] (run (fn [] ~expr))]
         (clojure.core/tap>
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
         (if (= flow# :throw) (throw result#) result#)))))

(defmacro tap>
  ([expr]     (tap>* nil :info &form expr))
  ([expr tag] (tap>* tag :info &form expr)))
