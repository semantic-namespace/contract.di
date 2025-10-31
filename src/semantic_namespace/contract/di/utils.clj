(ns semantic-namespace.contract.di.utils
    "Enhanced Dependency Injection with compound identity resolution and planning.
   Provides transparent component selection, alternatives, and execution planning."
    (:require [clojure.set :as set]
              [clojure.string :as str]
              [clojure.tools.namespace.dependency :as dep]
              [semantic-namespace.contract :as contract]
              [semantic-namespace.contract.type :as contract.type]
              [semantic-namespace.contract.di.component :as di.component]))

;; =============================================================================
;; Component Resolution
;; =============================================================================

(defn find-components-with-capability
  "Find all components that provide a specific capability."
  [capability]
  (->> (contract.type/instances #{:semantic-namespace.di/component})
       (filter #(contains? % capability))
       (map (fn [compound-id]
              {:identity compound-id
               :contract (contract/fetch (disj compound-id :semantic-namespace.contract/instance))}))))

(defn score-component
  "Score a component based on preference matching.
   Higher score = better match."
  [component-identity preferences excluded]
  (cond
    ;; Excluded components get negative score
    (some #(contains? component-identity %) excluded)
    -1

    ;; Score by preference overlap
    :else
    (count (set/intersection component-identity preferences))))

(defn resolve-component
  "Find the best component for a capability given preferences."
  [capability preferences excluded]
  (let [candidates (find-components-with-capability capability)
        scored (map (fn [{:keys [identity contract]}]
                      {:identity identity
                       :contract contract
                       :capability capability
                       :score (score-component identity preferences excluded)})
                    candidates)
        sorted (sort-by :score > scored)
        best (first sorted)]
    (when (and best (>= (:score best) 0))
      (assoc best
             :alternatives (rest sorted)
             :selection-reason (cond
                                 (> (:score best) 0)
                                 (format "Matched %d preferences" (:score best))

                                 (zero? (:score best))
                                 "Default selection (no preferences matched)"

                                 :else
                                 "No valid selection")))))

;; =============================================================================
;; Dependency Analysis
;; =============================================================================

(defn extract-dependencies
  "Extract all dependencies for a component."
  [component-contract]
  (or (:di.component/deps component-contract) []))

(defn build-dependency-graph
  "Build a dependency graph from resolved components."
  [resolved-components]
  (reduce (fn [graph {:keys [contract capability]}]
            (let [deps (extract-dependencies contract)]
              (reduce (fn [g dep]
                        (dep/depend g capability dep))
                      graph
                      deps)))
          (dep/graph)
          resolved-components))

(defn calculate-init-order
  "Calculate initialization order from dependency graph."
  [dependency-graph]
  (dep/topo-sort dependency-graph))

(defn find-transitive-deps
  "Find all transitive dependencies for target components."
  [targets all-contracts]
  (loop [to-process (set targets)
         processed #{}
         dependencies #{}]
    (if (empty? to-process)
      dependencies
      (let [current (first to-process)
            contract (first (filter #(= (:di.component/identity %) current)
                                    all-contracts))
            deps (if contract
                   (set (extract-dependencies contract))
                   #{})]
        (recur (set/union (disj to-process current)
                          (set/difference deps processed))
               (conj processed current)
               (set/union dependencies deps))))))

;; =============================================================================
;; Planning System
;; =============================================================================

(defn conflicting?
  "Check if two component identities conflict."
  [id1 id2]
  ;; Example: can't have both :production/ready and :test/isolated
  (and (or (contains? id1 :production/ready)
           (contains? id1 :test/isolated))
       (or (contains? id2 :production/ready)
           (contains? id2 :test/isolated))
       (not= (contains? id1 :production/ready)
             (contains? id2 :production/ready))))

(defn conflict-reason
  [id1 id2]
  "Mixing production and test components")

(defn find-conflicts
  "Find any conflicting component selections."
  [resolved-components]
  ;; Check for components that shouldn't be used together
  (let [identities (map :identity resolved-components)]
    (->> (for [id1 identities
               id2 identities
               :when (and (not= id1 id2)
                          (conflicting? id1 id2))]
           {:component1 id1
            :component2 id2
            :reason (conflict-reason id1 id2)})
         (distinct))))

(defn check-requirements
  "Check if all requirements can be satisfied."
  [targets resolved-components]
  (let [required (set targets)
        available (set (map :capability resolved-components))
        missing (set/difference required available)]
    {:satisfied (set/intersection required available)
     :missing missing
     :conflicts (find-conflicts resolved-components)}))

(defn generate-warnings
  "Generate warnings about the plan."
  [plan]
  (let [warnings (atom [])]

    ;; Check if all test components
    (when (every? #(contains? (:identity %) :test/isolated)
                  (:resolution plan))
      (swap! warnings conj "All components are test implementations"))

    ;; Check if missing auth
    (when (some #(contains? (:identity %) :api/endpoint)
                (:resolution plan))
      (when-not (some #(contains? (:identity %) :auth/service)
                      (:resolution plan))
        (swap! warnings conj "API endpoint without auth service")))

    ;; Check for slow startup
    (when (> (count (filter #(contains? (:identity %) :slow/startup)
                            (:resolution plan)))
             3)
      (swap! warnings conj "Multiple slow-starting components detected"))

    @warnings))

(defn plan
  "Create an execution plan for starting components.
   Returns detailed information about what would happen."
  [targets & [{:keys [prefer exclude] :as options}]]
  (let [targets-set (if (set? targets) targets #{targets})
        preferences (or prefer #{})
        excluded (or exclude #{})

        ;; Find all required components
        all-contracts (map #(contract/fetch (disj % :semantic-namespace.contract/instance))
                           (contract.type/instances #{:semantic-namespace.di/component}))
        all-deps (find-transitive-deps targets-set all-contracts)
        all-required (set/union targets-set all-deps)

        ;; Resolve each component
        resolution (keep (fn [capability]
                           (resolve-component capability preferences excluded))
                         all-required)

        ;; Build dependency graph
        dep-graph (build-dependency-graph resolution)
        init-order (calculate-init-order dep-graph)

        ;; Check requirements
        requirements (check-requirements all-required resolution)

        ;; Generate plan
        plan {:targets targets-set
              :preferences preferences
              :excluded excluded
              :resolution resolution
              :dependency-order init-order
              :requirements requirements
              :graph dep-graph}]

    ;; Add warnings
    (assoc plan :warnings (generate-warnings plan))))

;; =============================================================================
;; Plan Analysis & Inspection
;; =============================================================================

(defn categorize-component
  "Categorize a component based on its identity aspects."
  [identity]
  {:type (cond
           (contains? identity :production/ready) :production
           (contains? identity :test/isolated) :test
           (contains? identity :dev/local) :development
           :else :unknown)
   :startup (cond
              (contains? identity :fast/startup) :fast
              (contains? identity :slow/startup) :slow
              :else :normal)
   :persistence (cond
                  (contains? identity :persistent/storage) :persistent
                  (contains? identity :ephemeral/storage) :ephemeral
                  :else :unknown)})

(defn explain-selection
  "Explain why a specific component was selected."
  [capability plan]
  (let [selection (first (filter #(= (:capability %) capability)
                                 (:resolution plan)))]
    (when selection
      {:selected (:identity selection)
       :score (:score selection)
       :reason (:selection-reason selection)
       :alternatives (map (fn [alt]
                            {:identity (:identity alt)
                             :score (:score alt)})
                          (:alternatives selection))
       :preferences-matched (set/intersection (:identity selection)
                                              (:preferences plan))})))

(defn alternatives
  "Show all available alternatives for a capability."
  [capability]
  (let [components (find-components-with-capability capability)]
    (map (fn [{:keys [identity contract]}]
           {:identity identity
            :attributes (categorize-component identity)})
         components)))

(defn requirements
  "Check what's needed to start target components."
  [targets]
  (let [targets-set (if (set? targets) targets #{targets})
        all-contracts (map #(contract/fetch (disj % :semantic-namespace.contract/instance))
                           (contract.type/instances #{:semantic-namespace.di/component}))
        deps (find-transitive-deps targets-set all-contracts)
        available (set (map :di.component/identity all-contracts))]
    {:targets targets-set
     :required deps
     :available (set/intersection deps available)
     :missing (set/difference deps available)
     :suggestion (when-let [missing (seq (set/difference deps available))]
                   (format "Need to implement: %s"
                           (str/join ", " missing)))}))

(defn plan-diff
  "Compare two different plans."
  [plan1 plan2]
  (let [res1 (into {} (map (fn [r] [(:capability r) (:identity r)])
                           (:resolution plan1)))
        res2 (into {} (map (fn [r] [(:capability r) (:identity r)])
                           (:resolution plan2)))]
    {:changes (reduce (fn [changes capability]
                        (let [id1 (get res1 capability)
                              id2 (get res2 capability)]
                          (if (not= id1 id2)
                            (assoc changes capability
                                   {:from id1
                                    :to id2
                                    :diff {:removed (set/difference id1 id2)
                                           :added (set/difference id2 id1)}})
                            changes)))
                      {}
                      (set/union (set (keys res1)) (set (keys res2))))}))

;; =============================================================================
;; Plan Visualization
;; =============================================================================

(defn plan-report
  "Generate a human-readable report of the plan."
  [plan]
  (println "\n=== System Start Plan ===")
  (println "\nTargets:" (str/join ", " (map name (:targets plan))))

  (when (seq (:preferences plan))
    (println "Preferences:" (str/join ", " (map name (:preferences plan)))))

  (println "\n--- Component Selection ---")
  (doseq [{:keys [capability identity score selection-reason]} (:resolution plan)]
    (println (format "  %s" (name capability)))
    (println (format "    Selected: %s"
                     (str/join ", " (map name identity))))
    (println (format "    Score: %d | Reason: %s" score selection-reason)))

  (println "\n--- Initialization Order ---")
  (println "  " (str/join " -> " (map name (:dependency-order plan))))

  (let [missing (get-in plan [:requirements :missing])]
    (when (seq missing)
      (println "\n⚠️  Missing Components:")
      (doseq [m missing]
        (println (format "  - %s" (name m))))))

  (when-let [warnings (:warnings plan)]
    (when (seq warnings)
      (println "\n⚠️  Warnings:")
      (doseq [w warnings]
        (println (format "  - %s" w)))))

  (println "\n=== End Plan ===\n"))

(defn to-graphviz-plan
  [plan]
  (str "digraph SystemPlan {\n"
       "  rankdir=TB;\n"
       "  node [shape=box];\n"

       ;; Component nodes
       (str/join "\n"
                 (for [{:keys [capability identity score]} (:resolution plan)]
                   (format "  \"%s\" [label=\"%s\\nScore: %d\"%s];"
                           (name capability)
                           (name capability)
                           score
                           (cond
                             (contains? identity :test/isolated) ", style=dashed"
                             (contains? identity :production/ready) ", style=bold"
                             :else ""))))
       "\n"

       ;; Dependencies
       (str/join "\n"
                 (for [{:keys [capability contract]} (:resolution plan)
                       dep (extract-dependencies contract)]
                   (format "  \"%s\" -> \"%s\";"
                           (name capability)
                           (name dep))))
       "\n}"))

(defn to-mermaid-plan
  [plan]
  (str "graph TB\n"
       (str/join "\n"
                 (for [{:keys [capability identity score]} (:resolution plan)]
                   (format "  %s[\"%s<br/>Score: %d\"]"
                           (name capability)
                           (name capability)
                           score)))
       "\n"
       (str/join "\n"
                 (for [{:keys [capability contract]} (:resolution plan)
                       dep (extract-dependencies contract)]
                   (format "  %s --> %s"
                           (name capability)
                           (name dep))))))

(defn visualize-plan
  "Generate visualization data for the plan."
  [plan]
  {:graphviz (to-graphviz-plan plan)
   :mermaid (to-mermaid-plan plan)})

;; =============================================================================
;; Execution
;; =============================================================================

(defn execute-plan
  "Execute a plan, actually starting the components."
  [plan]
  (if-let [missing (seq (get-in plan [:requirements :missing]))]
    (throw (ex-info "Cannot execute plan: missing components"
                    {:missing missing
                     :plan plan}))
    (let [system (atom {})]
      (doseq [capability (:dependency-order plan)]
        (when-let [resolution (first (filter #(= (:capability %) capability)
                                             (:resolution plan)))]
          (let [contract (:contract resolution)
                initialized (di.component/init contract @system)]
            (swap! system assoc capability initialized))))
      @system)))

(defn start
  "Plan and start components in one step."
  [targets & [options]]
  (let [plan (plan targets options)]
    (plan-report plan)
    (if-let [missing (seq (get-in plan [:requirements :missing]))]
      (throw (ex-info "Cannot start: missing components"
                      {:missing missing
                       :suggestion (get-in plan [:requirements :suggestion])}))
      (execute-plan plan))))

;; =============================================================================
;; REPL Development Support
;; =============================================================================

(defn replan
  "Replan with different options."
  [existing-plan new-options]
  (plan (:targets existing-plan) new-options))

(defn dry-run
  "Show what would happen without executing."
  [targets & [options]]
  (let [p (plan targets options)]
    (plan-report p)
    p))

(defn quick-start
  "Start with common development preferences."
  [targets]
  (start targets {:prefer #{:fast/startup :dev/local :test/isolated}}))

(defn production-start
  "Start with production preferences."
  [targets]
  (start targets {:prefer #{:production/ready :persistent/storage}
                  :exclude #{:test/isolated :dev/local}}))

;; =============================================================================
;; System Introspection
;; =============================================================================

(defn system-status
  "Get current system status."
  []
  (let [all-components (contract.type/instances #{:semantic-namespace.di/component})
        by-type (group-by (fn [c]
                            (cond
                              (contains? c :production/ready) :production
                              (contains? c :test/isolated) :test
                              (contains? c :dev/local) :development
                              :else :unknown))
                          all-components)]
    {:total (count all-components)
     :by-type (update-vals by-type count)
     :capabilities (set (mapcat (fn [c]
                                  (filter #(not (contains? #{:semantic-namespace.contract/instance
                                                             :semantic-namespace.di/component}
                                                           %))
                                          c))
                                all-components))}))

(defn validate-system
  "Validate that all components can be properly initialized."
  []
  (let [all-components (contract.type/instances #{:semantic-namespace.di/component})
        results (map (fn [component-id]
                       (let [contract (contract/fetch (disj component-id :semantic-namespace.contract/instance))]
                         {:component component-id
                          :valid? (and contract
                                       (:di.component/init-handler contract)
                                       (:di.component/identity contract))
                          :issues (cond
                                    (nil? contract) ["No contract found"]
                                    (nil? (:di.component/init-handler contract)) ["Missing init handler"]
                                    (nil? (:di.component/identity contract)) ["Missing identity"]
                                    :else [])}))
                     all-components)]
    {:valid? (every? :valid? results)
          :components results}))
