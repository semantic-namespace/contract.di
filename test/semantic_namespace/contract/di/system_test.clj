(ns semantic-namespace.contract.di.system-test
  (:require [clojure.test :refer (deftest testing is)]
            [semantic-namespace.contract.di.component] 
            [semantic-namespace.contract.di.system :as system]
            [semantic-namespace.compound.identity :as compound.identity]))

;; dyn load example system
(require '[semantic-namespace.contract.di.example])

(deftest system-test
  (testing "define components and init the system"   

    
    
;;(reset!*)
    (is (= (system/init)
           #:component{:db ["hello db!" {}],
                       :welcome
                       ["Hello semantic component"
                        #:component{:db ["hello db!" {}]}]}))))





(deftest system-identity-test

  (is (= {:identity
          #{:my-app.components.mock/welcome :semantic-namespace.di/component},
          :aspects
          (:my-app.components.mock/welcome :semantic-namespace.di/component),
          :value nil,
          :semantic-neighbors
          ({:identity
            #{:my-app.components.mock/welcome
              :semantic-namespace.di/component
              :semantic-namespace.contract/instance},
            :shared
            #{:my-app.components.mock/welcome
              :semantic-namespace.di/component},
            :similarity 2/3}
           {:identity
            #{:semantic-namespace.contract/type
              :semantic-namespace.di/component},
            :shared #{:semantic-namespace.di/component},
            :similarity 1/3}
           {:identity
            #{:semantic-namespace.di/component
              :my-app.components.mock/db
              :semantic-namespace.contract/instance},
            :shared #{:semantic-namespace.di/component},
            :similarity 1/4}),
          :suggested-aspects
          (:semantic-namespace.contract/instance
           :semantic-namespace.contract/type
           :my-app.components.mock/db),
          :usage-count 1}
         (compound.identity/describe #{:semantic-namespace.di/component
                                       :my-app.components.mock/welcome})))

  (is (= '{:total-identities 3,
           :namespaces
           (:semantic-namespace.contract
            :semantic-namespace.di
            :my-app.components.mock),
           :aspect-frequency
           ([:semantic-namespace.di/component 3]
            [:semantic-namespace.contract/instance 2]
            [:semantic-namespace.contract/type 1]
            [:my-app.components.mock/db 1]
            [:my-app.components.mock/welcome 1]),
           :clusters
           {:semantic-namespace.contract
            #{#{:semantic-namespace.di/component
                :my-app.components.mock/db
                :semantic-namespace.contract/instance}
              #{:semantic-namespace.contract/type
                :semantic-namespace.di/component}
              #{:my-app.components.mock/welcome
                :semantic-namespace.di/component
                :semantic-namespace.contract/instance}},
            :semantic-namespace.di
            #{#{:semantic-namespace.di/component
                :my-app.components.mock/db
                :semantic-namespace.contract/instance}
              #{:semantic-namespace.contract/type
                :semantic-namespace.di/component}
              #{:my-app.components.mock/welcome
                :semantic-namespace.di/component
                :semantic-namespace.contract/instance}},
            :my-app.components.mock
            #{#{:semantic-namespace.di/component
                :my-app.components.mock/db
                :semantic-namespace.contract/instance}
              #{:my-app.components.mock/welcome
                :semantic-namespace.di/component
                :semantic-namespace.contract/instance}}},
           :anomalies ()}
         (sort (compound.identity/generate-docs)))))


;; update graphviz 
(spit "graph.dot" (compound.identity/to-graphviz))
