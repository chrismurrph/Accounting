(defproject instaxpile "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [clj-time "0.13.0"]]
  :source-paths ["dev" "src" "test"]

  ;; Allows printing of stack trace from REPL, plus other things...
  :profiles {:dev {
                   :repl-options {
                                  :init-ns          user
                                  :port             7001}

                   :env          {:dev true}
                   :dependencies [[org.clojure/test.check "0.9.0"]
                                  [org.clojure/java.classpath "0.2.3"]
                                  [org.clojure/tools.namespace "0.2.11"]]}})
