(defproject janus "0.0.2"
  :description "Consumer-driven contracts, verified both ways."
  :url "http://github.com/gga/janus"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [org.clojure/data.json "0.2.3"]
                 [json-path "0.2.0"]
                 [clj-http "0.3.2"]
                 [org.clojure/core.logic "0.6.6"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/data.xml "0.0.3"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [org.mortbay.jetty/jetty "6.1.14"]
                 [ring-serve "0.1.2"]
                 [midje "1.6-beta1"]
                 [org.clojure/data.generators "0.1.2"]
                 [ring-cors "0.1.0"]]
  :profiles {:dev {
                   :injections [(require 'clojure.pprint)]
                   :source-paths ["examples/"]
                   :dependencies [ [clj-http-fake "0.2.3"]
                                   [ring/ring-devel "1.1.0"]
                                   [ring/ring-json "0.2.0"]
                                   [compojure "1.1.5"]
                                   [liberator "0.9.0"]
                                   [org.clojure/tools.namespace "0.2.4"]
                                   [hiccup "1.0.4"]]
}}
  :main janus)
