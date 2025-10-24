(ns semantic-namespace.contract.di.component
  (:require [clojure.spec.alpha :as s]
            [semantic-namespace.contract :as contract]
            [semantic-namespace.contract.type :as contract.type]))

(defonce registry (atom {}))

(s/def :di.component/init-handler fn?)
(s/def :di.component/halt-handler fn?)
(s/def :di.component/identity qualified-keyword?) ;; public component identity
(s/def :di.component/deps (s/coll-of qualified-keyword?))

(contract.type/def #{:semantic-namespace.di/component}
  [:di.component/identity :di.component/deps :di.component/init-handler :di.component/halt-handler])

(defn init [component-spec system]
  ((:di.component/init-handler component-spec)
   (reduce
    (fn [c dependency-kw]
      (let [component (dependency-kw system)]
        (assoc c dependency-kw component)))
    {} (:di.component/deps component-spec))))
