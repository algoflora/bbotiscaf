(defproject io.github.algoflora/bbotiscaf "0.1.0"
  :description "BBotiscaf - A framework for complex Telegram Bots development"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0-rc2"]
                 [metosin/malli "0.16.3"]
                 [integrant/integrant "0.10.0"]
                 [aero/aero "1.1.6"]
                 [tick/tick "0.7.5"]
                 [eftest/eftest "0.6.0"]]
  :source-paths ["src"]
  :resource-paths ["resources"]
  :main ^:skip-aot bbotiscaf.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
