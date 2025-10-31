(ns semantic-namespace.contract.di.system
  (:require [clojure.tools.namespace.dependency :as dep]
            [semantic-namespace.contract.type :as contract.type]
            [semantic-namespace.contract :as contract]
            [semantic-namespace.contract.di.component :as system.component]))

#_(defn reset!* []
  (reset! system.component/registry {}))
#_(mapv #(let [initialised (system.component/init % {})
             contract (contract/fetch :semantic-namespace.di/component %)
             ]
         [% initialised]) (contract.type/instances :semantic-namespace.di/component))

(defn- find-component-by-identity [contracts identity]
  (first (filter (fn [c]
             (= identity (:di.component/identity c))) contracts)))

(defn init [& [config]]
  (let [system* (atom {})
        components (contract.type/instances #{:semantic-namespace.di/component})
        contracts (mapv contract/fetch components)
        system (->> contracts
                    (reduce
                     (fn [graph component-contract]
                       (reduce
                        (fn [graph* dep]
                          (let [id-d (:di.component/identity (find-component-by-identity contracts dep))]
                            (dep/depend graph*  (:di.component/identity component-contract) id-d)))
                        graph
                        (:di.component/deps component-contract)))
                     (dep/graph)))
        system-components-sorted (filter
                                  (partial contains? (set (mapv :di.component/identity contracts)))
                                  (dep/topo-sort system))]

    (reduce (fn [s component-kw]
              (let [component-spec
                    (find-component-by-identity contracts component-kw)
                    initialised-component (system.component/init component-spec s)]
                (assoc s (:di.component/identity component-spec) initialised-component))) {}

            system-components-sorted)))

(defn halt [& [config]]
  ;; check if ::services.component/halt exists then call it
  ;; or default to integrant    
)
