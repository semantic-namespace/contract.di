(ns semantic-namespace.contract.di.ontology
  (:require [clojure.spec.alpha :as s]
            ;; [semantic-namespace.contract.ontology] requires?
            ))


;;Component specific ontology
;; Allows axioms on layering, dependency purity, architecture invariants.
(s/def :component/type #{:component/infrastructure
                         :component/service
                         :component/feature
                         :component/api})


;; Component-specific too
;;Expresses meaning without including configuration; supports capability-based axioms.
(s/def :capability/type #{:capability/persistence
                          :capability/cache
                          :capability/messaging
                          :capability/search
                          :capability/compute
                          :capability/external-api
                          :capability/config})

;; example
(def foo #{:semantic-namespace.di/component
           :component/service
           :domain/orders
           :capability/persistence
           :tier/service
           :integration/internal})

