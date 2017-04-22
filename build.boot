(set-env! :dependencies '[[org.clojure/clojure "RELEASE"]
                          [org.clojure/clojurescript "1.9.521"]
                          [adzerk/boot-cljs "2.0.0"]
                          [pandeiro/boot-http "0.7.6"]
                          [adzerk/boot-reload "0.5.1"]
                          [org.martinklepsch/boot-garden "1.3.2-0"]
                          [org.clojure/tools.nrepl "0.2.13"]
                          [midje "1.9.0-alpha5"]
                          [adzerk/boot-test "1.2.0"]
                          [org.clojure/test.check "0.9.0"]
                          [crisptrutski/boot-cljs-test "0.3.0"]
                          [zilti/boot-midje "0.1.2"]]

          :source-paths #{"src"}
          :resource-paths #{"resources"})

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[org.martinklepsch.boot-garden :refer [garden]]
         '[crisptrutski.boot-cljs-test :as cljs]
         '[zilti.boot-midje :refer [midje]])

(deftask build [p production bool "Enable production build"]
  (comp (garden :styles-var 'smallworld.styles/screen :output-to "public/style.css" :pretty-print (not production))
        ;; (file :path "public/index.html"
        ;;       :content (clojure.string/join "" (smallworld.index/index!)))
        (apply cljs (when production [:optimizations :advanced]))
        (if production
          (sift :include #{#"public/(index.html|main.js|style.css)"})
          identity)))

(deftask dev [p production bool "Enable production build"]
  (comp (watch)
        (serve :dir "target/public")
        (reload :on-jsload 'smallworld.main/main)
        (build :production production)
        (target)))

(deftask release []
  (comp (build :production true)
        (target)))

(deftask run []
  (comp (watch)
        (serve :dir "target/public")
        (build :production false)
        (target)))

(deftask testing [] (merge-env! :source-paths #{"test"}) identity)

(deftask test []
  (comp (testing)
        (midje)
        (cljs/test-cljs)))
