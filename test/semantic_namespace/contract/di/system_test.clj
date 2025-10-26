(ns semantic-namespace.contract.di.system-test
  (:require [clojure.test :refer (deftest testing is)]
            [semantic-namespace.contract.di.component]
            [semantic-namespace.contract.di.example] 
            [semantic-namespace.contract.di.system :as system]))


(deftest system-test
  (testing "define components and init the system"   

;;(reset!*)
    (is (= (system/init)
           #:component{:db ["hello db!" {}],
                       :welcome
                       ["Hello semantic component"
                        #:component{:db ["hello db!" {}]}]}))))


