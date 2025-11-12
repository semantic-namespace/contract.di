(ns semantic-namespace.contract.di.component
  (:refer-clojure :exclude [def])
  (:require [clojure.spec.alpha :as s]
            [semantic-namespace.contract :as contract]
            [semantic-namespace.contract.docs :as docs]
            [semantic-namespace.contract.type :as contract.type]))

(s/def :di.component/env qualified-keyword?)
(s/def :di.component/init-handler fn?)
(s/def :di.component/halt-handler fn?)
(s/def :di.component/identity qualified-keyword?) ;; public component identity
(s/def :di.component/deps (s/coll-of qualified-keyword?))

(def fields [:di.component/identity :di.component/deps :di.component/init-handler :di.component/halt-handler :di.component/env])

(contract.type/def #{:semantic-namespace.di/component} fields)

(defn def
  ([id opts]
   (semantic-namespace.contract.di.component/def id id opts))
  ([id docs opts]
   (let [id (if (seq? id) id #{id})]
    (contract/def (into #{:semantic-namespace.di/component} id)
      (-> (select-keys opts fields)
          (update :di.component/env (fn [x] (or x ::prod)))
          (update :di.component/halt-handler
                  #(or % (fn [_] (str "bye bye" (:di.component/identity opts)))))))
    (docs/def id docs)))
  )

(defn init [component-spec system]
  ((:di.component/init-handler component-spec)
   (reduce
    (fn [c dependency-kw]
      (let [component (dependency-kw system)]
        (assoc c dependency-kw component)))
    {} (:di.component/deps component-spec))))
