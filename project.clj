(defproject Accounting "0.1.0-SNAPSHOT"
  :description "Accounting = doing the bookkeeping for my business"
  :url "http://seasoft.com.au/FIXME"
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/clojurescript "1.9.562"]
                 [org.omcljs/om "1.0.0-beta1"]
                 [awkay/untangled "1.0.0-SNAPSHOT"]
                 [navis/untangled-datomic "0.4.11"]
                 [clj-time "0.13.0"]
                 [com.andrewmcveigh/cljs-time "0.5.0"]
                 ;; Putting in this version of guava fixes datomic dependency issues
                 [com.google.guava/guava "21.0"]
                 [com.datomic/datomic-pro "0.9.5385"]
                 ]
  :source-paths ["src/main"]
  :resource-paths ["resources"]
  :clean-targets ^{:protect false} ["resources/public/js" "target" "out"]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}

  :plugins [[lein-cljsbuild "1.1.6"]]

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src/main" "src/dev"]
                :figwheel     {:on-jsload "cljs.user/refresh"}
                :compiler     {:main          cljs.user
                               :output-to     "resources/public/js/app.js"
                               :output-dir    "resources/public/js/app"
                               :preloads      [devtools.preload]
                               :asset-path    "js/app"
                               :optimizations :none}}]}

  ;; Allows printing of stack trace from REPL, plus other things...
  :profiles {:dev {:source-paths ["src/dev" "src/main" "src/test"]
                   :dependencies [[binaryage/devtools "0.9.2"]
                                  [org.clojure/java.classpath "0.2.3"]
                                  [org.clojure/tools.namespace "0.3.0-alpha4"]
                                  [figwheel-sidecar "0.5.9"]]}})
