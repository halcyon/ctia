#!/usr/bin/env bb

;; determines the build matrix for the GitHub Actions build. 
;; try it locally:
;;   # normal builds
;;   $ GITHUB_EVENT_NAME=pull_request ./scripts/actions/print-matrix.clj
;;   $ GITHUB_EVENT_NAME=push ./scripts/actions/print-matrix.clj
;;   # cron build
;;   $ GITHUB_EVENT_NAME=schedule ./scripts/actions/print-matrix.clj

(def default-java-version "11.0.9")
(def java-15-version "15")
(def non-cron-ctia-nsplits
  "Job parallelism for non cron tests."
  10)
(def cron-ctia-nsplits
  "Job parallelism for cron tests."
  2)

(defn valid-split? [{:keys [this_split total_splits
                            java_version ci_profiles] :as m}]
  (and (= #{:this_split :total_splits
            :java_version :ci_profiles} (set (keys m)))
       (nat-int? this_split)
       ((every-pred nat-int? pos?) total_splits)
       (<= 0 this_split)
       (< this_split total_splits)
       ((every-pred string? seq) java_version)
       ((every-pred string? seq) ci_profiles)))

(defn splits-for [base nsplits]
  {:pre [(pos? nsplits)]
   :post [(every? valid-split? %)
          (= (range nsplits)
             (map :this_split %))
          (= #{nsplits}
             (into #{} (map :total_splits) %))]}
  (for [this-split (range nsplits)]
    (assoc base
           :this_split this-split
           :total_splits nsplits)))

(defn non-cron-matrix
  "Actions matrix for non cron builds"
  []
  {:post [(every? valid-split? %)
          (zero? (mod (count %) non-cron-ctia-nsplits))]}
  (splits-for
    {:ci_profiles "default"
     :java_version default-java-version}
    non-cron-ctia-nsplits))

(defn cron-matrix
  "Actions matrix for cron builds"
  []
  {:post [(every? valid-split? %)
          (zero? (mod (count %) cron-ctia-nsplits))]}
  (mapcat #(splits-for % cron-ctia-nsplits)
          (concat
            [{:ci_profiles "default"
              :java_version default-java-version}]
            (map #(into {:ci_profiles "next-clojure"} %)
                 [{:java_version default-java-version}
                  {:java_version java-15-version}]))))

(defn edn-matrix []
  {:post [(seq %)]}
  (if (= "schedule" (System/getenv "GITHUB_EVENT_NAME"))
    (cron-matrix)
    (non-cron-matrix)))

(println (str "::set-output name=matrix::" (json/generate-string (edn-matrix) {:pretty false})))
