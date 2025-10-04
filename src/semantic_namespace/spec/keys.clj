(ns semantic-namespace.spec.keys
  (:require [clojure.spec.alpha :as s]
            [semantic-namespace.compound.identity :as compound-identity]
            [clojure.set :as set]))

(defn specs-kws
  [type]
  (::specs (get @compound-identity/registry type)))

(defn- add-to-registry [compound-identity req-spec-keys]
  (swap! compound-identity/registry assoc compound-identity {::specs req-spec-keys})
  compound-identity)

(defn def
  [compound-identity req-spec-keys & [force?]]
  (assert (compound-identity/valid? compound-identity))
  (let [{:keys [exists matches]} (compound-identity/exists? compound-identity {::specs req-spec-keys})]
    (if exists 
      (if matches
        compound-identity
        (if force?
          (add-to-registry compound-identity req-spec-keys)          
          (throw (let [internal-req-spec-keys (::specs (get @compound-identity/registry compound-identity))]
                   (ex-info (format "already you defined this composed identity for other spec-req-keys for %s" internal-req-spec-keys)
                            {:id compound-identity
                             :internal-keys internal-req-spec-keys
                             :keys req-spec-keys})))))
      (add-to-registry compound-identity req-spec-keys))))

(defn valid? [composed-identity vals]
  (let [specs (specs-kws composed-identity)
        vals-keys (set (keys vals))
        specs-keys (set specs)]
    (if-not  (= (set/intersection vals-keys specs-keys) specs-keys)
      false
      (every? #(s/valid? % (% vals)) specs))))

(defn related-identities
  "all composed identities with one namespace keyword"
  [namespace-keyword]
  (vec (filter #(contains? % namespace-keyword) (keys @compound-identity/registry))))


(defn explain [composed-identity vals]
  (let [specs (specs-kws composed-identity)
        vals-keys (set (keys vals))
        specs-keys (set specs)]
    (if-not  (= (set/intersection vals-keys specs-keys) specs-keys)
      {:valid? false
       :message (format "there are some missing keys %s" (set/difference specs-keys vals-keys))
       :id composed-identity
       :missing-keys (set/difference specs-keys vals-keys)
       :expected-keys specs}
      (mapv #(let [problems (s/explain-data % (% vals))]
               (merge {:spec %
                       :valid? (s/valid? % (% vals))}
                      problems)
               ) specs))))
