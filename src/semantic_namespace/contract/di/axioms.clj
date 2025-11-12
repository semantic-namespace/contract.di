(ns semantic-namespace.contract.di.axioms
  (:require
   [clojure.set :as set]
   [semantic-namespace.contract :as contract]
   [semantic-namespace.contract.axiom :as axiom]
   [semantic-namespace.contract.docs]))

(axiom/def
  :contract.di/dependencies-resolvable
  "All component dependencies (:di.component/deps) must resolve to defined component 
    identities. You cannot depend on components that don't exist."
  (fn [registry]
    (let [components (filter #(contains? % :semantic-namespace.di/component) (keys registry))
          all-identities (set (map #(:di.component/identity (contract/fetch %)) components))]
      (every? (fn [comp-id]
                (let [comp (contract/fetch comp-id)
                      deps (set (:di.component/deps comp []))]
                  (set/subset? deps all-identities)))
              components))))

(axiom/def
  :contract.di/acyclic-dependencies
  "Component dependency graph must be acyclic. Circular dependencies 
    (A → B → C → A) prevent successful initialization and must be eliminated."
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
      (not-any? #(has-cycle? dep-graph % #{} #{}) (keys dep-graph)))))


;; after ontology

(axiom/def
  #{:axiom.di/component-type-dependency-direction}
    "Enforces that component types follow the allowed dependency direction. 
   Higher-level architectural types (api, feature, service) must not 
   depend on lower-level types in reverse (service → api is allowed, api → service 
   is allowed, but service → api is not; and foundation-level infrastructure must 
   never depend on anything above it). This preserves architectural integrity and 
   prevents accidental inversion of control."
  identity)

(axiom/def
  #{:axiom.di/component-type-init-ordering}
    "Ensures that component initialization order respects the semantic hierarchy 
   of component types. Foundational components must initialize before services, 
   services before features, and features before API components. This guarantees 
   that each component's required dependencies are available at startup and prevents 
   undefined or partial initialization states."
  identity)

(axiom/def
  #{:axiom.di/tier-type-consistency}
    "Validates that a component’s declared tier matches its component type. 
   Foundation-tier components must be infrastructure-type, service-tier components 
   must be service-type, feature-tier components must be feature-type, and 
   api-tier components must be api-type. This alignment prevents contradictory 
   identity semantics and keeps the ontology coherent."
  identity)

(axiom/def
  #{:axiom.di/tier-purity-of-dependencies}
    "Requires that dependencies always point downwards in the architectural tier 
   hierarchy. Higher tiers may depend on lower ones, but lower tiers must never 
   depend on higher ones. This prevents structural contamination such as an 
   infrastructure component depending on an API or feature component, and protects 
   the stability and predictability of the dependency graph."
  identity)

(axiom/def
  #{:axiom.di/capability-component-type-consistency}
    "Ensures that declared capabilities are semantically compatible with the component’s 
   type. Low-level capabilities such as persistence, caching, messaging, or search 
   may only be declared by infrastructure or service components. Feature or API 
   components must not claim capabilities that belong to lower layers. This keeps 
   capabilities meaningful and prevents semantic overload."
  identity)

(axiom/def
  #{:axiom.di/capability-tier-consistency}
    "Verifies that capabilities are only used in tiers where they are structurally valid. 
   Foundation-tier components may declare any system capability, but service-tier 
   components may only declare business-level capabilities, and feature/api tiers 
   must not declare infrastructure-level capabilities. This maintains proper 
   separation of concerns across architectural layers."
  identity)

(axiom/def
  #{:axiom.di/capability-dependency-completeness}
    "Checks that if a component declares a capability, its dependency list includes 
   at least one component that actually provides the underlying resource for that 
   capability. For instance, a component claiming search capability must depend on 
   the component providing the search backend. This enforces that semantic claims 
   are backed by actual dependency structure."
  identity)

(axiom/def
  #{:axiom.di/integration-role-isolation}
    "Validates that integration roles respect the isolation boundaries between internal, 
   external, and cross-domain integrations. External integrations may not depend 
   on internal logic; cross-domain integrations may not depend on external APIs; 
   internal integrations must not introduce external coupling indirectly. This 
   protects integration boundaries and avoids unintended propagation of external 
   concerns."
  identity)

(axiom/def
  #{:axiom.di/integration-role-tier-alignment}
    "Ensures that integration roles appear only at appropriate tiers. External 
   integrations belong to infrastructure or service tiers, not feature or API. 
   Internal integrations may appear at any tier except foundation if they rely 
   on business logic. This rule prevents mixing integration semantics with user-facing 
   logic."
  identity)

(axiom/def
  #{:axiom.di/cross-cutting-consistency}
    "Checks that cross-cutting concerns such as security, compliance, logging, or 
   monitoring appear only on components that are responsible for such behavior. 
   This prevents low-level or unrelated components from incorrectly claiming 
   cross-cutting semantics and keeps the ontology clear and intentional."
  identity)

(axiom/def
  #{:axiom.di/cross-cutting-isolation}
    "Prevents components marked with cross-cutting roles from participating in cycles 
   or forming mutually dependent structures with other components. Cross-cutting 
   semantics require high stability and must remain cycle-free to avoid deadlocks 
   and bootstrap failures in the DI graph."
  identity)

(axiom/def
  #{:axiom.di/domain-dependency-boundary}
    "Enforces explicit boundaries between project-specific business domains. A component 
   belonging to domain A may depend on components in domain B only if the project 
   ontology explicitly allows this direction. This preserves domain integrity and 
   prevents accidental or implicit cross-domain coupling."
  identity)

(axiom/def
  #{:axiom.di/domain-tier-consistency}
    "Verifies that business domains appear only in tiers that are semantically compatible 
   with their purpose. Low-level technical domains must belong to foundation or service tiers, 
   and high-level business domains must not appear in foundation or infrastructure components. 
   This ensures each domain is placed at the correct architectural layer."
  identity)

(axiom/def
  #{:axiom.di/domain-capability-consistency}
    "Checks that the domain semantics imply the presence of certain capabilities somewhere 
   in the dependency chain. For example, domains dealing with data management must be backed 
   by persistence capability; domains handling search must be backed by a search capability. 
   This ensures domain semantics and DI structure remain aligned."
  identity)

(axiom/def
  #{:axiom.di/capability-init-handler-coherence}
    "Ensures that a component’s declared capabilities are reflected in its implementation 
   contract. If the identity says the component provides capability X, its init and halt 
   handlers must actually construct and release the corresponding resource. This binds 
   ontology semantics to concrete implementation behavior."
  identity)

(axiom/def
  #{:axiom.di/dependency-graph-ontology-coherence}
    "Validates that the entire DI dependency graph satisfies the semantic constraints imposed 
   by type, tier, capability, integration role, and domain ontologies. This is the global 
   coherence check ensuring that the dependency network expresses the intended architecture 
   rather than an accidental one."
  identity)

(axiom/def
  #{:axiom.di/cross-cutting-lifecycle-coherence}
    "Ensures that cross-cutting components (security, compliance, monitoring, logging) depend 
   only on stable, mature components. They must not depend on components marked experimental, 
   deprecated, or unstable. This protects critical system-wide concerns from becoming fragile 
   or unreliable."
  identity)

