(ns line-bot-sdk-clojure.core-test
  (:require [clojure.test :refer :all]
            [line-bot-sdk-clojure.core :refer :all]))

(deftest test-validate-signature
  (let [signature "3q8QXTAGaey18yL8FWTqdVlbMr6hcuNvM4tefa0o9nA="
        content "{}"
        line-channel-secret "SECRET"]
    (is (true? (validate-signature content signature line-channel-secret))))
  (let [signature "596359635963"
        content "{}"
        line-channel-secret "SECRET"]
    (is (false? (validate-signature content signature line-channel-secret)))))
