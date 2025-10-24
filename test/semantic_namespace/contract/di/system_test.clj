(ns semantic-namespace.contract.di.system-test
  (:require [clojure.test :refer (deftest testing is)]
            [semantic-namespace.contract :as contract]
            [semantic-namespace.contract.di.component]
            [semantic-namespace.contract.di.system :as system]))

(deftest system-test
  (testing "define components and init the system"

    
    (contract/def #{:semantic-namespace.di/component
                    :my-app.components.mock/db}
      {:di.component/deps []
       :di.component/identity :component/db
       :di.component/init-handler (fn [deps]
                                    ["hello db!" deps])
       :di.component/halt-handler (fn [deps]
                                    ["bye db" deps])})

    (contract/def #{:semantic-namespace.di/component
                    :my-app.components.mock/welcome}
      {:di.component/deps [:component/db]
       :di.component/identity :component/welcome
       :di.component/init-handler (fn [deps]
                                    ["Hello semantic component" deps])
       :di.component/halt-handler (fn [deps]
                                    ["Bye semantic component" deps])})

;;(reset!*)
    (is (= (system/init)
           #:component{:db ["hello db!" {}],
                       :welcome
                       ["Hello semantic component"
                        #:component{:db ["hello db!" {}]}]}))))


