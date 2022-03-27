(ns user
  (:require [clojure.tools.namespace.repl :as repl]))

(defn in-dev []
  (in-ns 'dev) :ok)

(defn dev
  []
  (repl/refresh :after 'user/in-dev))
