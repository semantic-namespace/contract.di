(defproject com.github.semantic-namespace/contract.di "0.1.2-SNAPSHOT"
  :description "semantic-spec -> dependency injection"
  :url "https://github.com/semantic-spec/contract.di"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.semantic-namespace/contract "0.1.1-SNAPSHOT"]
                 [com.github.semantic-namespace/contract.docs "0.1.1-SNAPSHOT"]
                 [com.github.semantic-namespace/compound-identity "0.1.1-SNAPSHOT"]]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :creds :gpg}]]
  :profiles {:dev {:source-paths   ["dev/src"]
                   :repl-options   {:init-ns dev}
                   :dependencies [[org.clojure/tools.namespace "1.4.4"]]}})
