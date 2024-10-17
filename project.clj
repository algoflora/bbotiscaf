(defproject io.github.algoflora/bbotiscaf "0.1.3"
  :description "BBotiscaf - A framework for complex Telegram Bots development"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[aero/aero "1.1.6"]
                 [cheshire "5.13.0"]
                 [datalevin "0.9.12"]
                 [http-kit/http-kit "2.8.0"]
                 [integrant/integrant "0.10.0"]
                 [me.raynes/fs "1.4.6"]
                 [metosin/malli "0.16.3"]
                 [org.clojure/clojure "1.12.0"]
                 [org.clojure/core.async "1.6.681"]
                 [resauce "0.2.0"]
                 [tick/tick "0.7.5"]]

  :source-paths ["src"]
  :resource-paths ["resources"]
  :main ^:skip-aot bbotiscaf.core

  :target-path "target/%s"

  :jvm-opts ["--add-opens=java.base/java.nio=ALL-UNNAMED"
             "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]
  :profiles {:dev {:dependencies [[lambdaisland/kaocha "1.91.1392"]]
                   :source-paths ["src"]
                   :resource-paths ["resources" "test/resources"]
                   :jvm-opts ["-Dbbotiscaf.malli.instrument=true"
                              "-Dbbotiscaf.profile=test"
                              "--add-opens=java.base/java.nio=ALL-UNNAMED"
                              "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]}
             :uberjar {:aot [bbotiscaf.core]
                       :uberjar-name "bbotiscaf.jar"
                       :uberjar-exclusions ["bbotiscaf.aws.*"]
                       :env {:DTLV_COMPILE_NATIVE "true"
                             :USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM "false"}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"test"   ["with-profile" "dev" "run" "-m" "kaocha.runner" "--fail-fast"]})
