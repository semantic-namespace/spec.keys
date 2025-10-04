(ns semantic-namespace.spec.keys-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer (deftest testing is)]
            [semantic-namespace.spec.keys :as spec.keys])
  (:import [java.util UUID]))
  
(deftest spec-keys-def-test
  (testing "spec-keys identity is a set with 2, or more  namespaced keywords, and a collection of specs"
    (s/def ::id uuid?)
    (s/def ::alias string?)

    (spec.keys/def
      #{:app.api.endpoint.get/response
        :app.domain/user}
      [::id ::alias])

    (is (false? (spec.keys/valid? #{:app.api.endpoint.get/response :app.domain/user}
                                  {::id (UUID/randomUUID)})))

    (is (spec.keys/valid? #{:app.api.endpoint.get/response :app.domain/user}
                          {::id (UUID/randomUUID) ::alias "@tangrammer"}))

    (testing "validation with extra keys"
      (is (spec.keys/valid? #{:app.api.endpoint.get/response :app.domain/user}
                            {::id (UUID/randomUUID) ::alias "@tangrammer" ::email "foo"})))

    (s/def ::email string?)
    (spec.keys/def
      #{:app.api.endpoint.get/request
        :app.domain/user
        :app.auth/jwt}
      [::id ::email])

    (is (= (spec.keys/related-identities :app.domain/user)
           [#{:app.api.endpoint.get/response :app.domain/user}
            #{:app.api.endpoint.get/request :app.domain/user :app.auth/jwt}]))

    (testing "2 namespaced keywords mean the same on any order"
      (is (spec.keys/valid? #{:app.domain/user :app.api.endpoint.get/response}
                            {::id (UUID/randomUUID) ::alias "@tangrammer"})))

    (is (false?
         (spec.keys/valid? #{:app.domain/user :app.api.endpoint.get/response}
                           {::id (UUID/randomUUID) ::github-alias "@tangrammer"})))

    (is (=
         (spec.keys/explain #{:app.domain/user :app.api.endpoint.get/response}
                            {::id (UUID/randomUUID) ::github-alias "@tangrammer"})
         {:valid? false,
          :message
          "there are some missing keys #{:semantic-namespace.spec.keys-test/alias}",
          :id #{:app.api.endpoint.get/response :app.domain/user},
          :missing-keys #{:semantic-namespace.spec.keys-test/alias},
          :expected-keys
          [:semantic-namespace.spec.keys-test/id
           :semantic-namespace.spec.keys-test/alias]}))

    (is (=
         (spec.keys/explain #{:app.domain/user :app.api.endpoint.get/response}
                            {::id (UUID/randomUUID) ::alias 80})
         [{:spec :semantic-namespace.spec.keys-test/id, :valid? true}
          {:spec :semantic-namespace.spec.keys-test/alias,
           :valid? false,
           :clojure.spec.alpha/problems
           [{:path [],
             :pred 'clojure.core/string?,
             :val 80,
             :via [:semantic-namespace.spec.keys-test/alias],
             :in []}],
           :clojure.spec.alpha/spec
           :semantic-namespace.spec.keys-test/alias,
           :clojure.spec.alpha/value 80}]))))


