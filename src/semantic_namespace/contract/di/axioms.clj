(ns semantic-namespace.contract.di.axioms
  (:require             [semantic-namespace.contract :as contract]
                        [semantic-namespace.contract.axiom]
                        [semantic-namespace.contract.docs]))


(contract/def
  #{:semantic-namespace/axiom
    :axiom-type/requires-definition
    :contract.di/dependencies-resolvable}
  {:axiom/rule
   (fn [registry]
     (let [components     (filter #(contains? % :semantic-namespace.di/component)
                                  (keys registry))
           all-identities (set (map #(:di.component/identity (contract/fetch %))
                                    components))]
       ;; Every dependency must reference an existing component identity
       (every? (fn [comp-id]
                 (let [comp (contract/fetch comp-id)
                       deps (set (:di.component/deps comp []))]
                   (clojure.set/subset? deps all-identities)))
               components)))})


(contract/def #{:semantic-namespace/docs
                :axiom-type/requires-definition
                :contract.di/dependencies-resolvable}
  {:docs/content
   "All component dependencies (:di.component/deps) must resolve to defined component 
    identities. You cannot depend on components that don't exist."})


(contract/def
  #{:semantic-namespace/axiom
    :axiom-type/structural
    :contract.di/acyclic-dependencies}
  {:axiom/rule
   (fn [registry]
     (let [components (filter #(contains? % :semantic-namespace.di/component) 
                             (keys registry))
           dep-graph (reduce (fn [g comp-id]
                              (let [comp (contract/fetch comp-id)
                                    identity-kw (:di.component/identity comp)
                                    deps (:di.component/deps comp [])]
                                (assoc g identity-kw deps)))
                            {}
                            components)
           has-cycle? (fn has-cycle? [graph node visited path]
                        (cond
                          (contains? path node) true
                          (contains? visited node) false
                          :else (some #(has-cycle? graph % 
                                                  (conj visited node) 
                                                  (conj path node))
                                     (get graph node []))))]
       (not-any? #(has-cycle? dep-graph % #{} #{}) (keys dep-graph))))
    
})

(contract/def 
   #{:semantic-namespace/docs
    :axiom-type/structural
    :contract.di/acyclic-dependencies}
  {:docs/content
      "Component dependency graph must be acyclic. Circular dependencies 
    (A → B → C → A) prevent successful initialization and must be eliminated."})
