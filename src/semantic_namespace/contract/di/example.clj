(ns semantic-namespace.contract.di.example
  (:require [semantic-namespace.contract.di.component :as component]))

(component/def :my-app.components.mock/db
  "mock db"
  {:di.component/deps []
   :di.component/identity :component/db
   :di.component/init-handler (fn [deps]
                                ["hello db!" deps])})

(component/def :my-app.components.mock/welcome
  "mocks welcome"
  {:di.component/deps [:component/db]
   :di.component/identity :component/welcome
   :di.component/init-handler (fn [deps]
                                ["Hello welcome" deps])})
