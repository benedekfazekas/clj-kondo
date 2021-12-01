(ns clj-kondo.impl.analyzer.re-frame
  {:no-doc true}
  (:require
     [clj-kondo.impl.analyzer.common :as common]
     [clj-kondo.impl.utils :as utils]))

(def counter (atom 0))

(defn new-id! []
  (str (swap! counter inc)))

(defn- assoc-reg-maybe [ctx name-expr fq-def]
  (if (:k name-expr)
    (let [ns (namespace fq-def)
          kns (keyword ns)
          re-frame-name (name fq-def)
          id (new-id!)]
      [(assoc-in
        ctx
        [:context kns :in-id] id)
       (-> name-expr
           (assoc :reg fq-def)
           (assoc-in [:context kns] {:id id :var re-frame-name}))])
    [ctx name-expr]))

(defn analyze-reg [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        [ctx reg-val] (assoc-reg-maybe ctx name-expr fq-def)]
    (common/analyze-children ctx (cons reg-val body))))

(defn analyze-subscribe [ctx expr ns]
  (let [kns (keyword ns)]
    (common/analyze-children (assoc-in ctx [:context kns :from-sub] (get-in ctx [:context kns :in-id])) expr)))

(defn analyze-reg-sub [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        arrow-subs (map last (filter #(= :<- (:k (first %))) (partition-all 2 body)))]
    (if (seq arrow-subs)
      (let [[ctx reg-val] (assoc-reg-maybe ctx name-expr fq-def)]
        (doseq [s arrow-subs]
          (analyze-subscribe ctx [s] (str (namespace fq-def))))
        (common/analyze-children ctx (cons reg-val (last body))))
      (analyze-reg ctx expr fq-def))))
;;;; Scratch

(comment
  (require '[clj-kondo.core :as clj-kondo])
  (-> (with-in-str "(require '[re-frame.core :as re-frame]) (re-frame/reg-sub ::foo (fn [_]))"
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:output {:analysis {:context true
                                                                              :keywords true}}}}))
      :analysis :keywords)

  (-> (with-in-str "(require '[re-frame.core :as re-frame]) (re-frame/reg-sub ::foo (fn [x] (inc x)))"
        (clj-kondo/run! {:lang :cljs :lint ["-"] :config {:output {:analysis {:context true
                                                                              :keywords true}}}}))
      :analysis :var-usages)

  )
