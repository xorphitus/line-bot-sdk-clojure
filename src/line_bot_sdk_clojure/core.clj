(ns line-bot-sdk-clojure.core
  (:require [cheshire.core :refer [generate-string]]
            [clj-http.client :as http-client]
            [pandect.algo.sha256 :refer :all])
  (:import (java.util Base64)
           (java.security MessageDigest)))

(def line-api-endpoint "https://api.line.me/v2/bot")
(def line-api-reply-path "/message/reply")

(defn reply [to-user-id reply-token message line-channel-token]
  (let [body (generate-string {:to to-user-id
                               :replyToken reply-token
                               :messages [{:type "text"
                                           :text message}]})]
    (http-client/post (str line-api-endpoint line-api-reply-path)
     {:body body
      :headers {"Authorization" (str "Bearer " line-channel-token)}
      :content-type :json})))

(defn validate-signature [content signature line-channel-secret]
  (let [hash (sha256-hmac-bytes content line-channel-secret)
        decoded-signature (.. Base64 getDecoder (decode signature))]
    (. MessageDigest isEqual hash decoded-signature)))

(defn- compile-event [name args handler]
  (let [arg (first args)]
    {name `(fn[~arg] ~handler)}))

(defmacro MESSAGE [arg handler]
  (compile-event "message" arg handler))

(defmacro ELSE [arg handler]
  (compile-event :else arg handler))

(defmacro deflineevents [name & forms]
  `(defn ~name [events#]
     (let [handler-map# (apply merge ~@forms)]
       (map (fn [event#]
              (let [ev-type# (:type event#)]
                (if-let [handler# (get handler-map# ev-type#)]
                  (if (or (not= ev-type# "message") (fn? handler#))
                    (handler# event#)
                    (let [sub-type# (get-in event# [:message :type])
                          sub-events# handler#]
                      (if-let [sub-handler# (get sub-events# sub-type#)]
                        (sub-handler# event#)
                        ((:else sub-events#) event#))))
                  ((:else handler-map#) event#))))
            events#))))
