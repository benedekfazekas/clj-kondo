(ns clj-kondo.impl.analyzer.re-frame
  {:no-doc true}
  (:require
     [clj-kondo.impl.analyzer.common :as common]
     [clj-kondo.impl.utils :as utils]))

(defn analyze-reg [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [reg-val ctx] (if-let [kw (:k name-expr)]
                        [(assoc name-expr :reg fq-def) (assoc ctx :in-reg {:k kw :reg fq-def})]
                        [name-expr ctx])]
    (common/analyze-children ctx (cons reg-val body))))
